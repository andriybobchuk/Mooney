package com.andriybobchuk.mooney.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.andriybobchuk.mooney.core.data.preferences.StartupPrefs
import com.andriybobchuk.mooney.core.testing.isE2eBuild
import com.andriybobchuk.mooney.mooney.domain.FeatureFlags

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class MooneyDatabaseFactory(
    private val context: Context,
    private val startupPrefs: StartupPrefs
) {
    actual fun create(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        // Demo mode wins over every other bucket — it's the marketing recording
        // path and the user explicitly opted into it via Settings. We use a
        // distinct physical file (DB_NAME_DEMO) so the real ledger is
        // untouchable while the toggle is on.
        val dbName = when {
            startupPrefs.getDemoDbMode() -> AppDatabase.DB_NAME_DEMO
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
