@file:OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)

package com.example.roomiesync.chore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.ui.components.ChoreListItem
import com.example.roomiesync.ui.components.ChoreStatus
import com.example.roomiesync.ui.components.FullWidthButtonWithIcon
import com.example.roomiesync.ui.components.PillButton
import com.example.roomiesync.ui.components.SearchTextField
import com.example.roomiesync.ui.theme.ErrorRed
import com.example.roomiesync.ui.theme.PrimaryBackground
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.Typography
import com.example.roomiesync.ui.theme.WarningYellow
import com.example.roomiesync.data.SupabaseClient
import com.example.roomiesync.utils.getRelativeTimeText
import io.github.jan.supabase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.ExperimentalTime

@Composable
fun ChoreScreen(
    viewModel: ChoreViewModel = viewModel(),
    onAddChoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val queryParams by viewModel.queryParams.collectAsState()
    val focusManager = LocalFocusManager.current
    var selectedChore by remember { mutableStateOf<ChoreAssignment?>(null) }
    var reviewChore by remember { mutableStateOf<ChoreAssignment?>(null) }
    val currentUserId = remember { SupabaseClient.client.auth.currentUserOrNull()?.id }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = PrimaryBackground)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                    PillButton(text = "Sort", onClick = { showSortSheet = true })
                    Spacer(modifier = Modifier.width(10.dp))
                    PillButton(text = "Filters", onClick = { showFilterSheet = true })
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
                    val timeText = getRelativeTimeText(choreAssignment.dueDate)

                    val visualStatus = try {
                        ChoreStatus.valueOf(choreAssignment.status)
                    } catch (e: IllegalArgumentException) {
                        ChoreStatus.NOT_URGENT
                    }

                    ChoreListItem(
                        choreName = chore.title,
                        timeLeft = timeText,
                        status = visualStatus,
                        onComplete = {
                            if (visualStatus == ChoreStatus.PENDING_APPROVAL
                                && choreAssignment.assignedToId != currentUserId
                            ) {
                                reviewChore = choreAssignment
                            } else if (visualStatus != ChoreStatus.PENDING_APPROVAL) {
                                selectedChore = choreAssignment
                            }
                        }
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
                icon = Icons.Outlined.Add,
                onClick = onAddChoreClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showSortSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            SortSheetContent(
                currentParams = queryParams,
                onParamsChange = { newParams ->
                    viewModel.updateQueryParams(newParams)
                },
                onReset = {
                    viewModel.updateQueryParams(
                        queryParams.copy(
                            sortBy = SortField.TIME_UNTIL_DUE,
                            sortDirection = SortDirection.ASCENDING
                        )
                    )
                }
            )
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            FilterSheetContent(
                currentParams = queryParams,
                onParamsChange = { newParams ->
                    viewModel.updateQueryParams(newParams)
                },
                onReset = {
                    viewModel.updateQueryParams(ChoreQueryParams()) // Reset to defaults
                }
            )
        }
    }

    if (selectedChore != null) {
        ChoreSubmissionDialog(
            choreAssignment = selectedChore!!,
            onDismiss = { selectedChore = null },
            onSubmit = { photoUri ->
                // TODO: Upload the photo to S3 and report the link to the backend
                viewModel.submitChore(selectedChore!!.id, photoUri)
                selectedChore = null
            }
        )
    }

    if (reviewChore != null) {
        KudosDialog(
            choreAssignment = reviewChore!!,
            onDismiss = { reviewChore = null },
            onApprove = { id -> viewModel.approveChore(id) },
            onReject = { id -> viewModel.rejectChore(id) }
        )
    }
}

@Composable
fun SortSheetContent(
    currentParams: ChoreQueryParams,
    onParamsChange: (ChoreQueryParams) -> Unit,
    onReset: () -> Unit
) {
    val sortBy = currentParams.sortBy ?: SortField.TIME_UNTIL_DUE
    val sortDirection = currentParams.sortDirection
    
    var isSortByExpanded by remember { mutableStateOf(false) }
    var isDirectionExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort Chores",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (sortBy != SortField.TIME_UNTIL_DUE || sortDirection != SortDirection.ASCENDING) {
                TextButton(onClick = onReset) {
                    Text("Reset", color = PrimaryGreen)
                }
            }
        }

        // Sort By Dropdown
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Sort by", style = Typography.labelMedium)
            Box {
                OutlinedTextField(
                    value = when (sortBy) {
                        SortField.CHORE_NAME -> "Chore Name"
                        SortField.TIME_UNTIL_DUE -> "Time Until Due"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = PrimaryGreen
                    )
                )
                Box(modifier = Modifier
                    .matchParentSize()
                    .clickable { isSortByExpanded = true })
                
                DropdownMenu(
                    expanded = isSortByExpanded,
                    onDismissRequest = { isSortByExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    SortField.entries.forEach { field ->
                        DropdownMenuItem(
                            text = { 
                                Text(when (field) {
                                    SortField.CHORE_NAME -> "Chore Name"
                                    SortField.TIME_UNTIL_DUE -> "Time Until Due"
                                }) 
                            },
                            onClick = {
                                onParamsChange(currentParams.copy(sortBy = field))
                                isSortByExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Sort Direction Dropdown
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Order", style = Typography.labelMedium)
            Box {
                OutlinedTextField(
                    value = when (sortDirection) {
                        SortDirection.ASCENDING -> "Ascending"
                        SortDirection.DESCENDING -> "Descending"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = PrimaryGreen
                    )
                )
                Box(modifier = Modifier
                    .matchParentSize()
                    .clickable { isDirectionExpanded = true })
                
                DropdownMenu(
                    expanded = isDirectionExpanded,
                    onDismissRequest = { isDirectionExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    SortDirection.entries.forEach { direction ->
                        DropdownMenuItem(
                            text = { 
                                Text(when (direction) {
                                    SortDirection.ASCENDING -> "Ascending"
                                    SortDirection.DESCENDING -> "Descending"
                                }) 
                            },
                            onClick = {
                                onParamsChange(currentParams.copy(sortDirection = direction))
                                isDirectionExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSheetContent(
    currentParams: ChoreQueryParams,
    onParamsChange: (ChoreQueryParams) -> Unit,
    onReset: () -> Unit
) {
    val statuses = currentParams.statuses
    val assignedToMeOnly = currentParams.assignedToMeOnly
    val startDate = currentParams.dueDateStart
    val endDate = currentParams.dueDateEnd
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter Chores",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (statuses.isNotEmpty() || !assignedToMeOnly || startDate != null || endDate != null) {
                TextButton(onClick = onReset) {
                    Text("Reset", color = PrimaryGreen)
                }
            }
        }

        // Statuses
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Status", style = Typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(ChoreStatus.URGENT, ChoreStatus.OVERDUE, ChoreStatus.PENDING_APPROVAL).forEach { status ->
                    val color = when (status) {
                        ChoreStatus.URGENT -> WarningYellow
                        ChoreStatus.OVERDUE -> ErrorRed
                        ChoreStatus.PENDING_APPROVAL -> Color(0xFF2196F3)
                        else -> PrimaryGreen
                    }
                    
                    FilterChip(
                        selected = statuses.contains(status),
                        onClick = {
                            val newStatuses = if (statuses.contains(status)) {
                                statuses - status
                            } else {
                                statuses + status
                            }
                            onParamsChange(currentParams.copy(statuses = newStatuses))
                        },
                        label = { Text(status.name.replace("_", " ")) },
                        leadingIcon = if (statuses.contains(status)) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color,
                            selectedLeadingIconColor = color,
                            labelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Assigned to me
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Assigned to me", style = Typography.labelMedium)
            Switch(
                checked = assignedToMeOnly,
                onCheckedChange = { onParamsChange(currentParams.copy(assignedToMeOnly = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryGreen
                )
            )
        }

        // Due Date Range
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Due Date Range", style = Typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Start Date
                Box(modifier = Modifier.weight(1f)) {
                    val dateText = startDate?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: ""
                    
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Start Date") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = PrimaryGreen
                        )
                    )
                    Box(modifier = Modifier
                        .matchParentSize()
                        .clickable { showStartDatePicker = true })
                }
                
                // End Date
                Box(modifier = Modifier.weight(1f)) {
                    val dateText = endDate?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it))
                    } ?: ""
                    
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("End Date") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = PrimaryGreen
                        )
                    )
                    Box(modifier = Modifier
                        .matchParentSize()
                        .clickable { showEndDatePicker = true })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
    
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onParamsChange(currentParams.copy(dueDateStart = datePickerState.selectedDateMillis))
                    showStartDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = PrimaryGreen)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onParamsChange(currentParams.copy(dueDateEnd = datePickerState.selectedDateMillis))
                    showEndDatePicker = false
                }) {
                    Text("OK", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = PrimaryGreen)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
