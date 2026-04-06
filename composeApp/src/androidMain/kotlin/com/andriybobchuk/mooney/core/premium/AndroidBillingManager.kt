package com.andriybobchuk.mooney.core.premium

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidBillingManager(
    context: Context,
    private val activityProvider: ActivityProvider
) : BillingManager {

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: Flow<Boolean> = _isSubscribed

    private var purchaseDeferred: CompletableDeferred<PurchaseResult>? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        val deferred = purchaseDeferred
        purchaseDeferred = null

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            _isSubscribed.value = true
                            deferred?.complete(PurchaseResult.Success)
                            return@PurchasesUpdatedListener
                        }
                    }
                }
                deferred?.complete(PurchaseResult.Error("Purchase not completed"))
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                deferred?.complete(PurchaseResult.Cancelled)
            }
            else -> {
                deferred?.complete(PurchaseResult.Error("Purchase failed (code ${billingResult.responseCode})"))
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connectBillingClient()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                // No-op — connection ready
            }

            override fun onBillingServiceDisconnected() {
                // Will retry on next operation
            }
        })
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        val deferred = CompletableDeferred<Boolean>()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                deferred.complete(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                deferred.complete(false)
            }
        })
        return deferred.await()
    }

    override suspend fun fetchProducts(): List<BillingProduct>? {
        if (!ensureConnected()) return null

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result: ProductDetailsResult = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return null

        return result.productDetailsList?.map { details ->
            val price = details.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice ?: "10 PLN"

            BillingProduct(
                id = details.productId,
                localizedPrice = price
            )
        }
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        val activity = activityProvider.getActivity()
            ?: return PurchaseResult.Error("No activity available")

        if (!ensureConnected()) return PurchaseResult.Error("Billing not available")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = billingClient.queryProductDetails(params)
        val productDetails = result.productDetailsList?.firstOrNull()
            ?: return PurchaseResult.Error("Product not found")

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return PurchaseResult.Error("No offer available")

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        purchaseDeferred = CompletableDeferred()
        val launchResult = billingClient.launchBillingFlow(activity, flowParams)

        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            purchaseDeferred = null
            return PurchaseResult.Error("Failed to launch billing flow")
        }

        val purchaseResult = purchaseDeferred!!.await()

        // Acknowledge if successful
        if (purchaseResult is PurchaseResult.Success) {
            acknowledgePendingPurchases()
        }

        return purchaseResult
    }

    override suspend fun restorePurchases(): Boolean {
        if (!ensureConnected()) return false
        return checkSubscriptionActive()
    }

    override suspend fun verifySubscription(): Boolean {
        if (!ensureConnected()) return false
        val active = checkSubscriptionActive()
        _isSubscribed.value = active
        return active
    }

    private suspend fun checkSubscriptionActive(): Boolean {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return false

        for (purchase in result.purchasesList) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    try {
                        val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(ackParams)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Best-effort acknowledgement
                    }
                }
                _isSubscribed.value = true
                return true
            }
        }
        _isSubscribed.value = false
        return false
    }

    private suspend fun acknowledgePendingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)
        for (purchase in result.purchasesList) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                try {
                    val ackParams = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Best-effort
                }
            }
        }
    }
}
