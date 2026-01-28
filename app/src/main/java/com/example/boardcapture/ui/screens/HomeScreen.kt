package com.example.boardcapture.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.boardcapture.data.Subject
import com.example.boardcapture.ui.components.SubjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    subjects: List<Subject>,
    onSubjectClick: (Subject) -> Unit,
    onViewPhotos: (Subject) -> Unit,  // ADD THIS
    onAddSubject: (Subject) -> Unit,
    onDeleteSubject: (Subject) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BoardCapture") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        if (subjects.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No subjects yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add one!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Subject list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {

                items(subjects) { subject ->
                    SubjectCard(
                        subject = subject,
                        onTap = { onSubjectClick(subject) },
                        onView = { onViewPhotos(subject) },  // CHANGED
                        onDelete = { subjectToDelete = subject }
                    )
                }
            }
        }
    }

    // Add subject dialog
    if (showDialog) {
        AddSubjectDialog(
            onDismiss = { showDialog = false },
            onConfirm = { subjectName ->
                val newSubject = Subject(
                    id = System.currentTimeMillis().toString(),
                    name = subjectName,
                    photoCount = 0
                )
                onAddSubject(newSubject)
                showDialog = false
            }
        )
    }

    // Delete confirmation dialog
    subjectToDelete?.let { subject ->
        DeleteConfirmationDialog(
            subjectName = subject.name,
            onConfirm = {
                onDeleteSubject(subject)
                subjectToDelete = null
            },
            onDismiss = {
                subjectToDelete = null
            }
        )
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Subject") },
        text = {
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                label = { Text("Subject name") },
                singleLine = true,
                placeholder = { Text("e.g., Mathematics") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (subjectName.isNotBlank()) {
                        onConfirm(subjectName.trim())
                    }
                },
                enabled = subjectName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    subjectName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Subject?") },
        text = {
            Text("Are you sure you want to delete \"$subjectName\" and all its photos? This cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}