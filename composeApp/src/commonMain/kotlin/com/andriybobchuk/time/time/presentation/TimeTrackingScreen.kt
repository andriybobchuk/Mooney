package com.andriybobchuk.time.time.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.time.domain.TimeBlock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import com.andriybobchuk.time.core.presentation.DateTimeUtils
import com.andriybobchuk.time.core.presentation.Icons
import com.andriybobchuk.time.core.presentation.Toolbars
import com.andriybobchuk.time.core.presentation.bottomSheetBackground
import com.andriybobchuk.time.core.presentation.buttonBackground
import com.andriybobchuk.time.core.presentation.buttonTextColor
import com.andriybobchuk.time.core.presentation.cardBackground
import com.andriybobchuk.time.core.presentation.secondaryTextColor
import com.andriybobchuk.time.core.presentation.textColor
import com.andriybobchuk.time.time.data.TimeDataSource
import com.andriybobchuk.time.time.domain.Job
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackingScreen(
    viewModel: TimeTrackingViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Time Blocks",
                customContent = {
                    DateSelectorInTopBar(
                        selectedDate = state.selectedDate,
                        onDateSelected = { date ->
                            viewModel.onAction(TimeTrackingAction.SelectDate(date))
                        }
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { bottomNavbar() },
        // floatingActionButton = {
        //     FloatingActionButton(
        //         onClick = {
        //             viewModel.onAction(TimeTrackingAction.ShowAddSheet)
        //         },
        //         containerColor = Color.Black,
        //         contentColor = Color.White
        //     ) {
        //         Icon(
        //             modifier = Modifier.size(18.dp),
        //             painter = Icons.AddIcon(),
        //             contentDescription = ""
        //         )
        //     }
        // },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Calendar view (scrollable)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val sortedBlocks = state.timeBlocks.sortedBy { it.startTime }
                
                if (sortedBlocks.isNotEmpty()) {
                    val startHour = sortedBlocks.first().startTime.hour
                    val endHour = (sortedBlocks.last().endTime ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())).hour
                    
                                        // Show events and empty hours in chronological order
                    val shownHourLabels = mutableSetOf<Int>()
                    
                    for (hour in startHour..endHour) {
                        // Check if this hour is covered by any event's duration
                        val isHourCovered = sortedBlocks.any { timeBlock ->
                            val eventStartHour = timeBlock.startTime.hour
                            val eventEndHour = (timeBlock.endTime ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())).hour
                            hour in eventStartHour..eventEndHour
                        }
                        
                        if (isHourCovered) {
                            // This hour is covered by an event, show events that start in this hour
                            val eventsThisHour = sortedBlocks.filter { it.startTime.hour == hour }
                            eventsThisHour.forEach { timeBlock ->
                                // Show hour label only once per hour
                                if (hour !in shownHourLabels) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                               // .height(calendarConfig.hourHeight),
                                                .height(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${hour}:00",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.secondaryTextColor(),
                                                modifier = Modifier.width(50.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.secondaryTextColor().copy(alpha = 0.2f))
                                            )
                                        }
                                    }
                                    shownHourLabels.add(hour)
                                }
                                
                                // Show the event
                                item {
                                    val durationHours = timeBlock.getDurationInHours()
                                    val heightMultiplier = maxOf(durationHours, 1.0)
                                    val eventHeight = maxOf(calendarConfig.hourHeight * heightMultiplier.toFloat(), calendarConfig.minEventHeight)
                                    TimeBlockCard(
                                        timeBlock = timeBlock,
                                        onDelete = {
                                            viewModel.onAction(TimeTrackingAction.DeleteTimeBlock(timeBlock.id))
                                        },
                                        onEdit = {
                                            viewModel.onAction(TimeTrackingAction.EditTimeBlock(timeBlock))
                                        },
                                        modifier = Modifier.height(eventHeight)
                                    )
                                }
                            }
                        } else {
                            // This hour is truly empty (gap), show label
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                       // .height(calendarConfig.hourHeight),
                                        .height(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${hour}:00",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.secondaryTextColor(),
                                        modifier = Modifier.width(50.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.secondaryTextColor().copy(alpha = 0.2f))
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Calculate summary data
                        val totalHours = sortedBlocks.sumOf { it.getDurationInHours() }
                        val jobBreakdown = sortedBlocks
                            .groupBy { it.jobId }
                            .mapValues { (jobId, blocks) ->
                                val jobHours = blocks.sumOf { it.getDurationInHours() }
                                val percentage = if (totalHours > 0) ((jobHours / totalHours) * 100).toInt() else 0
                                com.andriybobchuk.time.time.domain.JobSummary(
                                    jobId = jobId,
                                    jobName = blocks.first().jobName,
                                    totalHours = jobHours,
                                    percentage = percentage.toDouble()
                                )
                            }
                        
                        val summary = com.andriybobchuk.time.time.domain.DailySummary(
                            date = state.selectedDate,
                            blocks = sortedBlocks,
                            totalHours = totalHours,
                            jobBreakdown = jobBreakdown
                        )
                        Spacer(Modifier.height(16.dp))
                        TotalSummaryCard(summary = summary)
                        Spacer(Modifier.height(70.dp))
                    }
                } else {
                    // Empty state
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No time blocks for this day, yet",
                                color = MaterialTheme.colorScheme.secondaryTextColor(),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            // Floating JobButtons at the bottom
            JobButtons(
                jobs = state.jobs,
                activeTimeBlock = state.activeTimeBlock,
                onStartTracking = { jobId ->
                    viewModel.onAction(TimeTrackingAction.StartTracking(jobId))
                },
                onStopTracking = {
                    viewModel.onAction(TimeTrackingAction.StopTracking)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
    
    // Edit Time Block Bottom Sheet
    if (state.showEditSheet && state.editingTimeBlock != null) {
        EditTimeBlockSheet(
            timeBlock = state.editingTimeBlock!!,
            jobs = state.jobs,
            onDismiss = {
                viewModel.onAction(TimeTrackingAction.HideEditSheet)
            },
            onSave = { updatedTimeBlock ->
                viewModel.onAction(TimeTrackingAction.UpdateTimeBlock(updatedTimeBlock))
            }
        )
    }
    
    // Add Time Block Bottom Sheet
    if (state.showAddSheet) {
        AddTimeBlockSheet(
            jobs = state.jobs,
            onDismiss = {
                viewModel.onAction(TimeTrackingAction.HideAddSheet)
            },
            onSave = { jobId, startTime, endTime ->
                viewModel.onAction(TimeTrackingAction.AddTimeBlock(jobId, startTime, endTime))
            }
        )
    }
}

@Composable
fun DateSelectorInTopBar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Generate last 7 days for dropdown
    val dateOptions = remember {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        (0..6).map { daysAgo ->
            currentDate.minus(DatePeriod(days = daysAgo))
        }.reversed()
    }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.buttonBackground()
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Text(
                text = DateTimeUtils.formatDate(selectedDate),
                color = MaterialTheme.colorScheme.buttonTextColor()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dateOptions.forEach { date ->
                DropdownMenuItem(
                    text = { Text(DateTimeUtils.formatDateWithYear(date)) },
                    onClick = {
                        onDateSelected(date)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TimeBlockCard(
    timeBlock: TimeBlock,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get job color from TimeDataSource
    val jobColor = remember(timeBlock.jobId) {
        val job = TimeDataSource.jobs.find { it.id == timeBlock.jobId }
        job?.color?.let { Color(it) }
    }?:Color(0xFF808080)
    
    var showContextMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .padding(vertical = 2.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showContextMenu = true }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = jobColor.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Row {
                    Text(
                        text = timeBlock.jobName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.textColor()
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = DateTimeUtils.formatDuration(timeBlock.getDurationInHours()),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.textColor()
                    )
                }
                Spacer(Modifier.weight(1f))
                Row {
                    val end = timeBlock.endTime?.let { endTime ->
                        DateTimeUtils.formatTime(endTime)
                    }
                    Text(
                        text = "${DateTimeUtils.formatTime(timeBlock.startTime)} - ${end?:"In Progress"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondaryTextColor()
                    )
                }
            }
        }
    }
    
    // Context menu
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                onEdit()
                showContextMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDelete()
                showContextMenu = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimeBlockSheet(
    timeBlock: TimeBlock,
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onSave: (TimeBlock) -> Unit
) {
    var selectedJobId by remember { mutableStateOf(timeBlock.jobId) }
    var startTimeText by remember { mutableStateOf(DateTimeUtils.formatTime(timeBlock.startTime)) }
    var endTimeText by remember { mutableStateOf(timeBlock.endTime?.let { DateTimeUtils.formatTime(it) } ?: "") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.bottomSheetBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job selection
            var jobExpanded by remember { mutableStateOf(false) }
            val selectedJob = jobs.find { it.id == selectedJobId }

            Text("Edit Time Block", color = MaterialTheme.colorScheme.textColor())
            Text("Project", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.textColor())
            Box {
                Button(
                    onClick = { jobExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.buttonBackground()
                    )
                ) {
                    Text(
                        text = selectedJob?.name ?: "Select Project",
                        color = MaterialTheme.colorScheme.buttonTextColor()
                    )
                }
                
                DropdownMenu(
                    expanded = jobExpanded,
                    onDismissRequest = { jobExpanded = false }
                ) {
                    jobs.forEach { job ->
                        DropdownMenuItem(
                            text = { Text(job.name) },
                            onClick = {
                                selectedJobId = job.id
                                jobExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start time
            Text("Start Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End time
            Text("End Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.textColor())
                }
                
                Button(
                    onClick = {
                        // Parse time and create updated time block
                        val startTime = parseTimeString(startTimeText, timeBlock.startTime.date)
                        val endTime = if (endTimeText.isNotEmpty()) parseTimeString(endTimeText, timeBlock.startTime.date) else null
                        
                        if (startTime != null) {
                            val updatedTimeBlock = timeBlock.copy(
                                jobId = selectedJobId,
                                jobName = selectedJob?.name ?: timeBlock.jobName,
                                startTime = startTime,
                                endTime = endTime
                            )
                            onSave(updatedTimeBlock)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.buttonBackground()
                    )
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.buttonTextColor())
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeBlockSheet(
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onSave: (String, kotlinx.datetime.LocalDateTime, kotlinx.datetime.LocalDateTime) -> Unit
) {
    var selectedJobId by remember { mutableStateOf("") }
    var startTimeText by remember { mutableStateOf("") }
    var endTimeText by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.bottomSheetBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job selection
            var jobExpanded by remember { mutableStateOf(false) }
            val selectedJob = jobs.find { it.id == selectedJobId }

            Text("Add Time Block", color = MaterialTheme.colorScheme.textColor())
            Text("Project", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.textColor())
            Box {
                Button(
                    onClick = { jobExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.buttonBackground()
                    )
                ) {
                    Text(
                        text = selectedJob?.name ?: "Select Project",
                        color = MaterialTheme.colorScheme.buttonTextColor()
                    )
                }
                
                DropdownMenu(
                    expanded = jobExpanded,
                    onDismissRequest = { jobExpanded = false }
                ) {
                    jobs.forEach { job ->
                        DropdownMenuItem(
                            text = { Text(job.name) },
                            onClick = {
                                selectedJobId = job.id
                                jobExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start time
            Text("Start Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End time
            Text("End Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val startTime = parseTimeString(startTimeText, currentDate)
                        val endTime = parseTimeString(endTimeText, currentDate)
                        
                        if (startTime != null && endTime != null && selectedJobId.isNotEmpty()) {
                            onSave(selectedJobId, startTime, endTime)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun parseTimeString(timeString: String, date: kotlinx.datetime.LocalDate): kotlinx.datetime.LocalDateTime? {
    return try {
        val parts = timeString.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            if (hour in 0..23 && minute in 0..59) {
                kotlinx.datetime.LocalDateTime(date, kotlinx.datetime.LocalTime(hour, minute))
            } else null
        } else null
    } catch (e: Exception) {
        null
    }
}

@Composable
fun TotalSummaryCard(summary: com.andriybobchuk.time.time.domain.DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackground()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${DateTimeUtils.formatDuration(summary.totalHours)} in total",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.textColor()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            summary.jobBreakdown.values.toList().forEach { jobSummary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${jobSummary.jobName}: ${DateTimeUtils.formatDuration(jobSummary.totalHours)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.textColor()
                    )
                    Text(
                        text = "${jobSummary.percentage}%",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondaryTextColor()
                    )
                }
            }
        }
    }
}

@Composable
fun JobButtons(
    jobs: List<com.andriybobchuk.time.time.domain.Job>,
    activeTimeBlock: TimeBlock?,
    onStartTracking: (String) -> Unit,
    onStopTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 5.dp)
    ) {
        if (activeTimeBlock != null) {
            // Stop button
            Button(
                onClick = onStopTracking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.buttonBackground()
                )
            ) {
                Text(
                    "Stop Tracking (${activeTimeBlock.jobName})",
                    color = MaterialTheme.colorScheme.buttonTextColor()
                )
            }
        } else {
            // Job buttons
            Row(
                modifier = Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                jobs.forEach { job ->
                    Button(
                        onClick = { onStartTracking(job.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(job.color)
                        )
                    ) {
                        Row {
                            Icon(
                                modifier = Modifier.size(18.dp),
                                painter = Icons.AddIcon(),
                                tint = MaterialTheme.colorScheme.textColor(),
                                contentDescription = ""
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = job.name,
                                color = MaterialTheme.colorScheme.textColor()
                            )
                        }
                    }
                }
            }
        }
    }
} 

// --- Calendar View Config ---
data class CalendarViewConfig(
    val hourHeight: Dp = 40.dp,
    val minEventHeight: Dp = 40.dp
)

val calendarConfig = CalendarViewConfig() 