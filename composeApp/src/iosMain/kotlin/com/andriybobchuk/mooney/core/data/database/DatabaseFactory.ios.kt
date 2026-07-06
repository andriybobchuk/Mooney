package com.andriybobchuk.mooney.core.data.database

import androidx.room.RoomDatabase
import androidx.room.Room
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class MooneyDatabaseFactory {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val dbName = if (FeatureFlags.isDebug) AppDatabase.DB_NAME_DEV else AppDatabase.DB_NAME
        val dbFile = documentDirectory() + "/$dbName"
        @Suppress("SpreadOperator")
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile
        ).addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .addCallback(SEED_DATABASE_CALLBACK)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return requireNotNull(documentDirectory?.path)
    }
}
