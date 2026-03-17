package com.andriybobchuk.mooney.core.platform

expect class FileHandler {
    suspend fun saveTextFile(content: String, fileName: String): Result<Unit>
    suspend fun pickAndReadTextFile(): Result<String?>
}

sealed class FileError : Exception() {
    data object NoFileSelected : FileError()
    data class SaveFailed(override val message: String) : FileError()
    data class ReadFailed(override val message: String) : FileError()
}