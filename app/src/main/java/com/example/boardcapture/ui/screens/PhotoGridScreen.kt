package com.example.boardcapture.ui.screens

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Photo(
    val uri: Uri,
    val id: Long,
    val displayName: String,
    val dateAdded: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGridScreen(
    subjectName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load photos when screen opens
    LaunchedEffect(subjectName) {
        withContext(Dispatchers.IO) {
            val loadedPhotos = loadPhotosForSubject(context, subjectName)
            withContext(Dispatchers.Main) {
                photos = loadedPhotos
            }
        }
    }

    // Handle back button press
    androidx.activity.compose.BackHandler(enabled = selectedPhoto != null) {
        selectedPhoto = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(subjectName)
                        Text(
                            "${photos.size} photos",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (photos.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No photos yet",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Take some photos to see them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            PhotoGrid(
                photos = photos,
                modifier = Modifier.padding(padding),
                selectedPhoto = selectedPhoto,
                onPhotoClick = { selectedPhoto = it },
                onDismissPhoto = { selectedPhoto = null },
                onDeleteClick = { photo ->
                    photoToDelete = photo
                    showDeleteDialog = true
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && photoToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Photo?") },
            text = { Text("Are you sure you want to delete this photo? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val photo = photoToDelete!!
                        val deleted = deletePhoto(context, photo)
                        if (deleted) {
                            photos = photos.filter { it.id != photo.id }
                            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                snackbarHostState.showSnackbar("Photo deleted")
                            }
                        } else {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                snackbarHostState.showSnackbar("Failed to delete photo")
                            }
                        }
                        showDeleteDialog = false
                        photoToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PhotoGrid(
    photos: List<Photo>,
    modifier: Modifier = Modifier,
    selectedPhoto: Photo?,
    onPhotoClick: (Photo) -> Unit,
    onDismissPhoto: () -> Unit,
    onDeleteClick: (Photo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(photos) { photo ->
            PhotoThumbnail(
                photo = photo,
                onClick = { onPhotoClick(photo) },
                onDeleteClick = { onDeleteClick(photo) }
            )
        }
    }

    // Full-screen photo viewer
    selectedPhoto?.let { photo ->
        FullScreenPhotoViewer(
            photo = photo,
            onDismiss = onDismissPhoto,
            onDeleteClick = {
                onDeleteClick(photo)
                onDismissPhoto()
            }
        )
    }
}

@Composable
fun PhotoThumbnail(
    photo: Photo,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            shape = MaterialTheme.shapes.small
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Delete button overlay
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoViewer(
    photo: Photo,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo.displayName) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onDeleteClick,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Photo"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)

                            // Only allow panning when zoomed in
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                // Reset position when zoomed out
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * Load all photos for a subject from MediaStore
 */
private fun loadPhotosForSubject(context: Context, subjectName: String): List<Photo> {
    val photos = mutableListOf<Photo>()

    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.RELATIVE_PATH
    )

    val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
    } else {
        "${MediaStore.Images.Media.DATA} LIKE ?"
    }

    val selectionArgs = arrayOf("%BoardCapture/$subjectName%")

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val date = cursor.getLong(dateColumn)

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id
            )

            photos.add(
                Photo(
                    uri = contentUri,
                    id = id,
                    displayName = name,
                    dateAdded = date
                )
            )
        }
    }

    return photos
}

/**
 * Delete a photo from MediaStore
 */
private fun deletePhoto(context: Context, photo: Photo): Boolean {
    return try {
        context.contentResolver.delete(photo.uri, null, null) > 0
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}