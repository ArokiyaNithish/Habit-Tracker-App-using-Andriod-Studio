package com.example.habittracker.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun insertHabit(habit: Habit) = habitDao.insertHabit(habit)
    suspend fun deleteHabit(habit: Habit) = habitDao.deleteHabit(habit)
    
    fun getCompletionsForHabit(habitId: Int) = habitDao.getCompletionsForHabit(habitId)
    
    suspend fun insertCompletion(completion: HabitCompletion) = habitDao.insertCompletion(completion)
    suspend fun deleteCompletion(completion: HabitCompletion) = habitDao.deleteCompletion(completion)

    fun getTasksForDate(date: Long): Flow<List<Task>> = habitDao.getTasksForDate(date)
    suspend fun insertTask(task: Task) = habitDao.insertTask(task)
    suspend fun updateTask(task: Task) = habitDao.updateTask(task)
    suspend fun deleteTask(task: Task) = habitDao.deleteTask(task)
}
