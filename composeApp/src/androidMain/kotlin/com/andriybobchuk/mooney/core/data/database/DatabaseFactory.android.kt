package com.andriybobchuk.mooney.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.andriybobchuk.mooney.core.testing.isE2eBuild
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class MooneyDatabaseFactory(
    private val context: Context
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        val dbName = when {
            isE2eBuild -> AppDatabase.DB_NAME_E2E
            FeatureFlags.isDebug -> AppDatabase.DB_NAME_DEV
            else -> AppDatabase.DB_NAME
        }
        val dbFile = appContext.getDatabasePath(dbName)

        @Suppress("SpreadOperator")
        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        ).addMigrations(*ALL_MIGRATIONS.toTypedArray())
            .addCallback(SEED_DATABASE_CALLBACK)
    }
}
