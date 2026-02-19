package com.example.roomiesync.chore

import com.example.roomiesync.ui.components.ChoreStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Chore @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    @SerialName("house_id") val houseId: String,
    val title: String,
    val description: String,
    @SerialName("recurrence_type") val recurrenceType: String,
    @SerialName("created_at") val createdAt: Instant
)

@Serializable
data class ChoreAssignment @OptIn(ExperimentalTime::class) constructor(
    val id: String,
    @SerialName("chore_id") val choreId: String,
    @SerialName("assigned_to_id") val assignedToId: String,
    val status: String, // "NOT_URGENT", "URGENT", "OVERDUE", "COMPLETED", etc.
    @SerialName("proof_photo_url") val proofPhotoUrl: String? = null,
    @SerialName("verified_by") val verifiedBy: String? = null,
    @SerialName("due_date") val dueDate: Instant,
    @SerialName("completed_at") val completedAt: Instant? = null,
    val chore: Chore? = null
)

data class ChoreState(
    val chores: List<ChoreAssignment> = emptyList(),
    val searchText: String = "",
    val isSearchFocused: Boolean = false,
    val isLoading: Boolean = false
)
