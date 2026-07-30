package com.andriybobchuk.mooney.core.premium

import kotlinx.coroutines.flow.Flow

data class BillingProduct(
    val id: String,
    val localizedPrice: String
)

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data object Cancelled : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

interface BillingManager {
    val isSubscribed: Flow<Boolean>
    suspend fun fetchProducts(): List<BillingProduct>?
    suspend fun purchase(productId: String): PurchaseResult
    suspend fun restorePurchases(): Boolean
    suspend fun verifySubscription(): Boolean
}

const val PRODUCT_ID_MONTHLY = "mooney_pro_monthly"
const val PRODUCT_ID_WEEKLY = "mooney_pro_weekly"

/**
 * Canonical order for paywall tier lists — weekly first (impulse buy) then
 * monthly (default). Both platforms and every UI surface iterate this same
 * list so the tier ordering never drifts out of sync between iOS and Android.
 */
val ALL_PRODUCT_IDS: List<String> = listOf(PRODUCT_ID_WEEKLY, PRODUCT_ID_MONTHLY)
