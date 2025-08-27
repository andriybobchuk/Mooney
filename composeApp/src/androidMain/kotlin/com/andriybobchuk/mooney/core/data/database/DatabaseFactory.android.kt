package com.andriybobchuk.mooney.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.andriybobchuk.mooney.core.data.database.AppDatabase

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class MooneyDatabaseFactory(
    private val context: Context
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath(AppDatabase.DB_NAME)

        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    }
}