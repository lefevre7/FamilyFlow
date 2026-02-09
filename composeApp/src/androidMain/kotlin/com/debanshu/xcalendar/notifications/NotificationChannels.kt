package com.debanshu.xcalendar.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

internal object NotificationChannels {
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val prepChannel = NotificationChannel(
            ReminderConstants.CHANNEL_PREP,
            "Prep reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Heads-up prep reminders"
        }
        val startChannel = NotificationChannel(
            ReminderConstants.CHANNEL_START,
            "Start reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Start-time reminders"
        }
        val summaryChannel = NotificationChannel(
            ReminderConstants.CHANNEL_SUMMARY,
            "Daily summaries",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morning and midday summaries"
        }
        manager.createNotificationChannel(prepChannel)
        manager.createNotificationChannel(startChannel)
        manager.createNotificationChannel(summaryChannel)
    }
}
