package com.example.roomiesync.chore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class CreateChoreRecurrenceType {
    ONE_TIME,
    RECURRING
}

enum class RecurrenceUnit(val label: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months")
}

data class CreateChoreState(
    val title: String = "",
    val description: String = "",
    val isRecurring: Boolean = false,
    val recurrenceInterval: String = "1",
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
    val members: List<Profile> = emptyList(),
    val assignedTo: Profile? = null,
    val dueDate: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CreateChoreViewModel(
    private val choreRepository: ChoreRepository = ChoreRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateChoreState())
    val uiState: StateFlow<CreateChoreState> = _uiState.asStateFlow()

    private var houseId: String? = null

    init {
        loadHouseMembers()
    }

    private fun loadHouseMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val house = choreRepository.getUserHouse(userId)
                houseId = house?.id
                if (house != null) {
                    val members = choreRepository.getHouseMembers(house.id)
                    _uiState.update { it.copy(members = members, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No household found") }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
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
        val currentState = _uiState.value
        val currentHouseId = houseId

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
        if (currentHouseId == null) {
            _uiState.update { it.copy(errorMessage = "No household found") }
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

        viewModelScope.launch {
            val recurrenceType = if (currentState.isRecurring) {
                "${currentState.recurrenceInterval}_${currentState.recurrenceUnit.name.lowercase()}"
            } else {
                "none"
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dueDateIso = isoFormat.format(Date(currentState.dueDate))

            choreRepository.createChoreWithAssignment(
                houseId = currentHouseId,
                title = currentState.title,
                description = currentState.description,
                recurrenceType = recurrenceType,
                assignedToId = currentState.assignedTo.id,
                dueDateIso = dueDateIso
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }.onFailure { e ->
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to save chore") }
            }
        }
    }
}
