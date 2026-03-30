package com.andriybobchuk.mooney.core.analytics

/**
 * Bridge interface that Swift code implements to forward calls to Firebase SDK.
 * Register the implementation from Swift after FirebaseApp.configure() by calling
 * Analytics.setBridge(bridge).
 */
interface IosAnalyticsBridge {
    fun logEvent(name: String, params: Map<String, String>)
    fun setUserId(id: String?)
    fun setUserProperty(name: String, value: String)
    fun recordException(message: String, context: Map<String, String>)
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Analytics {
    private var bridge: IosAnalyticsBridge? = null

    /**
     * Must be called from Swift after FirebaseApp.configure().
     * Pass an implementation of [IosAnalyticsBridge] that delegates to Firebase SDK.
     */
    fun setBridge(bridge: IosAnalyticsBridge) {
        this.bridge = bridge
    }

    actual fun logEvent(name: String, params: Map<String, String>) {
        bridge?.logEvent(name, params)
    }

    actual fun setUserId(id: String?) {
        bridge?.setUserId(id)
    }

    actual fun setUserProperty(name: String, value: String) {
        bridge?.setUserProperty(name, value)
    }

    actual fun recordException(throwable: Throwable, context: Map<String, String>) {
        bridge?.recordException(
            message = throwable.message ?: throwable::class.simpleName ?: "Unknown error",
            context = context,
        )
    }

    actual fun log(message: String) {
        bridge?.log(message)
    }

    actual fun setCustomKey(key: String, value: String) {
        bridge?.setCustomKey(key, value)
    }
}
