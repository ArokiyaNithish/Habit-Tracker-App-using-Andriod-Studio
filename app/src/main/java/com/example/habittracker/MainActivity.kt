package com.example.habittracker

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habittracker.data.Habit
import com.example.habittracker.data.Task
import com.example.habittracker.ui.HabitViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: HabitViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Habits", "Schedule", "Calendar")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title) },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Default.CheckCircle
                                    1 -> Icons.Default.List
                                    else -> Icons.Default.DateRange
                                },
                                contentDescription = title
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HabitTrackerScreen(viewModel)
                1 -> ScheduleScreen(viewModel)
                2 -> CalendarInsightScreen(viewModel)
            }
        }
    }
}

@Composable
fun HabitTrackerScreen(viewModel: HabitViewModel) {
    val habits by viewModel.habits.collectAsState()
    val completions by viewModel.completions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "DAILY HABITS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().background(Color(0xFFDDEBFF)).padding(8.dp),
            textAlign = TextAlign.Center
        )

        Box(modifier = Modifier.weight(1f)) {
            Column {
                HabitHeaderRow(horizontalScrollState)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(habits) { habit ->
                        val habitCompletions = completions[habit.id] ?: emptyList()
                        HabitRow(
                            habit = habit,
                            completions = habitCompletions,
                            streak = viewModel.calculateStreak(habitCompletions),
                            scrollState = horizontalScrollState,
                            onToggle = { date, checked -> viewModel.toggleHabit(habit.id, date, checked) },
                            onLongPress = { viewModel.deleteHabit(habit) }
                        )
                    }
                }
            }
        }

        DailyProgressSection(habits, completions)
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.End).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Habit")
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, icon, goal ->
                viewModel.addHabit(name, icon, goal)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun HabitHeaderRow(scrollState: ScrollState) {
    val weekColors = listOf(Color(0xFFDDEBFF), Color(0xFFFFEBF2), Color(0xFFEBF9F2), Color(0xFFFFF7EB), Color(0xFFDDEBFF))
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    Column {
        Row(modifier = Modifier.padding(start = 120.dp).horizontalScroll(scrollState)) {
            for (i in 0 until 5) {
                val weekDayCount = if (i == 4) (daysInMonth - 28) else 7
                if (weekDayCount > 0) {
                    Text(
                        text = "week ${i + 1}",
                        modifier = Modifier.width((weekDayCount * 35).dp).background(weekColors[i % weekColors.size]).border(0.2.dp, Color.Gray),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA))) {
            Text("Habit Name", modifier = Modifier.width(120.dp).border(0.2.dp, Color.LightGray).padding(start = 8.dp), fontSize = 10.sp)
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                val dayFormat = SimpleDateFormat("E", Locale.getDefault())
                for (i in 1..daysInMonth) {
                    calendar.set(Calendar.DAY_OF_MONTH, i)
                    val dayName = dayFormat.format(calendar.time).first()
                    val isToday = i == today
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        modifier = Modifier
                            .width(35.dp)
                            .background(if (isToday) Color(0xFFFFEB3B).copy(alpha = 0.3f) else Color.Transparent)
                            .border(0.2.dp, Color.LightGray)
                            .padding(vertical = 2.dp)
                    ) {
                        Text(text = dayName.toString(), fontSize = 9.sp, fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold)
                        Text(text = "$i", fontSize = 10.sp, color = if (isToday) Color.Red else Color.Unspecified)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitRow(habit: Habit, completions: List<Long>, streak: Int, scrollState: ScrollState, onToggle: (Long, Boolean) -> Unit, onLongPress: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val todayDay = calendar.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .width(120.dp)
                .height(35.dp)
                .border(0.2.dp, Color.LightGray)
                .combinedClickable(onClick = { }, onLongClick = { showDeleteConfirm = true })
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${habit.icon} ${habit.name}",
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            if (streak > 0) {
                Text("🔥 $streak", fontSize = 8.sp, color = Color(0xFFFF5722), fontWeight = FontWeight.Bold)
            }
        }
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            for (day in 1..daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val dayMillis = calendar.timeInMillis
                val isCompleted = completions.contains(dayMillis)
                val isToday = day == todayDay
                
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(if (isToday) Color(0xFFFFEB3B).copy(alpha = 0.1f) else Color.Transparent)
                        .border(0.2.dp, Color.LightGray)
                        .clickable { onToggle(dayMillis, !isCompleted) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Text("✔", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else if (isToday) {
                        Box(modifier = Modifier.size(4.dp).background(Color.Red.copy(alpha = 0.5f), shape = MaterialTheme.shapes.extraSmall))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit?") },
            text = { Text("Delete '${habit.name}'?") },
            confirmButton = {
                Button(onClick = { onLongPress(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DailyProgressSection(habits: List<Habit>, completions: Map<Int, List<Long>>) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFDDEBFF)).padding(16.dp)) {
        Text("STATISTICS", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("goal", modifier = Modifier.width(30.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("percentage", modifier = Modifier.weight(1f), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("count", modifier = Modifier.width(40.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        habits.forEach { habit ->
            val count = completions[habit.id]?.size ?: 0
            val percentage = if (habit.targetDays > 0) (count * 100 / habit.targetDays).coerceAtMost(100) else 0
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${habit.targetDays}", fontSize = 9.sp, modifier = Modifier.width(30.dp))
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("$percentage%", fontSize = 9.sp, modifier = Modifier.width(30.dp))
                    LinearProgressIndicator(
                        progress = percentage / 100f,
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = Color(0xFF1976D2),
                        trackColor = Color.White
                    )
                }
                Text("$count / ${habit.targetDays}", fontSize = 9.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun ScheduleScreen(viewModel: HabitViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Daily Timetable", style = MaterialTheme.typography.headlineMedium)
        DateSelector(selectedDate) { viewModel.setSelectedDate(it) }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tasks) { task ->
                TaskItem(task, onToggle = { viewModel.toggleTask(task) }, onDelete = { viewModel.deleteTask(task) })
            }
        }
        Button(onClick = { showAddTaskDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Add Task / Schedule")
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(onDismiss = { showAddTaskDialog = false }, onAdd = { title, time ->
            viewModel.addTask(title, time)
            showAddTaskDialog = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarInsightScreen(viewModel: HabitViewModel) {
    val habits by viewModel.habits.collectAsState()
    val completions by viewModel.completions.collectAsState()
    var selectedHabit by remember { mutableStateOf<Habit?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Habit Insights", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            habits.forEach { habit ->
                FilterChip(
                    selected = selectedHabit?.id == habit.id,
                    onClick = { selectedHabit = habit },
                    label = { Text("${habit.icon} ${habit.name}") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedHabit != null) {
            val habitCompletions = completions[selectedHabit!!.id] ?: emptyList()
            val streak = viewModel.calculateStreak(habitCompletions)
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Streak", style = MaterialTheme.typography.titleSmall)
                    Text("$streak Days", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                }
            }
            
            MonthCalendarGrid(habitCompletions)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a habit to view calendar")
            }
        }
    }
}

@Composable
fun MonthCalendarGrid(completions: List<Long>) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    Column {
        Text(monthName, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        
        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(300.dp)) {
            items(firstDayOfWeek) { Box(modifier = Modifier.size(40.dp)) }
            items(daysInMonth) { day ->
                val dayNum = day + 1
                calendar.set(currentYear, currentMonth, dayNum, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val isDone = completions.contains(calendar.timeInMillis)
                
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .background(if (isDone) Color(0xFF4CAF50) else Color(0xFFF0F0F0), MaterialTheme.shapes.small)
                        .border(1.dp, Color.LightGray, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$dayNum", color = if (isDone) Color.White else Color.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DateSelector(selectedDate: Long, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { 
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            onDateSelected(calendar.timeInMillis) 
        }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev") }
        Row(modifier = Modifier.clickable {
            DatePickerDialog(context, { _, y, m, d ->
                val newCal = Calendar.getInstance()
                newCal.set(y, m, d, 0, 0, 0)
                newCal.set(Calendar.MILLISECOND, 0)
                onDateSelected(newCal.timeInMillis)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(sdf.format(Date(selectedDate)), fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = { 
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            onDateSelected(calendar.timeInMillis) 
        }) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).combinedClickable(onClick = { }, onLongClick = { showDeleteConfirm = true }), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
        Column {
            Text(task.title, fontWeight = FontWeight.SemiBold)
            if (task.scheduledTime.isNotEmpty()) {
                Text(task.scheduledTime, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false }, title = { Text("Delete Task?") }, text = { Text("Remove '${task.title}'?") },
            confirmButton = { Button(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } })
    }
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("⭐") }
    var goal by remember { mutableStateOf("30") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Habit") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Habit Name") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = icon, onValueChange = { icon = it }, label = { Text("Icon/Emoji") })
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal Days") })
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onAdd(name, icon, goal.toIntOrNull() ?: 30) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Task / Timetable") },
        text = {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") })
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val cal = Calendar.getInstance()
                    TimePickerDialog(context, { _, h, m ->
                        val ampm = if (h >= 12) "PM" else "AM"
                        val hour = if (h > 12) h - 12 else if (h == 0) 12 else h
                        time = String.format("%02d:%02d %s", hour, m, ampm)
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                }) { Text(if (time.isEmpty()) "Select Time" else "Time: $time") }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onAdd(title, time) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
