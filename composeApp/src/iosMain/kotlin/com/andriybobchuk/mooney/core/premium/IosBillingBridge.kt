package com.andriybobchuk.mooney.core.premium

/**
 * Global holder so [IosBillingManager] can pick up the Swift bridge even if
 * `setIosBillingBridge` was called from `iOSApp.swift`'s AppDelegate before
 * Koin finished bootstrapping (Koin inits inside `MainViewController.configure`).
 * The manager reads this on its first `init` and the holder is set-once.
 */
internal object IosBillingBridgeHolder {
    var bridge: IosBillingBridge? = null
}

/**
 * Top-level entry point for `iOSApp.swift` to attach the Swift StoreKit 2
 * bridge to the Koin-managed [IosBillingManager] singleton. Kept here so
 * Swift sees a single free function (`IosBillingBridgeKt.setIosBillingBridge`)
 * rather than having to fish into Koin's container directly.
 *
 * Safe to call from AppDelegate (before Koin starts) — the bridge is stashed
 * in [IosBillingBridgeHolder] and picked up lazily when [IosBillingManager]
 * is first instantiated by Koin.
 */
fun setIosBillingBridge(bridge: IosBillingBridge) {
    IosBillingBridgeHolder.bridge = bridge
}

/**
 * Native StoreKit 2 bridge — implemented in `IosBillingBridge.swift` and
 * wired up via [IosBillingManager.setBridge] from `iOSApp.swift`.
 *
 * StoreKit 1 (`SKPaymentQueue` / `SKProduct`) is deprecated on iOS 26 and has
 * been the source of two App Store rejections (Submission 1f7ed921):
 *  - iPad popover presentation could crash with no `sourceView`
 *  - The observer-based callback model can silently drop transitions when
 *    the bridge between SKPaymentTransactionObserverProtocol and Kotlin
 *    coroutines doesn't deliver
 *
 * StoreKit 2 in Swift uses native `async/await` and returns a single Result
 * per call — much harder to lose, and Swift owns the UI presentation so
 * iPad layout issues never surface in Kotlin.
 *
 * Callbacks are used (not suspend functions) because Kotlin/Native cannot
 * call Swift `async` functions directly; the Kotlin side wraps each callback
 * in a `suspendCancellableCoroutine` to expose a suspend API to the rest of
 * the app.
 */
interface IosBillingBridge {

    /**
     * Fetch the localized price for a single product. `onResult` is invoked
     * exactly once with the formatted string (e.g. "$2.99") or `null` if the
     * product can't be resolved (not approved in App Store Connect, network
     * failure, etc).
     */
    fun fetchPrice(productId: String, onResult: (String?) -> Unit)

    /**
     * Launch the native StoreKit 2 purchase flow. `onResult` is invoked
     * exactly once with:
     *  - `status = "success"` — purchase completed & transaction finished
     *  - `status = "cancelled"` — user dismissed the system sheet
     *  - `status = "error"` — anything else; `message` describes why
     */
    fun purchase(productId: String, onResult: (BridgePurchaseResult) -> Unit)

    /**
     * Check current entitlements (already-purchased subscriptions). Returns
     * `true` if the user has an active entitlement for the product.
     */
    fun checkEntitlement(productId: String, onResult: (Boolean) -> Unit)
}

data class BridgePurchaseResult(
    val status: String,
    val message: String?
)
