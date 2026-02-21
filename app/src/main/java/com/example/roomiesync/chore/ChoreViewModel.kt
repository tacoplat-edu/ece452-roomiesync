@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.chore

import androidx.lifecycle.ViewModel
import com.example.roomiesync.ui.components.ChoreStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Clock

class ChoreViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChoreState())
    val uiState: StateFlow<ChoreState> = _uiState.asStateFlow()
    
    private val _queryParams = MutableStateFlow(ChoreQueryParams())
    val queryParams: StateFlow<ChoreQueryParams> = _queryParams.asStateFlow()
    
    // Store the complete list of chores locally for filtering
    private var allChores: List<ChoreAssignment> = emptyList()

    init {
        // We'll use Clock.System.now() and assume it returns the compatible Instant type
        val now = Clock.System.now()
        
        // Helper to create dummy chores
        fun createChoreAssignment(
            id: String,
            title: String,
            description: String,
            status: String,
            dueDate: Instant,
            assignedToId: String = "user-1"
        ): ChoreAssignment {
            val chore = Chore(
                id = "chore-$id",
                houseId = "house-1",
                title = title,
                description = description,
                recurrenceType = "weekly",
                createdAt = now
            )
            return ChoreAssignment(
                id = id,
                choreId = chore.id,
                assignedToId = assignedToId,
                status = status,
                dueDate = dueDate,
                chore = chore
            )
        }

        allChores = listOf(
            createChoreAssignment("1", "Break down cardboard", "Recycle the large boxes in the hallway", "OVERDUE", now.minus(1.days)),
            createChoreAssignment("2", "Take out the trash", "Empty kitchen and bathroom bins", "URGENT", now.plus(3.hours)),
            createChoreAssignment("3", "Sweep the floors", "Sweep the living room and kitchen", "NOT_URGENT", now.plus(1.days)),
            createChoreAssignment("4", "Clean the master bathroom", "Scrub the toilet and sink", "NOT_URGENT", now.plus(3.days)),
            createChoreAssignment("5", "Clean the master bathroom", "Wipe down the mirror", "NOT_URGENT", now.plus(3.days)),
            createChoreAssignment("6", "Clean the master bathroom", "Mop the floor", "NOT_URGENT", now.plus(3.days)),
            createChoreAssignment("7", "Clean the master bathroom", "Replenish toilet paper", "NOT_URGENT", now.plus(3.days)),
            createChoreAssignment("8", "Water plants", "Living room plants", "NOT_URGENT", now.plus(2.days), "user-2") // Someone else's chore
        )
        applyFilters()
    }

    fun completeChore(choreId: String) {
        // Placeholder implementation
//        _uiState.update { currentState ->
//            val updatedChores = currentState.chores.filter { it.id != choreId }
//            currentState.copy(chores = updatedChores)
//        }
    }

    fun addChore() {
        // Placeholder for adding a new chore
    }

    fun onSearchTextChanged(text: String) {
        _uiState.update { it.copy(searchText = text) }
        applyFilters()
    }
    
    fun onSearchFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isSearchFocused = focused) }
    }
    
    fun updateQueryParams(params: ChoreQueryParams) {
        _queryParams.value = params
        applyFilters()
    }
    
    private fun applyFilters() {
        val params = _queryParams.value
        val query = _uiState.value.searchText
        
        var filtered = allChores
        
        // 1. Text Search
        if (query.isNotBlank()) {
            filtered = filtered.filter { assignment ->
                val chore = assignment.chore ?: return@filter false
                chore.title.contains(query, ignoreCase = true) || 
                chore.description.contains(query, ignoreCase = true)
            }
        }
        
        // 2. Statuses
        if (params.statuses.isNotEmpty()) {
            filtered = filtered.filter { assignment ->
                 try {
                    val status = ChoreStatus.valueOf(assignment.status)
                    params.statuses.contains(status)
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        // 3. Assigned to me
        if (params.assignedToMeOnly) {
            filtered = filtered.filter { it.assignedToId == "user-1" } // Assuming current user is "user-1"
        }
        
        // 4. Date Range
        if (params.dueDateStart != null) {
            val startInstant = Instant.fromEpochMilliseconds(params.dueDateStart)
            filtered = filtered.filter { it.dueDate >= startInstant }
        }
        if (params.dueDateEnd != null) {
            val endInstant = Instant.fromEpochMilliseconds(params.dueDateEnd)
            filtered = filtered.filter { it.dueDate <= endInstant }
        }
        
        // 5. Sorting
        filtered = when (params.sortBy) {
            SortField.CHORE_NAME -> {
                if (params.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.chore?.title }
                } else {
                    filtered.sortedByDescending { it.chore?.title }
                }
            }
            SortField.TIME_UNTIL_DUE -> {
                if (params.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.dueDate }
                } else {
                    filtered.sortedByDescending { it.dueDate }
                }
            }
            null -> filtered
        }
        
        _uiState.update { it.copy(chores = filtered) }
    }
}
