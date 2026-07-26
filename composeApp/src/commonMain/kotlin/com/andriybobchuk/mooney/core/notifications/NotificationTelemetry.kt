package com.andriybobchuk.mooney.core.notifications

import com.andriybobchuk.mooney.core.analytics.AnalyticsEvent
import com.andriybobchuk.mooney.core.analytics.AnalyticsTracker
import org.koin.mp.KoinPlatform

/**
 * Shared entry point that both platforms call when the app is opened via the
 * reminder notification. Fires `notification_opened` — a small but important
 * attribution signal that tells us whether users act on the reminder or open
 * the app on their own schedule.
 *
 * Wiring:
 *  - Android: `MainActivity.onCreate` / `onNewIntent` checks
 *    [EXTRA_FROM_NOTIFICATION] and calls `markOpened()`.
 *  - iOS: `AppDelegate` implements `UNUserNotificationCenterDelegate` and
 *    calls `NotificationTelemetry().markOpened()` from
 *    `userNotificationCenter:didReceive:`.
 *
 * Resolving via Koin's global context keeps the caller sites platform-only —
 * neither MainActivity nor Swift has to plumb the tracker through their DI.
 */
class NotificationTelemetry {
    fun markOpened() {
        try {
            val tracker = KoinPlatform.getKoin().get<AnalyticsTracker>()
            tracker.trackEvent(AnalyticsEvent.NotificationOpened)
        } catch (_: Exception) {
            // best-effort — never crash the launch flow for an analytics miss
        }
    }

    companion object {
        /** Intent-extra key set by the reminder PendingIntent on Android. */
        const val EXTRA_FROM_NOTIFICATION = "com.andriybobchuk.mooney.EXTRA_FROM_NOTIFICATION"
    }
}
