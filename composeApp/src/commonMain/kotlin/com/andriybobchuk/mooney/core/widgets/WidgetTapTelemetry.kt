package com.andriybobchuk.mooney.core.widgets

import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import org.koin.mp.KoinPlatform

/**
 * Cross-platform helper the platform layers call when a widget tap opens the
 * app. Fires `widget_tapped` so both iOS `onOpenURL` and Android
 * `MainActivity.onNewIntent` paths land in the same Firebase event.
 *
 * URL shape (both platforms): `mooney://widget/<kind>/<action>`
 *
 * Where `<kind>` is one of "balance"/"today"/"budget"/"streak"/"quick_add"
 * and `<action>` is a short route hint like "assets"/"home"/"analytics"/
 * "add_transaction".
 */
object WidgetTapTelemetry {
    fun handleWidgetOpen(url: String) {
        try {
            val trimmed = url.removePrefix("mooney://widget/")
            if (trimmed == url) return   // Not a widget deep-link
            val parts = trimmed.split("/")
            val kind = parts.getOrNull(0) ?: return
            val action = parts.getOrNull(1) ?: "home"
            val tracker = KoinPlatform.getKoin().get<AnalyticsTracker>()
            tracker.trackEvent(AnalyticsEvent.WidgetTapped(kind = kind, action = action))
        } catch (_: Throwable) {
            // Best-effort — analytics failure on the launch path is unacceptable.
        }
    }
}
