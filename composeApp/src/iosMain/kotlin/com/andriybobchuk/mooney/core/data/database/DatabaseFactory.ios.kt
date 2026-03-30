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
        return Room.databaseBuilder<AppDatabase>(
            name = dbFile
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
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
