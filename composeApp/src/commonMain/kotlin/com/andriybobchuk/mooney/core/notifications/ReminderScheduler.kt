package com.andriybobchuk.mooney.core.notifications

/**
 * Platform bridge for scheduling the user-configurable spending reminder.
 *
 * The reminder is opt-in via Settings → Reminders. The user chooses either a
 * daily cadence (every day at the chosen local time) or a weekly cadence
 * (once a week on the chosen weekday at the chosen local time). Calling
 * either schedule method replaces any previously-scheduled reminder so the
 * single "mooney_reminder" identity stays valid; [cancel] removes it.
 *
 * Defaults when the user enables for the first time: daily at 20:00 local.
 */
expect class ReminderScheduler() {

    /** Schedule (or reschedule) a daily reminder at the chosen local time. */
    fun scheduleDaily(hour: Int, minute: Int)

    /**
     * Schedule (or reschedule) a weekly reminder on the chosen weekday at the
     * chosen local time. [weekday] uses ISO numbering: 1 = Mon … 7 = Sun.
     */
    fun scheduleWeekly(weekday: Int, hour: Int, minute: Int)

    /** Cancel any pending reminder. Idempotent. */
    fun cancel()
}

const val DEFAULT_REMINDER_HOUR = 20
const val DEFAULT_REMINDER_MINUTE = 0
const val DEFAULT_REMINDER_WEEKDAY = 7 // Sunday in ISO numbering

enum class ReminderMode {
    OFF,
    DAILY,
    WEEKLY;

    companion object {
        fun fromStorage(value: String?): ReminderMode = when (value) {
            DAILY.name -> DAILY
            WEEKLY.name -> WEEKLY
            else -> OFF
        }
    }
}
