package com.example.chatagent

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.chatagent.notification.NotificationHelper
import com.example.chatagent.worker.DailySummaryWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        const val DAILY_SUMMARY_HOUR = 5
        const val DAILY_SUMMARY_MINUTE = 12
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        // Schedule daily summary with LLM
        scheduleDailySummary()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun scheduleDailySummary() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Calculate delay until next scheduled time
        val currentTime = Calendar.getInstance()
        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, DAILY_SUMMARY_HOUR)
            set(Calendar.MINUTE, DAILY_SUMMARY_MINUTE)
            set(Calendar.SECOND, 0)

            // If scheduled time already passed today, schedule for tomorrow
            if (before(currentTime)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = scheduledTime.timeInMillis - currentTime.timeInMillis

        // Daily summary work request
        val dailySummaryRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            1, TimeUnit.DAYS // Run once per day
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,  // Always reschedule on app start
            dailySummaryRequest
        )

        val delayMinutes = initialDelay / 1000 / 60
        val delaySeconds = (initialDelay / 1000) % 60

        android.util.Log.d("MyApp", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MyApp", "⏰ DAILY SUMMARY SCHEDULER")
        android.util.Log.d("MyApp", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("MyApp", "📅 Scheduled time: ${DAILY_SUMMARY_HOUR}:${DAILY_SUMMARY_MINUTE.toString().padStart(2, '0')}")
        android.util.Log.d("MyApp", "⏱️  Next run in: ${delayMinutes}m ${delaySeconds}s")
        android.util.Log.d("MyApp", "📆 Next run at: ${scheduledTime.time}")
        android.util.Log.d("MyApp", "🔁 Repeats: Every 24 hours")
        android.util.Log.d("MyApp", "✅ Worker will run even when app is closed!")
        android.util.Log.d("MyApp", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}