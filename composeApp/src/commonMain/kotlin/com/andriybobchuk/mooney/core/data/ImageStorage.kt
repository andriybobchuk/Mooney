package com.andriybobchuk.mooney.core.data

expect class ImageStorage {
    suspend fun saveImage(bytes: ByteArray, fileName: String): String?
    suspend fun loadImage(path: String): ByteArray?
    suspend fun deleteImage(path: String): Boolean
}