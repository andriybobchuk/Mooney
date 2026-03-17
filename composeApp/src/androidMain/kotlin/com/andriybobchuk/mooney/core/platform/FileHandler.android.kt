package com.andriybobchuk.mooney.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

actual class FileHandler(private val context: Context) {
    
    actual suspend fun saveTextFile(content: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFileName = "mooney_backup_$timestamp.json"
            
            // Save to internal storage first
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val file = File(exportDir, exportFileName)
            file.writeText(content)
            
            // Create an intent to share the file
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
    
    actual suspend fun pickAndReadTextFile(): Result<String?> = withContext(Dispatchers.Main) {
        try {
            // Create an intent to pick a file
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
            }
            
            val chooserIntent = Intent.createChooser(intent, "Select Mooney Backup")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Note: This simplified approach won't wait for result
            // In a production app, you'd use registerForActivityResult or similar
            context.startActivity(chooserIntent)
            
            // For now, return a message indicating manual file selection is needed
            Result.failure(FileError.ReadFailed("Please use the file picker to select your backup file"))
        } catch (e: Exception) {
            Result.failure(FileError.ReadFailed(e.message ?: "Failed to read file"))
        }
    }
}