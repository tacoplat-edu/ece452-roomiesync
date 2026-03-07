@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.chore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.SupabaseClient
import com.example.roomiesync.ui.components.ChoreStatus
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Clock

class ChoreViewModel(
    private val choreRepository: ChoreRepository = ChoreRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChoreState())
    val uiState: StateFlow<ChoreState> = _uiState.asStateFlow()

    private val _queryParams = MutableStateFlow(ChoreQueryParams())
    val queryParams: StateFlow<ChoreQueryParams> = _queryParams.asStateFlow()

    private var allChores: List<ChoreAssignment> = emptyList()
    private var currentUserId: String? = null

    init {
        loadChores()
    }

    fun refresh() {
        loadChores()
    }

    private fun loadChores() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                currentUserId = userId
                if (userId == null) return@launch

                val house = choreRepository.getUserHouse(userId)
                if (house == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val rows = choreRepository.getChoreAssignments(house.id)
                val now = Clock.System.now()

                allChores = rows.mapNotNull { row ->
                    val choreData = row.chores ?: return@mapNotNull null
                    val dueInstant = try {
                        Instant.parse(row.dueDate ?: return@mapNotNull null)
                    } catch (_: Exception) { return@mapNotNull null }

                    val uiStatus = when (row.status) {
                        "completed" -> "COMPLETED"
                        "pending_approval" -> "PENDING_APPROVAL"
                        else -> when {
                            dueInstant < now -> "OVERDUE"
                            dueInstant - now < 24.hours -> "URGENT"
                            else -> "NOT_URGENT"
                        }
                    }

                    ChoreAssignment(
                        id = row.id,
                        choreId = row.choreId,
                        assignedToId = row.assignedToId,
                        status = uiStatus,
                        proofPhotoUrl = row.proofPhotoUrl,
                        verifiedBy = row.verifiedBy,
                        dueDate = dueInstant,
                        completedAt = row.completedAt?.let { try { Instant.parse(it) } catch (_: Exception) { null } },
                        chore = Chore(
                            id = choreData.id,
                            houseId = choreData.houseId,
                            title = choreData.title,
                            description = choreData.description ?: "",
                            recurrenceType = choreData.recurrenceType ?: "none",
                            createdAt = try { Instant.parse(choreData.createdAt ?: "") } catch (_: Exception) { now }
                        )
                    )
                }.filter { it.status != "COMPLETED" }

                applyFilters()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun submitChore(choreId: String, photoUrl: String) {
        viewModelScope.launch {
            choreRepository.submitChoreForReview(choreId, photoUrl)
                .onSuccess { loadChores() }
                .onFailure { e -> e.printStackTrace() }
        }
    }

    fun approveChore(assignmentId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            choreRepository.approveChore(assignmentId, userId)
                .onSuccess { loadChores() }
                .onFailure { e -> e.printStackTrace() }
        }
    }

    fun rejectChore(assignmentId: String) {
        viewModelScope.launch {
            choreRepository.rejectChore(assignmentId)
                .onSuccess { loadChores() }
                .onFailure { e -> e.printStackTrace() }
        }
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

        if (query.isNotBlank()) {
            filtered = filtered.filter { assignment ->
                val chore = assignment.chore ?: return@filter false
                chore.title.contains(query, ignoreCase = true) ||
                    chore.description.contains(query, ignoreCase = true)
            }
        }

        if (params.statuses.isNotEmpty()) {
            filtered = filtered.filter { assignment ->
                try {
                    val status = ChoreStatus.valueOf(assignment.status)
                    params.statuses.contains(status)
                } catch (_: Exception) { false }
            }
        }

        if (params.assignedToMeOnly) {
            filtered = filtered.filter { it.assignedToId == currentUserId }
        }

        if (params.dueDateStart != null) {
            val startInstant = Instant.fromEpochMilliseconds(params.dueDateStart)
            filtered = filtered.filter { it.dueDate >= startInstant }
        }
        if (params.dueDateEnd != null) {
            val endInstant = Instant.fromEpochMilliseconds(params.dueDateEnd)
            filtered = filtered.filter { it.dueDate <= endInstant }
        }

        filtered = when (params.sortBy) {
            SortField.CHORE_NAME -> if (params.sortDirection == SortDirection.ASCENDING)
                filtered.sortedBy { it.chore?.title } else filtered.sortedByDescending { it.chore?.title }
            SortField.TIME_UNTIL_DUE -> if (params.sortDirection == SortDirection.ASCENDING)
                filtered.sortedBy { it.dueDate } else filtered.sortedByDescending { it.dueDate }
            null -> filtered
        }

        _uiState.update { it.copy(chores = filtered) }
    }
}