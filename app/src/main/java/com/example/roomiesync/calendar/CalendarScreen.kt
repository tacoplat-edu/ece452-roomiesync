@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.chore.ChoreAssignment
import com.example.roomiesync.chore.ChoreViewModel
import kotlin.time.ExperimentalTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val iosBlue = Color(0xFF007AFF)
private val iosBackground = Color(0xFFF2F2F7)
private val iosCard = Color.White
private val iosMuted = Color(0xFF8E8E93)
private val iosDivider = Color(0xFFE5E5EA)
private val overdueColor = Color(0xFFFF453A)
private val urgentColor = Color(0xFFFF9F0A)
private val upcomingColor = Color(0xFF30D158)

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: ChoreViewModel = viewModel(),
    onCreateChoreForDay: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayStart = remember { startOfDayMillis(System.currentTimeMillis()) }
    var visibleMonthStart by rememberSaveable { mutableLongStateOf(startOfMonthMillis(todayStart)) }
    var selectedDayStart by rememberSaveable { mutableLongStateOf(todayStart) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val choresByDay = remember(uiState.chores) { groupChoresByDay(uiState.chores) }
    val monthCells = remember(visibleMonthStart) { buildMonthCells(visibleMonthStart) }
    val selectedDayChores = choresByDay[selectedDayStart].orEmpty().sortedBy { it.dueDate.toEpochMilliseconds() }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val selectedDateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    if (uiState.isLoading && uiState.chores.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(iosBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = iosBlue)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iosBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MonthHeader(
            monthLabel = monthFormatter.format(Date(visibleMonthStart)),
            onPreviousMonth = {
                visibleMonthStart = shiftMonth(visibleMonthStart, -1)
            },
            onNextMonth = {
                visibleMonthStart = shiftMonth(visibleMonthStart, 1)
            },
            onTodayClick = {
                visibleMonthStart = startOfMonthMillis(todayStart)
                selectedDayStart = todayStart
            }
        )

        WeekdayLabels()

        CalendarMonthGrid(
            cells = monthCells,
            selectedDayStart = selectedDayStart,
            choresByDay = choresByDay,
            currentMonthStart = visibleMonthStart,
            onDayClick = { dayStart ->
                selectedDayStart = dayStart
                if (!isSameMonth(dayStart, visibleMonthStart)) {
                    visibleMonthStart = startOfMonthMillis(dayStart)
                }
            }
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = iosCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, iosDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDateFormatter.format(Date(selectedDayStart)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(iosBlue.copy(alpha = 0.12f))
                            .clickable { onCreateChoreForDay(toDefaultDueTimeMillis(selectedDayStart)) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Schedule chore",
                            tint = iosBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Schedule chore",
                            color = iosBlue,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                if (selectedDayChores.isEmpty()) {
                    Text(
                        text = "No chores scheduled for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = iosMuted
                    )
                } else {
                    selectedDayChores.forEach { chore ->
                        DayChoreRow(
                            chore = chore,
                            timeLabel = timeFormatter.format(Date(chore.dueDate.toEpochMilliseconds()))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    monthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = iosBlue
            )
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Today",
            color = iosBlue,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onTodayClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = iosBlue
            )
        }
    }
}

@Composable
private fun WeekdayLabels() {
    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = iosMuted
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    cells: List<CalendarDayCell>,
    selectedDayStart: Long,
    choresByDay: Map<Long, List<ChoreAssignment>>,
    currentMonthStart: Long,
    onDayClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { cell ->
                    val isSelected = cell.startOfDayMillis == selectedDayStart
                    val isCurrentMonth = isSameMonth(cell.startOfDayMillis, currentMonthStart)
                    val dayChores = choresByDay[cell.startOfDayMillis].orEmpty()

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clickable { onDayClick(cell.startOfDayMillis) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) iosBlue else iosCard
                        ),
                        border = BorderStroke(1.dp, if (isSelected) iosBlue else iosDivider)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cell.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    isSelected -> Color.White
                                    isCurrentMonth -> Color.Black
                                    else -> iosMuted
                                },
                                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Medium
                            )

                            if (dayChores.isNotEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    dayChores.take(3).forEach { chore ->
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    color = if (isSelected) {
                                                        Color.White
                                                    } else {
                                                        choreColor(chore.status)
                                                    }
                                                )
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayChoreRow(
    chore: ChoreAssignment,
    timeLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(choreColor(chore.status))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chore.chore?.title ?: "Untitled chore",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = iosMuted
            )
        }
    }
}

private data class CalendarDayCell(
    val startOfDayMillis: Long,
    val dayOfMonth: Int,
    val isToday: Boolean
)

private fun choreColor(status: String): Color {
    return when (status) {
        "OVERDUE" -> overdueColor
        "URGENT" -> urgentColor
        else -> upcomingColor
    }
}

private fun groupChoresByDay(chores: List<ChoreAssignment>): Map<Long, List<ChoreAssignment>> {
    return chores.groupBy { assignment ->
        startOfDayMillis(assignment.dueDate.toEpochMilliseconds())
    }
}

private fun buildMonthCells(monthStartMillis: Long): List<CalendarDayCell> {
    val todayStart = startOfDayMillis(System.currentTimeMillis())
    val monthStartCal = Calendar.getInstance().apply {
        timeInMillis = monthStartMillis
    }
    val firstWeekdayOffset = monthStartCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

    val gridStart = Calendar.getInstance().apply {
        timeInMillis = monthStartMillis
        add(Calendar.DAY_OF_MONTH, -firstWeekdayOffset)
    }

    return buildList(capacity = 42) {
        repeat(42) {
            val dayStart = startOfDayMillis(gridStart.timeInMillis)
            add(
                CalendarDayCell(
                    startOfDayMillis = dayStart,
                    dayOfMonth = gridStart.get(Calendar.DAY_OF_MONTH),
                    isToday = dayStart == todayStart
                )
            )
            gridStart.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

private fun startOfDayMillis(epochMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfMonthMillis(dayStartMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = dayStartMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun shiftMonth(monthStartMillis: Long, monthOffset: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = monthStartMillis
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun isSameMonth(dayStartMillis: Long, monthStartMillis: Long): Boolean {
    val dayCal = Calendar.getInstance().apply { timeInMillis = dayStartMillis }
    val monthCal = Calendar.getInstance().apply { timeInMillis = monthStartMillis }
    return dayCal.get(Calendar.YEAR) == monthCal.get(Calendar.YEAR) &&
        dayCal.get(Calendar.MONTH) == monthCal.get(Calendar.MONTH)
}

private fun toDefaultDueTimeMillis(dayStartMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = dayStartMillis
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
