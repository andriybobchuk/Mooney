package com.andriybobchuk.mooney.core.analytics

class DefaultAnalyticsTracker : AnalyticsTracker {

    override fun trackEvent(event: AnalyticsEvent) {
        runSafely {
            Analytics.logEvent(event.name, event.params)
            // Mirror each event into Crashlytics as a breadcrumb so that
            // crashes carry the trail of recent user actions automatically.
            // Format keeps the payload readable in the Crashlytics console.
            val paramSummary = if (event.params.isEmpty()) "" else " " + event.params.entries.joinToString(",") { "${it.key}=${it.value}" }
            Analytics.log("event:${event.name}$paramSummary")
        }
    }

    override fun trackScreenView(screenName: String) {
        // Firebase auto-collects screen_view; explicit calls were adding
        // noise to the funnel dashboards. We keep this method for the
        // interface contract + write a Crashlytics breadcrumb because
        // knowing the last screen when a crash lands is worth the write.
        runSafely { Analytics.setCustomKey("last_screen", screenName) }
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
