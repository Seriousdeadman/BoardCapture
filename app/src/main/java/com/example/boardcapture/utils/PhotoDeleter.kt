package com.example.boardcapture.utils

import android.content.ContentResolver
import android.content.ContentUris  // ADD THIS IMPORT
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object PhotoDeleter {

    fun deleteSubjectPhotos(context: Context, subjectName: String): Int {
        var deletedCount = 0

        // Query all images in the subject's folder
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Images.Media.DATA} LIKE ?"
        }

        val selectionArgs = arrayOf(
            "%BoardCapture/$subjectName%"
        )

        val cursor = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val deleteUri = ContentUris.withAppendedId(contentUri, id)  // Fixed - now using ContentUris

                try {
                    val deleted = context.contentResolver.delete(deleteUri, null, null)
                    if (deleted > 0) {
                        deletedCount++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return deletedCount
    }
}