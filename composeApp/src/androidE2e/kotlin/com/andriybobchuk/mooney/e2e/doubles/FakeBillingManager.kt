package com.andriybobchuk.mooney.e2e.doubles

import com.andriybobchuk.mooney.core.premium.BillingManager
import com.andriybobchuk.mooney.core.premium.BillingProduct
import com.andriybobchuk.mooney.core.premium.PRODUCT_ID_MONTHLY
import com.andriybobchuk.mooney.core.premium.PurchaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Deterministic premium state for E2E flows. Constructor arg drives the
 * initial subscription state; [purchase] flips it to true so a flow that
 * ends with "user subscribes" produces the same downstream state as a
 * real Play/StoreKit purchase without leaving E2E's control.
 */
class FakeBillingManager(startPremium: Boolean = false) : BillingManager {
    private val subscribed = MutableStateFlow(startPremium)

    override val isSubscribed: Flow<Boolean> = subscribed

    override suspend fun fetchProducts(): List<BillingProduct> = listOf(
        BillingProduct(id = PRODUCT_ID_MONTHLY, localizedPrice = "$3.99"),
    )

    override suspend fun purchase(productId: String): PurchaseResult {
        subscribed.value = true
        return PurchaseResult.Success
    }

    override suspend fun restorePurchases(): Boolean {
        // Restore does not spontaneously become true — the flow decides.
        return subscribed.value
    }

    override suspend fun verifySubscription(): Boolean = subscribed.value
}
