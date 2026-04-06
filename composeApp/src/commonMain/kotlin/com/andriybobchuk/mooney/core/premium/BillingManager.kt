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
