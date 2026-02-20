package com.example.roomiesync.chore

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomiesync.ui.components.FullWidthButtonWithIcon
import com.example.roomiesync.ui.components.getRequiredLabel
import com.example.roomiesync.ui.theme.PrimaryGreen
import com.example.roomiesync.ui.theme.Typography
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChoreFormScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateChoreViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRecurrenceUnitDropdownExpanded by remember { mutableStateOf(false) }
    var isAssigneeDropdownExpanded by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempSelectedDateMillis by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()), // Make it scrollable
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "Add a new chore",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    )

                    // Title
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = getRequiredLabel("Title"),
                            style = Typography.labelMedium,
                            color = Color.Black
                        )
                        OutlinedTextField(
                            value = uiState.title,
                            onValueChange = viewModel::updateTitle,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = PrimaryGreen
                            )
                        )
                    }

                    // Description
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = getRequiredLabel("Description"),
                            style = Typography.labelMedium,
                            color = Color.Black
                        )
                        OutlinedTextField(
                            value = uiState.description,
                            onValueChange = viewModel::updateDescription,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = PrimaryGreen
                            )
                        )
                    }

                    // Assign To
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = getRequiredLabel("Assign to"),
                            style = Typography.labelMedium,
                            color = Color.Black
                        )
                        Box {
                            OutlinedTextField(
                                value = uiState.assignedTo?.displayName ?: "",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select member"
                                    )
                                },
                                placeholder = { Text("Select a household member") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = PrimaryGreen
                                ),
                                enabled = false
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isAssigneeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = isAssigneeDropdownExpanded,
                                onDismissRequest = { isAssigneeDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f) // Optional: restrict width
                            ) {
                                uiState.members.forEach { profile ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(text = profile.displayName)
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateAssignedTo(profile)
                                            isAssigneeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Due Date
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = getRequiredLabel("Due date"),
                            style = Typography.labelMedium,
                            color = Color.Black
                        )
                        Box {
                            val dateText = uiState.dueDate?.let {
                                val date = Date(it)
                                val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                formatter.format(date)
                            } ?: ""

                            OutlinedTextField(
                                value = dateText,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Select date"
                                    )
                                },
                                placeholder = { Text("Select a due date") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = PrimaryGreen
                                ),
                                enabled = false
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }
                    }

                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = uiState.dueDate ?: System.currentTimeMillis()
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    tempSelectedDateMillis = datePickerState.selectedDateMillis
                                    showDatePicker = false
                                    showTimePicker = true
                                }) {
                                    Text("Next")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    if (showTimePicker) {
                        val initialTime = Calendar.getInstance().apply {
                            timeInMillis = uiState.dueDate ?: System.currentTimeMillis()
                        }
                        val timePickerState = rememberTimePickerState(
                            initialHour = initialTime.get(Calendar.HOUR_OF_DAY),
                            initialMinute = initialTime.get(Calendar.MINUTE),
                            is24Hour = true
                        )
                        
                        AlertDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    // Combine date and time
                                    val dateMillis = tempSelectedDateMillis ?: System.currentTimeMillis()
                                    val calendar = Calendar.getInstance().apply {
                                        // 1. Get the components from the UTC timestamp
                                        val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                                        utcCalendar.timeInMillis = dateMillis
                                        
                                        val year = utcCalendar.get(Calendar.YEAR)
                                        val month = utcCalendar.get(Calendar.MONTH)
                                        val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
                                        
                                        // 2. Set them to the local calendar
                                        set(year, month, day, timePickerState.hour, timePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    
                                    viewModel.updateDueDate(calendar.timeInMillis)
                                    showTimePicker = false
                                }) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTimePicker = false }) {
                                    Text("Cancel")
                                }
                            },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Enter time",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    TimeInput(state = timePickerState)
                                }
                            }
                        )
                    }

                    // Recurrence
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleRecurring(!uiState.isRecurring) }
                        ) {
                            Checkbox(
                                checked = uiState.isRecurring,
                                onCheckedChange = { viewModel.toggleRecurring(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryGreen,
                                    uncheckedColor = Color.Gray
                                )
                            )
                            Text(
                                text = "This is a recurring task",
                                style = Typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        if (uiState.isRecurring) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = getRequiredLabel("Every"),
                                    style = Typography.bodyLarge
                                )
                                
                                OutlinedTextField(
                                    value = uiState.recurrenceInterval,
                                    onValueChange = viewModel::updateRecurrenceInterval,
                                    modifier = Modifier.width(80.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryGreen,
                                        unfocusedBorderColor = PrimaryGreen
                                    )
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = uiState.recurrenceUnit.label,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select unit"
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryGreen,
                                            unfocusedBorderColor = PrimaryGreen
                                        ),
                                        enabled = false // Click handled by Box
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable { isRecurrenceUnitDropdownExpanded = true }
                                    )

                                    DropdownMenu(
                                        expanded = isRecurrenceUnitDropdownExpanded,
                                        onDismissRequest = { isRecurrenceUnitDropdownExpanded = false }
                                    ) {
                                        RecurrenceUnit.values().forEach { unit ->
                                            DropdownMenuItem(
                                                text = { Text(text = unit.label) },
                                                onClick = {
                                                    viewModel.updateRecurrenceUnit(unit)
                                                    isRecurrenceUnitDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val isIntervalValid = if (uiState.isRecurring) {
                        uiState.recurrenceInterval.isNotBlank() && uiState.recurrenceInterval.toIntOrNull()?.let { it > 0 } == true
                    } else {
                        true
                    }
                    
                    val isFormValid = uiState.title.isNotBlank() && 
                                      uiState.description.isNotBlank() && 
                                      isIntervalValid &&
                                      uiState.assignedTo != null &&
                                      uiState.dueDate != null

                    FullWidthButtonWithIcon(
                        text = "Save chore",
                        icon = Icons.Default.Check,
                        enabled = isFormValid && !uiState.isLoading,
                        onClick = {
                            viewModel.saveChore(onSuccess = onNavigateBack)
                        },
                    )
                    
                    // Add some bottom padding for better scroll experience
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
