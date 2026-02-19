@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.chore

import androidx.lifecycle.ViewModel
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
    
    // Store the complete list of chores locally for filtering
    private var allChores: List<ChoreAssignment> = emptyList()

    init {
        // We'll use Clock.System.now() and assume it returns the compatible Instant type
        // or we use java.time.Instant.now() if needed, but let's try the kotlin way first
        // given the user's context about kotlin.time.Instant.
        // If Clock.System.now() returns kotlinx.datetime.Instant and it's not assignment compatible,
        // we might need conversion. But based on the deprecation warning, they might be the same underlying type now.
        val now = Clock.System.now()
        
        // Helper to create dummy chores
        fun createChoreAssignment(
            id: String,
            title: String,
            description: String,
            status: String,
            dueDate: Instant
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
                assignedToId = "user-1",
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
            createChoreAssignment("7", "Clean the master bathroom", "Replenish toilet paper", "NOT_URGENT", now.plus(3.days))
        )
        _uiState.update { it.copy(chores = allChores) }
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
        _uiState.update { 
            it.copy(
                searchText = text,
                chores = filterChores(text)
            ) 
        }
    }
    
    private fun filterChores(query: String): List<ChoreAssignment> {
        if (query.isBlank()) return allChores
        return allChores.filter { assignment ->
            val chore = assignment.chore ?: return@filter false
            chore.title.contains(query, ignoreCase = true) || 
            chore.description.contains(query, ignoreCase = true)
        }
    }

    fun onSearchFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isSearchFocused = focused) }
    }
}
