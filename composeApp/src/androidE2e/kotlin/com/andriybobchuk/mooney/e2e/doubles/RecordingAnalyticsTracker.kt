package com.andriybobchuk.mooney.e2e.doubles

import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Captures every analytics event, screen view, and user-property write
 * so E2E flows can indirectly assert on side effects that never surface
 * in the UI (e.g. "adding the first transaction fired the FirstEvent").
 *
 * Also short-circuits Firebase Analytics + Crashlytics I/O so the test
 * emulator never blocks on network / disk writes we don't care about.
 *
 * Not currently wired into any flow — the Koin binding is here so the
 * next flow that needs "assert this event fired" can plug it in via a
 * dev-tools-style hook. Keeping the recorder in `androidE2e` means it
 * never ships to production.
 */
class RecordingAnalyticsTracker : AnalyticsTracker {

    private val events = ConcurrentLinkedQueue<AnalyticsEvent>()
    private val screenViews = ConcurrentLinkedQueue<String>()
    private val userProperties = ConcurrentLinkedQueue<Pair<String, String>>()
    private val logs = ConcurrentLinkedQueue<String>()

    override fun trackEvent(event: AnalyticsEvent) {
        events += event
    }

    override fun trackScreenView(screenName: String) {
        screenViews += screenName
    }

    override fun setUserProperty(name: String, value: String) {
        userProperties += name to value
    }

    override fun recordException(
        throwable: Throwable,
        screen: String,
        context: Map<String, String>,
    ) {
        // Silence — an unexpected exception during a flow surfaces as a
        // crash on the emulator anyway, and Crashlytics network I/O
        // would flake the run for reasons unrelated to the flow's intent.
    }

    override fun log(message: String) {
        logs += message
    }

    override fun setCustomKey(key: String, value: String) = Unit

    fun capturedEvents(): List<AnalyticsEvent> = events.toList()
    fun capturedScreenViews(): List<String> = screenViews.toList()
    fun capturedUserProperties(): List<Pair<String, String>> = userProperties.toList()
    fun capturedLogs(): List<String> = logs.toList()

    fun clear() {
        events.clear()
        screenViews.clear()
        userProperties.clear()
        logs.clear()
    }
}
