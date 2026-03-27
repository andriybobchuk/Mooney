package com.andriybobchuk.mooney.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class MooneyDatabaseFactory(
    private val context: Context
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        val dbName = if (FeatureFlags.isDebug) AppDatabase.DB_NAME_DEV else AppDatabase.DB_NAME
        val dbFile = appContext.getDatabasePath(dbName)

        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
    }
}
