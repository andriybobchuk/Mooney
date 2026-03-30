package com.andriybobchuk.mooney.core.analytics

interface AnalyticsTracker {
    fun trackEvent(event: AnalyticsEvent)
    fun trackScreenView(screenName: String)
    fun setUserProperty(name: String, value: String)

    // Crashlytics
    fun recordException(throwable: Throwable, screen: String, context: Map<String, String> = emptyMap())
    fun log(message: String)
    fun setCustomKey(key: String, value: String)
}
