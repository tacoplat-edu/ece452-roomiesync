@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.chore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.ui.components.ChoreListItem
import com.example.roomiesync.ui.components.ChoreStatus
import com.example.roomiesync.ui.components.FullWidthButtonWithIcon
import com.example.roomiesync.ui.components.PillButton
import com.example.roomiesync.ui.components.SearchTextField
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun ChoreScreen(
    viewModel: ChoreViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Background color from design
            .padding(top = 48.dp) // Top padding from design
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Chores",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // Filters and Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = !uiState.isSearchFocused,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillButton(text = "Sort", onClick = { /* TODO: Implement sort */ })
                    Spacer(modifier = Modifier.width(10.dp))
                    PillButton(text = "Filters", onClick = { /* TODO: Implement filters */ })
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            
            SearchTextField(
                value = uiState.searchText,
                onValueChange = { viewModel.onSearchTextChanged(it) },
                onFocusChanged = { viewModel.onSearchFocusChanged(it) },
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.weight(1f)
            )
        }

        // Chore List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.chores) { choreAssignment ->
                val chore = choreAssignment.chore
                if (chore != null) {
                    // Calculate time left
                    val timeText = getRelativeTimeText(choreAssignment.dueDate)
                    
                    // Map status string to ChoreStatus enum
                    val visualStatus = try {
                        ChoreStatus.valueOf(choreAssignment.status)
                    } catch (e: IllegalArgumentException) {
                        ChoreStatus.NOT_URGENT
                    }

                    ChoreListItem(
                        choreName = chore.title,
                        timeLeft = timeText,
                        status = visualStatus,
                        onComplete = { viewModel.completeChore(choreAssignment.id) }
                    )
                }
            }
        }

        // Add Chore Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            FullWidthButtonWithIcon(
                text = "Add a new chore",
                icon = Icons.Outlined.AddCircle,
                onClick = { viewModel.addChore() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun getRelativeTimeText(dueDate: Instant): String {
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
