package com.andriybobchuk.mooney.core.notifications

/**
 * Platform bridge for scheduling the daily expense-reminder notification.
 *
 * One notification only — fired daily at the configured local time. Cancelling
 * removes any pending fire. Scheduling twice replaces the previous schedule;
 * callers can call [scheduleDailyReminder] freely (idempotent).
 *
 * Mooney UX: pinned 19:30 by default; opt-out toggle in Settings.
 */
expect class NotificationScheduler() {

    /** Schedule (or reschedule) the daily reminder. Cancels any prior fire. */
    fun scheduleDailyReminder(hour: Int, minute: Int)

    /** Cancel any pending daily reminder. */
    fun cancelDailyReminder()
}

const val DAILY_REMINDER_HOUR = 19
const val DAILY_REMINDER_MINUTE = 30
