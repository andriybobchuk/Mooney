package com.andriybobchuk.mooney.core.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

actual class FileHandler(
    private val context: Context,
    private val filePickerLauncher: FilePickerLauncher,
) {

    actual suspend fun saveTextFile(content: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFileName = "mooney_backup_$timestamp.json"

            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, exportFileName)
            file.writeText(content)

            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, "Export Mooney Backup")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(FileError.SaveFailed(e.message ?: "Failed to save file"))
        }
    }

    actual suspend fun shareText(text: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooserIntent = Intent.createChooser(shareIntent, "Share")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(FileError.SaveFailed(e.message ?: "Failed to share"))
        }
    }

    actual suspend fun pickAndReadTextFile(): Result<String?> {
        val uri = filePickerLauncher.pick(arrayOf("application/json", "text/plain", "*/*"))
            ?: return Result.failure(FileError.NoFileSelected)

        return withContext(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (content == null) {
                    Result.failure(FileError.ReadFailed("Could not read file content"))
                } else {
                    Result.success(content)
                }
            } catch (e: Exception) {
                Result.failure(FileError.ReadFailed(e.message ?: "Failed to read file"))
            }
        }
    }
}
