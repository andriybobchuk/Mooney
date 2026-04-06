package com.andriybobchuk.mooney.core.premium

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.andriybobchuk.mooney.mooney.data.settings.PreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PremiumManager(
    private val dataStore: DataStore<Preferences>,
    private val billingManager: BillingManager
) {
    val isPremium: Flow<Boolean> = combine(
        dataStore.data.map { it[PreferencesKeys.IS_PREMIUM] ?: false },
        billingManager.isSubscribed
    ) { cached, live -> cached || live }

    suspend fun getIsPremium(): Boolean {
        val cached = dataStore.data.firstOrNull()?.get(PreferencesKeys.IS_PREMIUM) ?: false
        return cached || isPremium.firstOrNull() ?: false
    }

    suspend fun syncSubscriptionStatus() {
        val isActive = billingManager.verifySubscription()
        setPremium(isActive)
    }

    suspend fun purchase(productId: String): PurchaseResult {
        val result = billingManager.purchase(productId)
        if (result is PurchaseResult.Success) {
            setPremium(true)
        }
        return result
    }

    suspend fun restorePurchases(): Boolean {
        val restored = billingManager.restorePurchases()
        if (restored) setPremium(true)
        return restored
    }

    suspend fun fetchProducts(): List<BillingProduct>? {
        return billingManager.fetchProducts()
    }

    suspend fun setPremium(premium: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_PREMIUM] = premium
        }
    }
}
