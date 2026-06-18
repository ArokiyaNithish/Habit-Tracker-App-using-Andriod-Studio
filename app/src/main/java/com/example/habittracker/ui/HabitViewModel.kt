package com.example.habittracker.ui

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.*
import com.example.habittracker.data.*
import com.example.habittracker.widget.HabitWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val database = HabitDatabase.getDatabase(application)
    private val repository = HabitRepository(database.habitDao())

    val habits = repository.allHabits.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        emptyList()
    )

    private val _selectedDate = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val selectedDate: StateFlow<Long> = _selectedDate

    private val _insightMonth = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val insightMonth: StateFlow<Long> = _insightMonth

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks = _selectedDate.flatMapLatest { date ->
        repository.getTasksForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val completions: StateFlow<Map<Int, List<Long>>> = habits.flatMapLatest { habitList ->
        if (habitList.isEmpty()) {
            flowOf(emptyMap())
        } else {
            val flows = habitList.map { habit ->
                repository.getCompletionsForHabit(habit.id).map { completions ->
                    habit.id to completions.map { it.date }
                }
            }
            combine(flows) { it.toMap() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun setInsightMonth(timeInMillis: Long) {
        _insightMonth.value = timeInMillis
    }

    fun changeInsightMonth(delta: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = _insightMonth.value }
        cal.add(Calendar.MONTH, delta)
        _insightMonth.value = cal.timeInMillis
    }

    private fun updateWidget() {
        viewModelScope.launch {
            HabitWidget().updateAll(getApplication())
        }
    }

    fun toggleHabit(habitId: Int, date: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                repository.insertCompletion(HabitCompletion(habitId, date))
            } else {
                repository.deleteCompletion(HabitCompletion(habitId, date))
            }
            updateWidget()
        }
    }

    fun addHabit(name: String, icon: String, targetDays: Int) {
        viewModelScope.launch {
            repository.insertHabit(Habit(name = name, icon = icon, targetDays = targetDays))
            updateWidget()
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            updateWidget()
        }
    }

    fun addTask(title: String, time: String) {
        viewModelScope.launch {
            repository.insertTask(Task(title = title, scheduledTime = time, date = _selectedDate.value))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun calculateStreak(completions: List<Long>): Int {
        if (completions.isEmpty()) return 0
        val sortedCompletions = completions.sortedDescending()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        var streak = 0
        var currentCheck = calendar.timeInMillis
        
        if (!sortedCompletions.contains(currentCheck)) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            currentCheck = calendar.timeInMillis
            if (!sortedCompletions.contains(currentCheck)) return 0
        }

        while (sortedCompletions.contains(currentCheck)) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            currentCheck = calendar.timeInMillis
        }
        return streak
    }
}
