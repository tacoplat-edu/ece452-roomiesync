package com.example.roomiesync.home

import com.example.roomiesync.chore.ChoreAssignment
import com.example.roomiesync.data.House
import com.example.roomiesync.data.Profile

data class ActivityFeedItem(
    val id: String,
    val title: String,
    val description: String,
    val timestampMillis: Long,
    val iconType: ActivityIconType
)

enum class ActivityIconType {
    CHORE_COMPLETED,
    CHORE_ADDED,
    BILL_PAID,
    CHAT
}

data class HomeState(
    val isLoading: Boolean = true,
    val profile: Profile? = null,
    val house: House? = null,
    val chores: List<ChoreAssignment> = emptyList(),
    val recentActivity: List<ActivityFeedItem> = emptyList(),
    val errorMessage: String? = null
)