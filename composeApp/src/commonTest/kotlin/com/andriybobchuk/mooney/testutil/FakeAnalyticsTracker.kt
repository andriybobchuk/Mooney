package com.andriybobchuk.mooney.testutil

import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()
    val screenViews = mutableListOf<String>()
    val userProperties = mutableMapOf<String, String>()
    val exceptions = mutableListOf<Pair<Throwable, String>>()
    val logs = mutableListOf<String>()
    val customKeys = mutableMapOf<String, String>()

    override fun trackEvent(event: AnalyticsEvent) {
        events.add(event)
    }

    override fun trackScreenView(screenName: String) {
        screenViews.add(screenName)
    }

    override fun setUserProperty(name: String, value: String) {
        userProperties[name] = value
    }

    override fun recordException(throwable: Throwable, screen: String, context: Map<String, String>) {
        exceptions.add(throwable to screen)
    }

    override fun log(message: String) {
        logs.add(message)
    }

    override fun setCustomKey(key: String, value: String) {
        customKeys[key] = value
    }

    fun clear() {
        events.clear()
        screenViews.clear()
        userProperties.clear()
        exceptions.clear()
        logs.clear()
        customKeys.clear()
    }
}
