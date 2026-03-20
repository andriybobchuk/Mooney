package com.andriybobchuk.mooney.core.platform

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
actual class FileHandler {
    
    actual suspend fun saveTextFile(content: String, fileName: String): Result<Unit> = 
        suspendCancellableCoroutine { continuation ->
            val timestamp = NSDateFormatter().apply {
                dateFormat = "yyyyMMdd_HHmmss"
            }.stringFromDate(NSDate())
            
            val exportFileName = "mooney_backup_$timestamp.json"
            
            val documentsPath = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).firstOrNull() as? String
            
            if (documentsPath == null) {
                continuation.resume(Result.failure(FileError.SaveFailed("Cannot access documents directory")))
                return@suspendCancellableCoroutine
            }
            
            val filePath = "$documentsPath/$exportFileName"
            val fileURL = NSURL.fileURLWithPath(filePath)
            
            try {
                val data = content.encodeToByteArray().toNSData()
                val success = data.writeToURL(fileURL, atomically = true)
                
                if (success) {
                    // Present share sheet
                    val activityViewController = UIActivityViewController(
                        activityItems = listOf(fileURL),
                        applicationActivities = null
                    )
                    
                    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                    rootViewController?.presentViewController(
                        activityViewController,
                        animated = true,
                        completion = {
                            continuation.resume(Result.success(Unit))
                        }
                    )
                } else {
                    continuation.resume(Result.failure(FileError.SaveFailed("Failed to write file")))
                }
            } catch (e: Exception) {
                continuation.resume(Result.failure(FileError.SaveFailed(e.message ?: "Failed to save file")))
            }
        }
    
    actual suspend fun pickAndReadTextFile(): Result<String?> = 
        suspendCancellableCoroutine { continuation ->
            val documentPicker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeJSON, UTTypePlainText)
            )
            
            val delegate = DocumentPickerDelegate { urls ->
                if (urls.isEmpty()) {
                    continuation.resume(Result.failure(FileError.NoFileSelected))
                    return@DocumentPickerDelegate
                }
                
                val url = urls.first()
                try {
                    val accessing = url.startAccessingSecurityScopedResource()
                    val data = NSData.dataWithContentsOfURL(url)
                    if (accessing) {
                        url.stopAccessingSecurityScopedResource()
                    }
                    
                    if (data != null) {
                        val content = NSString.create(data, NSUTF8StringEncoding) as? String
                        continuation.resume(Result.success(content))
                    } else {
                        continuation.resume(Result.failure(FileError.ReadFailed("Cannot read file content")))
                    }
                } catch (e: Exception) {
                    continuation.resume(Result.failure(FileError.ReadFailed(e.message ?: "Failed to read file")))
                }
            }
            
            documentPicker.delegate = delegate
            
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(
                documentPicker,
                animated = true,
                completion = null
            )
        }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DocumentPickerDelegate(
    private val onPickedUrls: (List<NSURL>) -> Unit
) : UIDocumentPickerDelegateProtocol {
    
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        @Suppress("UNCHECKED_CAST")
        onPickedUrls(didPickDocumentsAtURLs as List<NSURL>)
    }
    
    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPickedUrls(emptyList())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = NSMutableData().apply {
    if (this@toNSData.isNotEmpty()) {
        this@toNSData.usePinned { pinned ->
            appendBytes(pinned.addressOf(0), this@toNSData.size.toULong())
        }
    }
}