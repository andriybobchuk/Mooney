package com.andriybobchuk.mooney.core.notifications

import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

private const val DAILY_REQUEST_ID = "mooney_daily_reminder"

actual class NotificationScheduler {

    actual fun scheduleDailyReminder(hour: Int, minute: Int) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        // Ask permission lazily — first scheduling triggers the system prompt.
        // If the user declines, the trigger is created but iOS silently drops
        // it, which is acceptable; user can re-enable in Settings later.
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        ) { _, _ -> }

        val content = UNMutableNotificationContent().apply {
            setTitle("Don't forget today's expenses")
            setBody("Open Mooney and log what you spent today.")
        }

        val components = NSDateComponents().apply {
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = true
        )

        // Replace any existing daily reminder before adding the new one so the
        // schedule doesn't compound after a toggle off/on cycle.
        center.removePendingNotificationRequestsWithIdentifiers(listOf(DAILY_REQUEST_ID))
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = DAILY_REQUEST_ID,
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(request) { _ -> }
    }

    actual fun cancelDailyReminder() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(DAILY_REQUEST_ID))
    }
}
