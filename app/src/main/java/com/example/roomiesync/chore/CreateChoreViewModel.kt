package com.example.roomiesync.chore

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

enum class CreateChoreRecurrenceType {
    ONE_TIME,
    RECURRING
}

enum class RecurrenceUnit(val label: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months")
}

@Serializable
data class Profile(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("created_at") val createdAt: String // simplified for now
)

data class CreateChoreState(
    val title: String = "",
    val description: String = "",
    val isRecurring: Boolean = false,
    val recurrenceInterval: String = "1",
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
    val members: List<Profile> = emptyList(),
    val assignedTo: Profile? = null,
    val dueDate: Long? = null, // Epoch millis for DatePicker
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CreateChoreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateChoreState())
    val uiState: StateFlow<CreateChoreState> = _uiState.asStateFlow()

    init {
        // TODO: Pull user profiles from API
        // Mock data for members
        val mockProfiles = listOf(
            Profile("1", "alice@example.com", "Alice", "2023-01-01T00:00:00Z"),
            Profile("2", "bob@example.com", "Bob", "2023-01-02T00:00:00Z"),
            Profile("3", "charlie@example.com", "Charlie", "2023-01-03T00:00:00Z")
        )
        _uiState.update { it.copy(members = mockProfiles) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun toggleRecurring(isRecurring: Boolean) {
        _uiState.update { it.copy(isRecurring = isRecurring) }
    }

    fun updateRecurrenceInterval(interval: String) {
        if (interval.all { it.isDigit() }) {
            _uiState.update { it.copy(recurrenceInterval = interval) }
        }
    }

    fun updateRecurrenceUnit(unit: RecurrenceUnit) {
        _uiState.update { it.copy(recurrenceUnit = unit) }
    }

    fun updateAssignedTo(profile: Profile) {
        _uiState.update { it.copy(assignedTo = profile) }
    }

    fun updateDueDate(dueDateMillis: Long?) {
        _uiState.update { it.copy(dueDate = dueDateMillis) }
    }

    fun saveChore(onSuccess: () -> Unit) {
        // Validation logic
        val currentState = _uiState.value
        if (currentState.title.isBlank() || currentState.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title and description cannot be empty") }
            return
        }
        
        if (currentState.assignedTo == null) {
             _uiState.update { it.copy(errorMessage = "Please assign the chore to someone") }
             return
        }
        
        if (currentState.dueDate == null) {
             _uiState.update { it.copy(errorMessage = "Please select a due date") }
             return
        }

        if (currentState.isRecurring) {
            val interval = currentState.recurrenceInterval.toIntOrNull()
            if (interval == null || interval <= 0) {
                _uiState.update { it.copy(errorMessage = "Invalid recurrence interval") }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Simulate network call or DB operation
        // In a real app, you would call a repository here
        // For now, we'll just simulate success
        // TODO: Submit form data to API

        _uiState.update { it.copy(isLoading = false) }
        onSuccess()
    }
}
