package com.andriybobchuk.mooney.core.premium

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSError
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.StoreKit.SKPayment
import platform.StoreKit.SKPaymentQueue
import platform.StoreKit.SKPaymentTransaction
import platform.StoreKit.SKPaymentTransactionObserverProtocol
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
    private var currentProductsDelegate: ProductsRequestDelegate? = null

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

        currentProductsDelegate = ProductsRequestDelegate(
            onSuccess = { products ->
                deferred.complete(products)
                currentProductsDelegate = null
            },
            onFailure = {
                deferred.complete(null)
                currentProductsDelegate = null
            }
        )

        val request = SKProductsRequest(productIdentifiers = setOf(PRODUCT_ID_MONTHLY))
        request.delegate = currentProductsDelegate
        request.start()

        val products = deferred.await() ?: return null
        cachedProducts = products

        return products.map { product ->
            val formatter = NSNumberFormatter().apply {
                numberStyle = NSNumberFormatterCurrencyStyle
                locale = product.priceLocale
            }
            val priceString = formatter.stringFromNumber(product.price) ?: "${product.price}"
            BillingProduct(
                id = product.productIdentifier,
                localizedPrice = priceString
            )
        }
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        if (!SKPaymentQueue.canMakePayments()) {
            return PurchaseResult.Error("Payments not allowed on this device")
        }

        var product = cachedProducts.firstOrNull { it.productIdentifier == productId }

        if (product == null) {
            fetchProducts()
            product = cachedProducts.firstOrNull { it.productIdentifier == productId }
        }

        if (product == null) {
            return PurchaseResult.Error("Product not found")
        }

        val deferred = CompletableDeferred<PurchaseResult>()
        purchaseDeferred = deferred
        val payment = SKPayment.paymentWithProduct(product)
        SKPaymentQueue.defaultQueue().addPayment(payment)

        return deferred.await()
    }

    override suspend fun restorePurchases(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        restoreDeferred = deferred
        _isSubscribed.value = false
        SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
        return deferred.await()
    }

    override suspend fun verifySubscription(): Boolean {
        // On iOS, don't call restoreCompletedTransactions silently — it prompts for Apple ID.
        // Rely on the DataStore cache instead. Real verification happens when user taps "Restore".
        return _isSubscribed.value
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
            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            when (transaction.transactionState as Long) {
                1L -> onPurchased(transaction)   // SKPaymentTransactionStatePurchased
                2L -> onFailed(transaction)      // SKPaymentTransactionStateFailed
                3L -> onRestored(transaction)    // SKPaymentTransactionStateRestored
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
