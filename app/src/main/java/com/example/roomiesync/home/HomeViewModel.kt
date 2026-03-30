@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.BuildConfig
import com.example.roomiesync.chore.Chore
import com.example.roomiesync.chore.ChoreAssignment
import com.example.roomiesync.data.ChatRepository
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.ExpenseRepository
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
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
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

                // Fetch all assignments for the house, including assignee profiles
                val assignmentRows = choreRepository.getChoreAssignments(house.id)

                // Fetch display names for all unique assignees so we can show real names in the feed
                val assigneeIds = assignmentRows.map { it.assignedToId }.distinct()
                val profilesByUserId = assigneeIds
                    .mapNotNull { id -> profileRepository.getProfile(id)?.let { id to it } }
                    .toMap()

                val now = kotlin.time.Clock.System.now()
                val nowMillis = System.currentTimeMillis()

                val choreAssignments = assignmentRows.mapNotNull { row ->
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
                }

                // Chores assigned to the current user, sorted by urgency with pending approval last
                val statusPriority = mapOf("OVERDUE" to 0, "URGENT" to 1, "NOT_URGENT" to 2, "PENDING_APPROVAL" to 3)
                val todoChores = choreAssignments
                    .filter { it.assignedToId == userId && it.status != "COMPLETED" }
                    .sortedWith(compareBy({ statusPriority[it.status] ?: 2 }, { it.dueDate }))

                // Chores completed by someone else that need the current user's approval
                val pendingApprovalList = choreAssignments.filter {
                    it.status == "PENDING_APPROVAL" && it.assignedToId != userId
                }

                // chores
                val choreActivities = choreAssignments
                    .filter { it.status == "COMPLETED" }
                    .map { assignment ->
                        val isCurrentUser = assignment.assignedToId == userId
                        val assigneeName = if (isCurrentUser) "You" else
                            profilesByUserId[assignment.assignedToId]?.displayName ?: "A housemate"
                        ActivityFeedItem(
                            id = assignment.id,
                            title = assignment.chore?.title ?: "Chore completed",
                            description = "$assigneeName completed this chore",
                            timestampMillis = assignment.completedAt?.toEpochMilliseconds() ?: nowMillis,
                            iconType = ActivityIconType.CHORE_COMPLETED
                        )
                    }

                // expenses
                val expenseActivities = try {
                    val expenses = expenseRepository.getExpensesForHouse(house.id)
                    expenses.map { expense ->
                        val isCurrentUser = expense.paidById == userId
                        val payerName = if (isCurrentUser) "You" else
                            profilesByUserId[expense.paidById]?.displayName ?: "Someone"

                        val timestamp = try {
                            Instant.parse(expense.createdAt ?: "").toEpochMilliseconds()
                        } catch (e: Exception) { nowMillis }

                        ActivityFeedItem(
                            id = expense.id,
                            title = "New Expense",
                            description = "$payerName added '${expense.description}'",
                            timestampMillis = timestamp,
                            iconType = ActivityIconType.BILL_PAID
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }

                // messages
                val chatActivities = try {
                    val messagesWithProfile = chatRepository.getMessagesForHouse(house.id)
                    messagesWithProfile.map { msgWithProfile ->
                        val msg = msgWithProfile.message
                        val isCurrentUser = msg.senderId == userId
                        val senderName = if (isCurrentUser) "You" else
                            msgWithProfile.profile.displayName ?: "Someone"

                        val timestamp = try {
                            val createdAtStr = msg.createdAt.toString()
                            Instant.parse(createdAtStr).toEpochMilliseconds()
                        } catch (e: Exception) {
                            (msg.createdAt as? Number)?.toLong() ?: nowMillis
                        }

                        ActivityFeedItem(
                            id = msg.id,
                            title = "New Message",
                            description = "$senderName: ${msg.content}",
                            timestampMillis = timestamp,
                            iconType = ActivityIconType.CHAT
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }

                val recentActivity = (choreActivities + expenseActivities + chatActivities)
                    .sortedByDescending { it.timestampMillis }
                    .take(20)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = profile,
                        house = house,
                        chores = todoChores,
                        pendingApprovalChores = pendingApprovalList,
                        assigneeProfiles = profilesByUserId,
                        recentActivity = recentActivity,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load data") }
            }
        }
    }
}
