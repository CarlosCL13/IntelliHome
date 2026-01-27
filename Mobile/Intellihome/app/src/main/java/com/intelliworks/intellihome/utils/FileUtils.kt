package com.intelliworks.intellihome.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(context, uri)

            // Crear archivo temporal en caché
            val tempFile = File(context.cacheDir, fileName)
            tempFile.createNewFile()

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "temp_file"
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            if (nameIndex != -1) {
                name = returnCursor.getString(nameIndex)
            }
            returnCursor.close()
        }
        return name
    }
}