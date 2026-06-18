package com.example.habittracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habittracker.MainActivity
import com.example.habittracker.data.HabitDatabase
import kotlinx.coroutines.flow.first
import java.util.*

class HabitNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = HabitDatabase.getDatabase(applicationContext)
        val habits = database.habitDao().getAllHabits().first()
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Find the first habit not completed today
        val pendingHabit = habits.firstOrNull { habit ->
            val completions = database.habitDao().getCompletionsForHabit(habit.id).first()
            completions.none { it.date == today }
        }

        showNotification(pendingHabit?.name, pendingHabit?.id ?: -1)
        return Result.success()
    }

    private fun showNotification(habitName: String?, habitId: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val contentText = if (habitName != null) {
            "Don't forget to: $habitName"
        } else {
            "All habits done for today? Keep it up!"
        }

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Habit Tracker")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Add "Done" action if there is a specific pending habit
        if (habitId != -1) {
            val actionIntent = Intent(applicationContext, HabitActionReceiver::class.java).apply {
                putExtra("HABIT_ID", habitId)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                applicationContext, habitId, actionIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_menu_save, "I did it!", actionPendingIntent)
        }

        notificationManager.notify(1, builder.build())
    }
}
