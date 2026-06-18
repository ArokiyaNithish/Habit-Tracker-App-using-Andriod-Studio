package com.example.habittracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.habittracker.MainActivity
import com.example.habittracker.data.HabitDatabase
import kotlinx.coroutines.flow.first
import java.util.*

class HabitWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = HabitDatabase.getDatabase(context)
        val habitDao = database.habitDao()
        
        // Fetch data for the widget
        val allHabits = habitDao.getAllHabits().first()
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        var completedToday = 0
        allHabits.forEach { habit ->
            val completions = habitDao.getCompletionsForHabit(habit.id).first()
            if (completions.any { it.date == today }) {
                completedToday++
            }
        }
        
        val totalHabits = allHabits.size
        val progressText = if (totalHabits > 0) "$completedToday / $totalHabits completed" else "No habits set"

        provideContent {
            HabitWidgetContent(progressText)
        }
    }

    @Composable
    private fun HabitWidgetContent(progressText: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Habit Tracker",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ColorProvider(Color.Black)
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Progress Box
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(Color(0xFFDDEBFF))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Daily Progress",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    )
                    Text(
                        text = progressText,
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.DarkGray))
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.height(12.dp))
            Text(
                text = "Tap to open app",
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray))
            )
        }
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
