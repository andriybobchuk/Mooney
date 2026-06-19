package com.andriybobchuk.mooney.core.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.andriybobchuk.mooney.R

private const val REMINDER_NOTIFICATION_ID = 4242

/**
 * Posts the gentle "log your spending" reminder. Triggered by WorkManager on
 * the cadence chosen in Settings. The channel itself is created on app
 * startup (see [ensureReminderNotificationChannel]) so the post here can
 * succeed even on a process started cold by WorkManager.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        ensureReminderNotificationChannel(applicationContext)
        val title = applicationContext.getString(R.string.app_name)
        val body = applicationContext.getString(R.string.reminder_body)
        val notification = NotificationCompat.Builder(applicationContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(REMINDER_NOTIFICATION_ID, notification)
        return Result.success()
    }
}
