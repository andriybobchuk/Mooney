package com.andriybobchuk.mooney.core.premium

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class PremiumManager(
    private val dataStore: DataStore<Preferences>,
    private val billingManager: BillingManager,
    private val analyticsTracker: AnalyticsTracker
) {
    // When billing is disabled (Android), every user is treated as premium so
    // no gates ever trigger and no premium UI ever shows.
    val isPremium: Flow<Boolean> = if (!isBillingEnabled) {
        flowOf(true)
    } else combine(
        dataStore.data.map { it[PreferencesKeys.IS_PREMIUM] ?: false },
        billingManager.isSubscribed
    ) { cached, live -> cached || live }

    private val _monthlyPriceFlow = MutableStateFlow<String?>(null)
    /** Localized monthly subscription price (e.g. "$2.99"). Null until fetched. */
    val monthlyPriceFlow: StateFlow<String?> = _monthlyPriceFlow.asStateFlow()

    suspend fun getIsPremium(): Boolean {
        if (!isBillingEnabled) return true
        val cached = dataStore.data.firstOrNull()?.get(PreferencesKeys.IS_PREMIUM) ?: false
        return cached || isPremium.firstOrNull() ?: false
    }

    suspend fun syncSubscriptionStatus() {
        if (!isBillingEnabled) return
        val isActive = billingManager.verifySubscription()
        setPremium(isActive)
    }

    suspend fun purchase(productId: String): PurchaseResult {
        if (!isBillingEnabled) return PurchaseResult.Cancelled
        val result = billingManager.purchase(productId)
        val status = when (result) {
            is PurchaseResult.Success -> "success"
            is PurchaseResult.Cancelled -> "cancelled"
            is PurchaseResult.Error -> "error"
        }
        analyticsTracker.trackEvent(AnalyticsEvent.SubscribeResult(status, productId))
        if (result is PurchaseResult.Success) {
            setPremium(true)
        }
        return result
    }

    suspend fun restorePurchases(): Boolean {
        if (!isBillingEnabled) return false
        val restored = billingManager.restorePurchases()
        if (restored) setPremium(true)
        analyticsTracker.trackEvent(AnalyticsEvent.RestorePurchasesTap(success = restored))
        return restored
    }

    suspend fun fetchProducts(): List<BillingProduct>? {
        if (!isBillingEnabled) return null
        return billingManager.fetchProducts()
    }

    /**
     * Pull the monthly-subscription localized price into [monthlyPriceFlow].
     * Safe to call repeatedly — re-fetches each time. Best-effort: errors and
     * empty results leave the flow null, which the UI handles gracefully.
     */
    suspend fun refreshMonthlyPrice() {
        if (!isBillingEnabled) return
        try {
            val products = billingManager.fetchProducts()
            val monthly = products?.firstOrNull { it.id == PRODUCT_ID_MONTHLY }
            _monthlyPriceFlow.value = monthly?.localizedPrice
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Leave the price as whatever it was; UI shows "—" as fallback.
        }
    }

    suspend fun setPremium(premium: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_PREMIUM] = premium
        }
    }
}
