@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.chore.Chore
import com.example.roomiesync.chore.ChoreAssignment
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.ProfileRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class HomeViewModel(
    private val choreRepository: ChoreRepository = ChoreRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    fun approveChore(assignmentId: String) {
        viewModelScope.launch {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            choreRepository.approveChore(assignmentId, userId)
            refresh()
        }
    }

    fun rejectChore(assignmentId: String) {
        viewModelScope.launch {
            choreRepository.rejectChore(assignmentId)
            refresh()
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Not authenticated") }
                    return@launch
                }

                val profile = profileRepository.getProfile(userId)

                val house = choreRepository.getUserHouse(userId)
                if (house == null) {
                    _uiState.update { it.copy(isLoading = false, profile = profile, errorMessage = "No household found") }
                    return@launch
                }

                val assignmentRows = choreRepository.getChoreAssignments(house.id)

                val choreAssignments = assignmentRows.mapNotNull { row ->
                    val choreData = row.chores ?: return@mapNotNull null
                    val dueInstant = try {
                        Instant.parse(row.dueDate ?: return@mapNotNull null)
                    } catch (_: Exception) { return@mapNotNull null }

                    val now = kotlin.time.Clock.System.now()
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
                }

                val todoChores = choreAssignments.filter {
                    it.assignedToId == userId && it.status != "COMPLETED" && it.status != "PENDING_APPROVAL"
                }

                val nowMillis = System.currentTimeMillis()
                
                val pendingApprovalList = choreAssignments.filter {
                    it.status == "PENDING_APPROVAL" && it.assignedToId != userId
                }
                
                val pendingApprovalActivities = pendingApprovalList.map {
                    ActivityFeedItem(
                        id = it.id,
                        title = "${it.chore?.title ?: "Chore"} needs approval",
                        description = "Please review the completed chore",
                        timestampMillis = it.completedAt?.toEpochMilliseconds() ?: nowMillis,
                        iconType = ActivityIconType.CHORE_PENDING_APPROVAL
                    )
                }

                val mockActivities = listOf(
                    ActivityFeedItem("a1", "Bob completed a chore", "Vacuum the living room", nowMillis - (1000 * 60 * 30), ActivityIconType.CHORE_COMPLETED),
                    ActivityFeedItem("a2", "Charlie added a new bill", "Hydro (\$45.00 each)", nowMillis - (1000 * 60 * 60 * 5), ActivityIconType.BILL_PAID),
                    ActivityFeedItem("a3", "Peter sent a message", "Won't be home tomorrow", nowMillis - (1000 * 60 * 60 * 24 * 2), ActivityIconType.CHAT),
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        house = house,
                        chores = todoChores,
                        pendingApprovalChores = pendingApprovalList,
                        recentActivity = pendingApprovalActivities + mockActivities,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load data") }
            }
        }
    }
}