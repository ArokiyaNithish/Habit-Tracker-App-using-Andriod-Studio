package com.example.habittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String = "", // For emojis like in the image
    val targetDays: Int = 30 // Goal count
)

@Entity(tableName = "habit_completions", primaryKeys = ["habitId", "date"])
data class HabitCompletion(
    val habitId: Int,
    val date: Long // Timestamp representing the day
)
