package com.andriybobchuk.mooney.core.data.database

import androidx.room.RoomDatabase
import androidx.room.Room
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class MooneyDatabaseFactory(
    private val startupPrefs: StartupPrefs
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        // Demo mode wins over every other bucket — user explicitly opted in
        // via Settings for a marketing recording. See AppDatabase.DB_NAME_DEMO.
        val dbName = when {
            startupPrefs.getDemoDbMode() -> AppDatabase.DB_NAME_DEMO
            FeatureFlags.isDebug -> AppDatabase.DB_NAME_DEV
            else -> AppDatabase.DB_NAME
        }
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
