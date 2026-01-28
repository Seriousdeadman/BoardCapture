package com.example.boardcapture

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.boardcapture.data.Subject
import com.example.boardcapture.ui.screens.HomeScreen
import com.example.boardcapture.ui.screens.PhotoGridScreen
import com.example.boardcapture.ui.theme.BoardCaptureTheme
import com.example.boardcapture.utils.FolderScanner
import com.example.boardcapture.utils.PhotoDeleter
import com.example.boardcapture.utils.PhotoSaver
import com.example.boardcapture.utils.rememberCameraPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoardCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BoardCaptureApp()
                }
            }
        }
    }
}

// Simple navigation state
sealed class Screen {
    object Home : Screen()
    data class PhotoGrid(val subjectName: String) : Screen()
}

@Composable
fun BoardCaptureApp() {
    val context = LocalContext.current

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var subjects by remember { mutableStateOf(emptyList<Subject>()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val scannedSubjects = FolderScanner.scanGalleryFolders(context)
            withContext(Dispatchers.Main) {
                subjects = scannedSubjects
            }
        }
    }

    var activeSubject by remember { mutableStateOf<Subject?>(null) }
    var currentPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingSubject by remember { mutableStateOf<Subject?>(null) }

    var launcherRef by remember { mutableStateOf<ActivityResultLauncher<Uri>?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoFile != null && activeSubject != null) {
            val savedUri = PhotoSaver.savePhotoToGallery(
                context = context,
                tempPhotoFile = currentPhotoFile!!,
                subjectName = activeSubject!!.name
            )

            if (savedUri != null) {
                subjects = subjects.map {
                    if (it.id == activeSubject!!.id) {
                        it.copy(photoCount = it.photoCount + 1)
                    } else {
                        it
                    }
                }

                val nextPhotoFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                currentPhotoFile = nextPhotoFile

                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    nextPhotoFile
                )

                launcherRef?.launch(photoUri)
            } else {
                Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        } else {
            currentPhotoFile?.delete()
            currentPhotoFile = null
            activeSubject = null
        }
    }

    launcherRef = cameraLauncher

    val requestPermission = rememberCameraPermission(
        onPermissionGranted = {
            pendingSubject?.let { subject ->
                activeSubject = subject

                val photoFile = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
                currentPhotoFile = photoFile

                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )

                cameraLauncher.launch(photoUri)
                pendingSubject = null
            }
        },
        onPermissionDenied = {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
            pendingSubject = null
        }
    )

    // Show different screens based on navigation state
    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                subjects = subjects,
                onSubjectClick = { subject ->
                    pendingSubject = subject
                    requestPermission()
                },
                onViewPhotos = { subject ->
                    currentScreen = Screen.PhotoGrid(subject.name)
                },
                onAddSubject = { newSubject ->
                    subjects = subjects + newSubject
                },
                onDeleteSubject = { subjectToDelete ->
                    CoroutineScope(Dispatchers.IO).launch {
                        PhotoDeleter.deleteSubjectPhotos(context, subjectToDelete.name)

                        withContext(Dispatchers.Main) {
                            subjects = subjects.filter { it.id != subjectToDelete.id }
                            Toast.makeText(context, "Deleted ${subjectToDelete.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        is Screen.PhotoGrid -> {
            // Handle back button to go back to Home when in PhotoGrid
            BackHandler {
                currentScreen = Screen.Home
            }

            PhotoGridScreen(
                subjectName = screen.subjectName,
                onBackClick = { currentScreen = Screen.Home }
            )
        }
    }
}