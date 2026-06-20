package com.andriybobchuk.mooney.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal const val REMINDER_CHANNEL_ID = "mooney_reminders"
internal const val REMINDER_WORK_NAME = "mooney_reminder"

actual class ReminderScheduler : KoinComponent {

    private val context: Context by inject()

    actual fun scheduleDaily(hour: Int, minute: Int) {
        ensureChannel(context)
        val initialDelayMs = millisUntilNextDailyOccurrence(hour, minute)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    actual fun scheduleWeekly(weekday: Int, hour: Int, minute: Int) {
        ensureChannel(context)
        val initialDelayMs = millisUntilNextWeeklyOccurrence(weekday, hour, minute)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = 7,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    actual fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
    }

    private fun millisUntilNextDailyOccurrence(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return cal.timeInMillis - now
    }

    private fun millisUntilNextWeeklyOccurrence(weekday: Int, hour: Int, minute: Int): Long {
        // Convert ISO weekday (Mon=1..Sun=7) to Calendar weekday (Sun=1..Sat=7).
        val targetCalendarDow = isoToCalendarDayOfWeek(weekday)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val currentDow = get(Calendar.DAY_OF_WEEK)
            var daysAhead = (targetCalendarDow - currentDow + 7) % 7
            if (daysAhead == 0 && timeInMillis <= now) daysAhead = 7
            add(Calendar.DAY_OF_MONTH, daysAhead)
        }
        return cal.timeInMillis - now
    }

    private fun isoToCalendarDayOfWeek(iso: Int): Int = when (iso) {
        1 -> Calendar.MONDAY
        2 -> Calendar.TUESDAY
        3 -> Calendar.WEDNESDAY
        4 -> Calendar.THURSDAY
        5 -> Calendar.FRIDAY
        6 -> Calendar.SATURDAY
        7 -> Calendar.SUNDAY
        else -> Calendar.SUNDAY
    }
}

/**
 * Creates the notification channel used by the reminder. Safe to call
 * multiple times; if the channel already exists, this is a no-op. Must be
 * called from app startup (MyApp.onCreate) so the channel exists before
 * either the user enables reminders or WorkManager posts one.
 */
fun ensureReminderNotificationChannel(context: Context) {
    ensureChannel(context)
}

private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    if (nm.getNotificationChannel(REMINDER_CHANNEL_ID) != null) return
    nm.createNotificationChannel(
        NotificationChannel(
            REMINDER_CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle nudges to log what you spent"
        }
    )
}
