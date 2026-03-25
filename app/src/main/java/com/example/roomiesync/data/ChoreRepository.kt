package com.example.roomiesync.data

import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateChorePayload(
    @SerialName("house_id") val houseId: String,
    val title: String,
    val description: String,
    @SerialName("recurrence_type") val recurrenceType: String
)

@Serializable
data class CreateChoreAssignmentPayload(
    @SerialName("chore_id") val choreId: String,
    @SerialName("assigned_to_id") val assignedToId: String,
    val status: String = "todo",
    @SerialName("due_date") val dueDate: String
)

@Serializable
data class ChoreRow(
    val id: String,
    @SerialName("house_id") val houseId: String,
    val title: String,
    val description: String? = null,
    @SerialName("recurrence_type") val recurrenceType: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChoreAssignmentRow(
    val id: String,
    @SerialName("chore_id") val choreId: String,
    @SerialName("assigned_to_id") val assignedToId: String,
    val status: String,
    @SerialName("proof_photo_url") val proofPhotoUrl: String? = null,
    @SerialName("verified_by") val verifiedBy: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val chores: ChoreRow? = null
)

@Serializable
data class HouseMemberRow(
    @SerialName("user_id") val userId: String,
    val role: String,
    val profiles: Profile? = null
)

class ChoreRepository {

    private val client get() = SupabaseClient.client

    suspend fun getUserHouse(userId: String): House? = withContext(Dispatchers.IO) {
        try {
            val membership = client.postgrest.from("house_members")
                .select(columns = Columns.raw("house_id, houses(*)")) {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeSingleOrNull<HouseMemberWithHouse>()
            membership?.houses
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            null
        }
    }

    suspend fun getHouseMembers(houseId: String): List<Profile> = withContext(Dispatchers.IO) {
        try {
            val rows = client.postgrest.from("house_members")
                .select(columns = Columns.raw("user_id, role, profiles(*)")) {
                    filter { eq("house_id", houseId) }
                }
                .decodeList<HouseMemberRow>()
            rows.mapNotNull { it.profiles }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    suspend fun getChoreAssignments(houseId: String): List<ChoreAssignmentRow> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("chore_assignments")
                .select(columns = Columns.raw("*, chores!inner(*)")) {
                    filter { eq("chores.house_id", houseId) }
                }
                .decodeList<ChoreAssignmentRow>()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    suspend fun createChoreWithAssignment(
        houseId: String,
        title: String,
        description: String,
        recurrenceType: String,
        assignedToId: String,
        dueDateIso: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val choreRow = client.postgrest.from("chores")
                .insert(CreateChorePayload(houseId, title, description, recurrenceType)) {
                    select()
                }
                .decodeSingle<ChoreRow>()

            client.postgrest.from("chore_assignments")
                .insert(CreateChoreAssignmentPayload(
                    choreId = choreRow.id,
                    assignedToId = assignedToId,
                    dueDate = dueDateIso
                ))
            Unit
        }
    }

    suspend fun submitChoreForReview(
        assignmentId: String,
        proofPhotoUrl: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("chore_assignments")
                .update({
                    set("status", "pending_approval")
                    set("proof_photo_url", proofPhotoUrl)
                }) {
                    filter { eq("id", assignmentId) }
                }
            Unit
        }
    }

    suspend fun approveChore(
        assignmentId: String,
        verifiedByUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("chore_assignments")
                .update({
                    set("status", "completed")
                    set("verified_by", verifiedByUserId)
                    set("completed_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()))
                }) {
                    filter { eq("id", assignmentId) }
                }
            Unit
        }
    }

    suspend fun rejectChore(
        assignmentId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("chore_assignments")
                .update({
                    set("status", "todo")
                    set("proof_photo_url", null as String?)
                }) {
                    filter { eq("id", assignmentId) }
                }
            Unit
        }
    }
}