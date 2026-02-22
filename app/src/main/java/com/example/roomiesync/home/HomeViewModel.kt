@file:OptIn(ExperimentalTime::class)

package com.example.roomiesync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.chore.Chore
import com.example.roomiesync.chore.ChoreAssignment
import com.example.roomiesync.data.House
import com.example.roomiesync.data.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Clock

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Use dummy data for now

            // Add a delay
            delay(800)

            val now = Clock.System.now()
            val nowMillis = System.currentTimeMillis()

            val profile = Profile(
                id = "user-1",
                email = "tom@example.com",
                displayName = "Tom",
            )

            val house = House(
                id = "house-1",
                name = "Tom's house",
                address = "123 University Ave, Waterloo",
                createdBy = "user-1"
            )

            // Dummy chores
            val chore1 = ChoreAssignment(
                id = "c1",
                choreId = "chore-1",
                assignedToId = "user-1",
                status = "OVERDUE",
                dueDate = now.minus(1.days),
                chore = Chore(id = "chore-1", houseId = "house-1", title = "Take out the trash", description = "", recurrenceType = "weekly", createdAt = now)
            )
            val chore2 = ChoreAssignment(
                id = "c2",
                choreId = "chore-2",
                assignedToId = "user-1",
                status = "URGENT",
                dueDate = now.plus(3.hours),
                chore = Chore(id = "chore-2", houseId = "house-1", title = "Clean kitchen counters", description = "", recurrenceType = "daily", createdAt = now)
            )
            val chore3 = ChoreAssignment(
                id = "c3",
                choreId = "chore-3",
                assignedToId = "user-2",
                status = "URGENT",
                dueDate = now.plus(5.hours),
                chore = Chore(id = "chore-3", houseId = "house-1", title = "Mop the kitchen", description = "", recurrenceType = "daily", createdAt = now)
            )

            // Dummy activity data
            val activities = listOf(
                ActivityFeedItem(
                    id = "a1",
                    title = "Bob completed a chore",
                    description = "Vacuum the living room",
                    timestampMillis = nowMillis - (1000 * 60 * 30), // 30 mins ago
                    iconType = ActivityIconType.CHORE_COMPLETED
                ),
                ActivityFeedItem(
                    id = "a2",
                    title = "Charlie added a new bill",
                    description = "Hydro ($45.00 each)",
                    timestampMillis = nowMillis - (1000 * 60 * 60 * 5), // 5 hours ago
                    iconType = ActivityIconType.BILL_PAID
                ),
                ActivityFeedItem(
                    id = "a3",
                    title = "Peter sent a message",
                    description = "Won't be home tomorrow",
                    timestampMillis = nowMillis - (1000 * 60 * 60 * 24 * 2), // 2 days ago
                    iconType = ActivityIconType.CHAT
                ),
                ActivityFeedItem(
                    id = "a4",
                    title = "Peter added a chore",
                    description = "Take out trash",
                    timestampMillis = nowMillis - (1000 * 60 * 60 * 24 * 2), // 2 days ago
                    iconType = ActivityIconType.CHORE_ADDED
                ),
                ActivityFeedItem(
                    id = "a5",
                    title = "Jack sent a message",
                    description = "Going to class.",
                    timestampMillis = nowMillis - (1000 * 60 * 60 * 24 * 3), // 3 days ago
                    iconType = ActivityIconType.CHAT
                )
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    profile = profile,
                    house = house,
                    chores = listOf(chore1, chore2, chore3),
                    recentActivity = activities
                )
            }
        }
    }
}