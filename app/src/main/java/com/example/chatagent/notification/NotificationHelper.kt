package com.example.chatagent.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.chatagent.MainActivity
import com.example.chatagent.R

object NotificationHelper {

    private const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for reminders and tasks"

    private const val SUMMARY_NOTIFICATION_ID = 1001
    private const val DAILY_SUMMARY_NOTIFICATION_ID = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderSummary(context: Context, summary: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📋 Reminder Summary")
            .setContentText(summary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(summary)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
    }

    fun showDailySummary(context: Context, summaryText: String) {
        android.util.Log.d("NotificationHelper", "showDailySummary called")
        android.util.Log.d("NotificationHelper", "Summary text length: ${summaryText.length}")

        createNotificationChannel(context)
        android.util.Log.d("NotificationHelper", "Notification channel created")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Extract first line for preview
        val firstLine = summaryText.lines().firstOrNull() ?: "Daily Summary"
        android.util.Log.d("NotificationHelper", "First line: $firstLine")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📊 Daily Reminder Summary")
            .setContentText(firstLine)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(summaryText)
                    .setBigContentTitle("📊 Daily Reminder Summary")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        android.util.Log.d("NotificationHelper", "Notification built, about to notify...")
        val notificationManager = NotificationManagerCompat.from(context)

        // Check if notifications are enabled
        if (!notificationManager.areNotificationsEnabled()) {
            android.util.Log.e("NotificationHelper", "❌ NOTIFICATIONS ARE DISABLED! Enable them in Settings!")
        } else {
            android.util.Log.d("NotificationHelper", "✅ Notifications are enabled")
        }

        notificationManager.notify(DAILY_SUMMARY_NOTIFICATION_ID, notification)
        android.util.Log.d("NotificationHelper", "notify() called - notification should appear now!")
    }
}
