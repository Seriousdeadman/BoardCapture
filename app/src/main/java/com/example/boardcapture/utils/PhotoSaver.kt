package com.example.boardcapture.utils


import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object PhotoSaver {

    fun savePhotoToGallery(
        context: Context,
        tempPhotoFile: File,
        subjectName: String
    ): Uri? {
        if (!tempPhotoFile.exists()) return null

        val timestamp = System.currentTimeMillis()
        val displayName = "${subjectName}_${timestamp}.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/BoardCapture/$subjectName")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageUri = context.contentResolver.insert(contentUri, contentValues) ?: return null

        return try {
            context.contentResolver.openOutputStream(imageUri)?.use { output ->
                tempPhotoFile.inputStream().use { input ->
                    input.copyTo(output, bufferSize = 8192)  // Larger buffer
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(imageUri, contentValues, null, null)
            }

            tempPhotoFile.delete()
            imageUri
        } catch (e: Exception) {
            context.contentResolver.delete(imageUri, null, null)
            null
        }
    }
}