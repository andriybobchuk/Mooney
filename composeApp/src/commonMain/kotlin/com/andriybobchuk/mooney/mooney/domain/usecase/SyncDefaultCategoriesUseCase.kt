package com.andriybobchuk.mooney.mooney.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.category.DefaultCategoryProvider
import com.andriybobchuk.mooney.core.data.database.AssetCategoryEntity
import com.andriybobchuk.mooney.core.data.database.AssetCategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryDao
import com.andriybobchuk.mooney.core.data.database.CategoryEntity
import com.andriybobchuk.mooney.core.data.database.TransactionDao
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

class SyncDefaultCategoriesUseCase(
    private val categoryProvider: DefaultCategoryProvider,
    private val categoryDao: CategoryDao,
    private val assetCategoryDao: AssetCategoryDao,
    private val transactionDao: TransactionDao,
    private val dataStore: DataStore<Preferences>,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend operator fun invoke() {
        try {
            val hasTransactions = transactionDao.getCount() > 0
            if (hasTransactions) {
                ensureVersionTracked()
                return
            }

            val prefs = dataStore.data.first()
            val storedVersion = prefs[PreferencesKeys.DEFAULTS_VERSION]

            val transactionDefaults = categoryProvider.getTransactionCategories()
            val assetDefaults = categoryProvider.getAssetCategories()

            if (storedVersion != null && transactionDefaults.version <= storedVersion) return

            // Sync transaction categories (additive only)
            for (cat in transactionDefaults.categories) {
                val existing = categoryDao.getById(cat.id)
                if (existing == null) {
                    categoryDao.upsert(
                        CategoryEntity(
                            id = cat.id,
                            title = cat.title,
                            type = cat.type,
                            emoji = cat.emoji,
                            parentId = cat.parentId
                        )
                    )
                }
            }

            // Sync asset categories (additive only)
            for (cat in assetDefaults.categories) {
                val existing = assetCategoryDao.getById(cat.id)
                if (existing == null) {
                    assetCategoryDao.upsert(
                        AssetCategoryEntity(
                            id = cat.id,
                            title = cat.title,
                            emoji = cat.emoji,
                            description = cat.description,
                            color = cat.color,
                            sortOrder = cat.sortOrder,
                            isLiability = cat.isLiability
                        )
                    )
                }
            }

            dataStore.edit { it[PreferencesKeys.DEFAULTS_VERSION] = transactionDefaults.version }

            val source = "bundled"
            analyticsTracker.trackEvent(
                AnalyticsEvent.DefaultsVersionApplied(transactionDefaults.version, source)
            )
            analyticsTracker.setUserProperty("defaults_version", transactionDefaults.version.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-critical — seeding from SQL already happened
        }
    }

    private suspend fun ensureVersionTracked() {
        val prefs = dataStore.data.first()
        if (prefs[PreferencesKeys.DEFAULTS_VERSION] == null) {
            try {
                val version = categoryProvider.getTransactionCategories().version
                dataStore.edit { it[PreferencesKeys.DEFAULTS_VERSION] = version }
                analyticsTracker.setUserProperty("defaults_version", version.toString())
            } catch (_: Exception) { }
        }
    }
}
