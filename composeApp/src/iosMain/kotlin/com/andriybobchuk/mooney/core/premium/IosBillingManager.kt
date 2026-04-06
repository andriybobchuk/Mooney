package com.andriybobchuk.mooney.core.premium

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSError
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
import platform.StoreKit.SKPaymentTransactionStateFailed
import platform.StoreKit.SKPaymentTransactionStatePurchased
import platform.StoreKit.SKPaymentTransactionStateRestored
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.darwin.NSObject

class IosBillingManager : BillingManager {

    private val _isSubscribed = MutableStateFlow(false)
    override val isSubscribed: Flow<Boolean> = _isSubscribed

    private var purchaseDeferred: CompletableDeferred<PurchaseResult>? = null
    private var restoreDeferred: CompletableDeferred<Boolean>? = null
    private var cachedProducts: List<SKProduct> = emptyList()

    private val transactionObserver = TransactionObserver(
        onPurchased = { transaction ->
            SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            _isSubscribed.value = true
            purchaseDeferred?.complete(PurchaseResult.Success)
            purchaseDeferred = null
        },
        onFailed = { transaction ->
            SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            val error = transaction.error
            val result = if (error?.code == 2L) { // SKErrorPaymentCancelled
                PurchaseResult.Cancelled
            } else {
                PurchaseResult.Error(error?.localizedDescription ?: "Purchase failed")
            }
            purchaseDeferred?.complete(result)
            purchaseDeferred = null
        },
        onRestored = { transaction ->
            SKPaymentQueue.defaultQueue().finishTransaction(transaction)
            _isSubscribed.value = true
        },
        onRestoreCompleted = {
            restoreDeferred?.complete(_isSubscribed.value)
            restoreDeferred = null
        },
        onRestoreFailed = {
            restoreDeferred?.complete(false)
            restoreDeferred = null
        }
    )

    init {
        SKPaymentQueue.defaultQueue().addTransactionObserver(transactionObserver)
    }

    override suspend fun fetchProducts(): List<BillingProduct>? {
        val deferred = CompletableDeferred<List<SKProduct>?>()

        val delegate = ProductsRequestDelegate(
            onSuccess = { products -> deferred.complete(products) },
            onFailure = { deferred.complete(null) }
        )

        val request = SKProductsRequest(productIdentifiers = setOf(PRODUCT_ID_MONTHLY))
        request.delegate = delegate
        request.start()

        val products = deferred.await() ?: return null
        cachedProducts = products

        return products.map { product ->
            val skProduct = product as SKProduct
            val priceString = "${skProduct.price}${skProduct.priceLocale.currencyCode ?: ""}"
            BillingProduct(
                id = skProduct.productIdentifier,
                localizedPrice = priceString
            )
        }
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        if (!SKPaymentQueue.canMakePayments()) {
            return PurchaseResult.Error("Payments not allowed on this device")
        }

        var product = cachedProducts.firstOrNull {
            (it as SKProduct).productIdentifier == productId
        } as? SKProduct

        if (product == null) {
            fetchProducts()
            product = cachedProducts.firstOrNull {
                (it as SKProduct).productIdentifier == productId
            } as? SKProduct
        }

        if (product == null) {
            return PurchaseResult.Error("Product not found")
        }

        purchaseDeferred = CompletableDeferred()
        val payment = SKPayment.paymentWithProduct(product)
        SKPaymentQueue.defaultQueue().addPayment(payment)

        return purchaseDeferred!!.await()
    }

    override suspend fun restorePurchases(): Boolean {
        restoreDeferred = CompletableDeferred()
        _isSubscribed.value = false
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
        return restoreDeferred!!.await()
    }

    override suspend fun verifySubscription(): Boolean {
        // On iOS with StoreKit 1, we rely on restore to check entitlements
        // The transaction observer will update _isSubscribed when restored transactions come in
        return restorePurchases()
    }
}

private class TransactionObserver(
    private val onPurchased: (SKPaymentTransaction) -> Unit,
    private val onFailed: (SKPaymentTransaction) -> Unit,
    private val onRestored: (SKPaymentTransaction) -> Unit,
    private val onRestoreCompleted: () -> Unit,
    private val onRestoreFailed: () -> Unit
) : NSObject(), SKPaymentTransactionObserverProtocol {

    override fun paymentQueue(
        queue: SKPaymentQueue,
        updatedTransactions: List<*>
    ) {
        for (item in updatedTransactions) {
            val transaction = item as? SKPaymentTransaction ?: continue
            when (transaction.transactionState) {
                SKPaymentTransactionStatePurchased -> onPurchased(transaction)
                SKPaymentTransactionStateFailed -> onFailed(transaction)
                SKPaymentTransactionStateRestored -> onRestored(transaction)
                else -> { /* deferred, purchasing — no action */ }
            }
        }
    }

    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        onRestoreCompleted()
    }

    override fun paymentQueue(
        queue: SKPaymentQueue,
        restoreCompletedTransactionsFailedWithError: NSError
    ) {
        onRestoreFailed()
    }
}

private class ProductsRequestDelegate(
    private val onSuccess: (List<SKProduct>) -> Unit,
    private val onFailure: () -> Unit
) : NSObject(), SKProductsRequestDelegateProtocol {

    override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
        @Suppress("UNCHECKED_CAST")
        val products = didReceiveResponse.products as? List<SKProduct> ?: emptyList()
        onSuccess(products)
    }

    override fun request(request: SKRequest, didFailWithError: NSError) {
        onFailure()
    }
}
