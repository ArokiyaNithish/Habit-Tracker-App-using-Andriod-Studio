package com.example.habittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val scheduledTime: String = "", // e.g., "09:00 AM"
    val isCompleted: Boolean = false,
    val date: Long // The day this task belongs to
)
