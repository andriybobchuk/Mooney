package com.andriybobchuk.mooney.core.presentation

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(
    onImagePicked: (ByteArray?) -> Unit
): ImagePicker

interface ImagePicker {
    fun pickImage()
}