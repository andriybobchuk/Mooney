package com.andriybobchuk.mooney.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.Foundation.NSData
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(
    onImagePicked: (ByteArray?) -> Unit
): ImagePicker {
    return remember {
        IOSImagePicker(onImagePicked)
    }
}

@OptIn(ExperimentalForeignApi::class)
class IOSImagePicker(
    private val onImagePicked: (ByteArray?) -> Unit
) : ImagePicker {
    
    private val delegate = ImagePickerDelegate(onImagePicked)
    
    override fun pickImage() {
        val imagePicker = UIImagePickerController().apply {
            sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            delegate = this@IOSImagePicker.delegate
        }
        
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            imagePicker,
            animated = true,
            completion = null
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onImagePicked: (ByteArray?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        
        if (image != null) {
            val imageData = UIImageJPEGRepresentation(image, 0.8)
            val bytes = imageData?.toByteArray()
            onImagePicked(bytes)
        } else {
            onImagePicked(null)
        }
        
        picker.dismissViewControllerAnimated(true, completion = null)
    }
    
    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onImagePicked(null)
        picker.dismissViewControllerAnimated(true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        memcpy(bytes.refTo(0), this.bytes, this.length)
    }
    return bytes
}