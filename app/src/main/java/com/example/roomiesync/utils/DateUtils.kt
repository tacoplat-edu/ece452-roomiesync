package com.example.roomiesync.utils

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun getRelativeTimeText(dueDate: Instant): String {
    val now = Clock.System.now()
    val duration = dueDate - now
    val isOverdue = duration.isNegative()
    val absDuration = duration.absoluteValue
    
    val days = absDuration.inWholeDays
    val hours = absDuration.inWholeHours
    val minutes = absDuration.inWholeMinutes
    
    val timeString = when {
        days > 0 -> "$days days"
        hours > 0 -> "$hours hours"
        else -> "$minutes minutes"
    }
    
    return if (isOverdue) "$timeString overdue" else "$timeString left"
}
