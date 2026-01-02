package com.andriybobchuk.mooney.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class ImageStorage(private val context: Context) {
    
    private val imagesDir: File
        get() = File(context.filesDir, "goal_images").apply {
            if (!exists()) mkdirs()
        }
    
    actual suspend fun saveImage(bytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(imagesDir, fileName)
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun loadImage(path: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            File(path).readBytes()
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun deleteImage(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }
}