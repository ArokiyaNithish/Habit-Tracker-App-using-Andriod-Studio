package com.example.habittracker

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.habittracker.worker.HabitNotificationWorker
import java.util.concurrent.TimeUnit

class HabitTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleNotifications()
    }

    private fun scheduleNotifications() {
        val workRequest = PeriodicWorkRequestBuilder<HabitNotificationWorker>(
            24, TimeUnit.HOURS // Remind once a day
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "habit_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
