package com.andriybobchuk.mooney.core.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class ImageStorage {
    
    private val fileManager = NSFileManager.defaultManager
    
    private val imagesDirectory: String
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )
            val documentsDirectory = paths.firstOrNull() as? String ?: ""
            val imagesPath = "$documentsDirectory/goal_images"
            
            if (!fileManager.fileExistsAtPath(imagesPath)) {
                fileManager.createDirectoryAtPath(
                    imagesPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }
            
            return imagesPath
        }
    
    actual suspend fun saveImage(bytes: ByteArray, fileName: String): String? = withContext(Dispatchers.Default) {
        try {
            val filePath = "$imagesDirectory/$fileName"
            bytes.usePinned { pinned ->
                val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                val success = data.writeToFile(filePath, atomically = true)
                if (success) filePath else null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun loadImage(path: String): ByteArray? = withContext(Dispatchers.Default) {
        try {
            val data = NSData.dataWithContentsOfFile(path)
            data?.let { nsData ->
                // Convert NSData to ByteArray using a simple approach
                val length = nsData.length.toInt()
                val byteArray = ByteArray(length)
                if (length > 0) {
                    byteArray.usePinned { pinned ->
                        nsData.bytes?.let { dataBytes ->
                            memcpy(pinned.addressOf(0), dataBytes, nsData.length)
                        }
                    }
                }
                byteArray
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual suspend fun deleteImage(path: String): Boolean = withContext(Dispatchers.Default) {
        try {
            fileManager.removeItemAtPath(path, error = null)
        } catch (e: Exception) {
            false
        }
    }
}