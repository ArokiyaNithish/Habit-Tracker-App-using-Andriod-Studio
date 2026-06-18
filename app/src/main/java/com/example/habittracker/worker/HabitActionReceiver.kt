package com.example.habittracker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habittracker.data.HabitCompletion
import com.example.habittracker.data.HabitDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*

class HabitActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra("HABIT_ID", -1)
        if (habitId != -1) {
            val db = HabitDatabase.getDatabase(context)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            scope.launch {
                db.habitDao().insertCompletion(HabitCompletion(habitId, today))
            }
        }
    }
}
