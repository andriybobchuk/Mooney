package com.andriybobchuk.mooney.core.analytics

class DefaultAnalyticsTracker : AnalyticsTracker {

    override fun trackEvent(event: AnalyticsEvent) {
        runSafely { Analytics.logEvent(event.name, event.params) }
    }

    override fun trackScreenView(screenName: String) {
        runSafely {
            Analytics.logEvent("screen_view", mapOf("screen_name" to screenName))
            Analytics.setCustomKey("last_screen", screenName)
        }
    }

    override fun setUserProperty(name: String, value: String) {
        runSafely { Analytics.setUserProperty(name, value) }
    }

    override fun recordException(throwable: Throwable, screen: String, context: Map<String, String>) {
        runSafely {
            val fullContext = context + ("screen" to screen)
            Analytics.recordException(throwable, fullContext)
        }
    }

    override fun log(message: String) {
        runSafely { Analytics.log(message) }
    }

    override fun setCustomKey(key: String, value: String) {
        runSafely { Analytics.setCustomKey(key, value) }
    }

    private inline fun runSafely(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            // Firebase not initialized or unavailable — silently ignore.
            // Analytics/Crashlytics must never crash the app.
        }
    }
}
