package com.andriybobchuk.mooney.mooney.domain.usecase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/**
 * Fires an analytics event the FIRST time something happens, then never again
 * (per install). Used to record activation moments — first account created,
 * first transaction created — that are valuable for funnel charts in Firebase
 * but only meaningful once. Subsequent calls become a no-op.
 *
 * Best-effort: any DataStore or analytics failure is swallowed silently to
 * avoid breaking the user-visible action that triggered the call.
 */
class TrackFirstEventUseCase(
    private val dataStore: DataStore<Preferences>,
    private val analyticsTracker: AnalyticsTracker
) {
    suspend fun firstAccount() {
        markOnce(PreferencesKeys.ANALYTICS_FIRST_ACCOUNT_FIRED, AnalyticsEvent.FirstAccountCreated)
    }

    suspend fun firstTransaction() {
        markOnce(PreferencesKeys.ANALYTICS_FIRST_TRANSACTION_FIRED, AnalyticsEvent.FirstTransactionCreated)
    }

    private suspend fun markOnce(
        key: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
        event: AnalyticsEvent
    ) {
        try {
            val fired = dataStore.data.first()[key] ?: false
            if (!fired) {
                analyticsTracker.trackEvent(event)
                dataStore.edit { it[key] = true }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // never break the calling action
        }
    }
}
