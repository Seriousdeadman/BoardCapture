package com.example.boardcapture.utils

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.boardcapture.data.Subject
import java.io.File

object FolderScanner {

    /**
     * Scan Pictures/BoardCapture/ folder and create Subject list
     */
    fun scanGalleryFolders(context: Context): List<Subject> {
        val subjects = mutableListOf<Subject>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore
            subjects.addAll(scanUsingMediaStore(context))
        } else {
            // Android 9 and below - Direct file access
            subjects.addAll(scanUsingFileSystem())
        }

        return subjects.sortedBy { it.name }
    }

    /**
     * Scan using MediaStore API (Android 10+)
     */
    private fun scanUsingMediaStore(context: Context): List<Subject> {
        val subjectMap = mutableMapOf<String, Int>() // name -> count

        val projection = arrayOf(
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media._ID
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Pictures/BoardCapture/%")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathColumn)

                // Extract subject name from path
                // Path format: "Pictures/BoardCapture/Mathematics/"
                val subjectName = extractSubjectName(path)

                if (subjectName != null) {
                    subjectMap[subjectName] = (subjectMap[subjectName] ?: 0) + 1
                }
            }
        }

        // Convert to Subject list
        return subjectMap.map { (name, count) ->
            Subject(
                id = name.hashCode().toString(), // Use name hash as ID
                name = name,
                photoCount = count
            )
        }
    }

    /**
     * Scan using file system (Android 9 and below)
     */
    private fun scanUsingFileSystem(): List<Subject> {
        val subjects = mutableListOf<Subject>()

        val boardCaptureDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "BoardCapture"
        )

        if (!boardCaptureDir.exists()) {
            return emptyList()
        }

        boardCaptureDir.listFiles()?.forEach { folder ->
            if (folder.isDirectory) {
                val photoCount = folder.listFiles()?.count { it.isFile } ?: 0

                subjects.add(
                    Subject(
                        id = folder.name.hashCode().toString(),
                        name = folder.name,
                        photoCount = photoCount
                    )
                )
            }
        }

        return subjects
    }

    /**
     * Extract subject name from relative path
     * Input: "Pictures/BoardCapture/Mathematics/"
     * Output: "Mathematics"
     */
    private fun extractSubjectName(path: String): String? {
        val parts = path.split("/")
        val boardCaptureIndex = parts.indexOf("BoardCapture")

        return if (boardCaptureIndex != -1 && boardCaptureIndex + 1 < parts.size) {
            parts[boardCaptureIndex + 1].takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    /**
     * Get actual photo count for a subject from gallery
     */
    fun getPhotoCount(context: Context, subjectName: String): Int {
        var count = 0

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Pictures/BoardCapture/$subjectName%")

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            count = cursor.count
        }

        return count
    }
}