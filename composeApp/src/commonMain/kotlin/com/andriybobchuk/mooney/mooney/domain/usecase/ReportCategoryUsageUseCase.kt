package com.andriybobchuk.mooney.mooney.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.core.data.category.DefaultCategoryProvider
import com.andriybobchuk.mooney.core.data.database.CategoryUsageDao
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.days

class ReportCategoryUsageUseCase(
    private val categoryUsageDao: CategoryUsageDao,
    private val categoryProvider: DefaultCategoryProvider,
    private val dataStore: DataStore<Preferences>,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend operator fun invoke() {
        try {
            val prefs = dataStore.data.first()
            val lastReport = prefs[PreferencesKeys.LAST_USAGE_REPORT]
            val now = Clock.System.now()

            if (lastReport != null) {
                val lastTime = Instant.parse(lastReport)
                if (now - lastTime < 7.days) return
            }

            val defaultIds = categoryProvider.getTransactionCategories()
                .categories.map { it.id }.toSet()
            val allUsage = categoryUsageDao.getMostUsedCategories(1000)
            val usedDefaultIds = allUsage.map { it.categoryId }.filter { it in defaultIds }.toSet()

            analyticsTracker.trackEvent(
                AnalyticsEvent.CategoryUsageSnapshot(
                    totalDefaults = defaultIds.size,
                    usedDefaults = usedDefaultIds.size,
                    unusedDefaults = defaultIds.size - usedDefaultIds.size
                )
            )

            dataStore.edit { it[PreferencesKeys.LAST_USAGE_REPORT] = now.toString() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-critical reporting
        }
    }
}
