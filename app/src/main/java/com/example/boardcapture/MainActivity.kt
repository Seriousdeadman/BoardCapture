package com.example.boardcapture

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.example.boardcapture.data.Subject
import com.example.boardcapture.ui.screens.HomeScreen
import com.example.boardcapture.ui.screens.PreviewScreen
import com.example.boardcapture.ui.theme.BoardCaptureTheme
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

@Composable
fun BoardCaptureApp() {
    // Track which screen we're on
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Track the photo we just took
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var currentSubject by remember { mutableStateOf<Subject?>(null) }

    // Create a temporary file for the camera to save to
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoFile = remember {
        File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
    }

    // Get URI for the file (required for Android security)
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    // Camera launcher - this opens the camera app
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Photo was taken successfully
            currentPhotoUri = photoUri
            currentScreen = Screen.Preview
        } else {
            // User cancelled or error occurred
            currentScreen = Screen.Home
        }
    }

    // Show different screens based on state
    when (currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                onSubjectClick = { subject ->
                    currentSubject = subject
                    // Launch camera
                    cameraLauncher.launch(photoUri)
                }
            )
        }
        is Screen.Preview -> {
            currentPhotoUri?.let { uri ->
                PreviewScreen(
                    photoUri = uri,
                    subjectName = currentSubject?.name ?: "Unknown",
                    onKeep = {
                        // TODO: Save photo to subject folder
                        println("Keeping photo for ${currentSubject?.name}")
                        currentScreen = Screen.Home
                    },
                    onRetake = {
                        // Launch camera again
                        currentSubject?.let { subject ->
                            cameraLauncher.launch(photoUri)
                        }
                    },
                    onCancel = {
                        // Delete temp file and go back
                        photoFile.delete()
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}

// Simple navigation state
sealed class Screen {
    object Home : Screen()
    object Preview : Screen()
}