package com.andriybobchuk.mooney.core.notifications

import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val REMINDER_REQUEST_ID = "mooney_reminder"

actual class ReminderScheduler {

    actual fun scheduleDaily(hour: Int, minute: Int) {
        requestAuthorizationLazily()
        val components = NSDateComponents().apply {
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }
        scheduleWithComponents(components)
    }

    actual fun scheduleWeekly(weekday: Int, hour: Int, minute: Int) {
        requestAuthorizationLazily()
        // iOS NSDateComponents weekday: 1 = Sunday … 7 = Saturday.
        // We accept ISO 1 = Mon … 7 = Sun, so convert.
        val iosWeekday = isoToIosWeekday(weekday)
        val components = NSDateComponents().apply {
            this.weekday = iosWeekday.toLong()
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }
        scheduleWithComponents(components)
    }

    actual fun cancel() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_REQUEST_ID))
    }

    private fun requestAuthorizationLazily() {
        // First call surfaces the system permission prompt. Subsequent calls
        // resolve immediately with the previously-granted decision. If the
        // user declined, the trigger is still created but iOS silently drops
        // it — acceptable, the user can re-enable in iOS Settings.
        UNUserNotificationCenter.currentNotificationCenter()
            .requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
            ) { _, _ -> }
    }

    private fun scheduleWithComponents(components: NSDateComponents) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val content = UNMutableNotificationContent().apply {
            // TODO: route through Compose Resources once iOS notification
            //  localization is wired up. Hardcoded English for now — iOS Bundle
            //  localization isn't reachable from kotlinx string resources yet.
            setTitle("Mooney")
            setBody("Tap to log what you spent today.")
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = true
        )
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REMINDER_REQUEST_ID))
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = REMINDER_REQUEST_ID,
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(request) { _ -> }
    }

    private fun isoToIosWeekday(iso: Int): Int = when (iso) {
        1 -> 2 // Mon
        2 -> 3 // Tue
        3 -> 4 // Wed
        4 -> 5 // Thu
        5 -> 6 // Fri
        6 -> 7 // Sat
        7 -> 1 // Sun
        else -> 1
    }
}
