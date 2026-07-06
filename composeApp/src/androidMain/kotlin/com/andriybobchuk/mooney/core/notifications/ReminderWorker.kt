package com.andriybobchuk.mooney.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.andriybobchuk.mooney.MainActivity
import com.andriybobchuk.mooney.R

private const val REMINDER_NOTIFICATION_ID = 4242

/**
 * Posts the gentle "log your spending" reminder. Triggered by WorkManager on
 * the cadence chosen in Settings. The channel itself is created on app
 * startup (see [ensureReminderNotificationChannel]) so the post here can
 * succeed even on a process started cold by WorkManager.
 *
 * Tapping the notification launches [MainActivity]; without a content intent
 * the reminder would just dismiss silently, breaking parity with iOS where
 * tapping a notification opens the app.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        ensureReminderNotificationChannel(applicationContext)
        val title = applicationContext.getString(R.string.app_name)
        val body = applicationContext.getString(R.string.reminder_body)

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(REMINDER_NOTIFICATION_ID, notification)
        return Result.success()
    }
}
