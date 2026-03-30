package com.example.roomiesync.data

import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    @OptIn(SupabaseExperimental::class)
    fun observeChoreAssignments(houseId: String): Flow<List<ChoreAssignmentRow>> {
        return channelFlow {
            var lastEmitted: List<ChoreAssignmentRow>? = null

            suspend fun emitIfChanged(rows: List<ChoreAssignmentRow>) {
                val normalizedRows = rows.sortedWith(compareBy({ it.dueDate ?: "" }, { it.id }))
                if (normalizedRows != lastEmitted) {
                    lastEmitted = normalizedRows
                    trySend(normalizedRows)
                }
            }

            launch {
                client.postgrest.from("chore_assignments")
                    .selectAsFlow<ChoreAssignmentRow, String>(primaryKey = ChoreAssignmentRow::id)
                    .map { getChoreAssignments(houseId) }
                    .collect { rows ->
                        emitIfChanged(rows)
                    }
            }

            launch {
                client.postgrest.from("chores")
                    .selectAsFlow<ChoreRow, String>(
                        primaryKey = ChoreRow::id,
                        filter = FilterOperation("house_id", FilterOperator.EQ, houseId)
                    )
                    .map { getChoreAssignments(houseId) }
                    .collect { rows ->
                        emitIfChanged(rows)
                    }
            }

            launch {
                while (isActive) {
                    emitIfChanged(getChoreAssignments(houseId))
                    delay(CHORE_POLL_INTERVAL_MS)
                }
            }
        }.flowOn(Dispatchers.IO)
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

            val assignments = generateAssignmentDates(recurrenceType, dueDateIso).map { date ->
                CreateChoreAssignmentPayload(
                    choreId = choreRow.id,
                    assignedToId = assignedToId,
                    dueDate = date
                )
            }

            client.postgrest.from("chore_assignments").insert(assignments)
            Unit
        }
    }

    private fun generateAssignmentDates(recurrenceType: String, firstDueDateIso: String): List<String> {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        if (recurrenceType == "none") return listOf(firstDueDateIso)

        val parts = recurrenceType.split("_")
        if (parts.size != 2) return listOf(firstDueDateIso)

        val interval = parts[0].toIntOrNull() ?: return listOf(firstDueDateIso)
        val calendarUnit = when (parts[1]) {
            "days" -> Calendar.DAY_OF_MONTH
            "weeks" -> Calendar.WEEK_OF_YEAR
            "months" -> Calendar.MONTH
            else -> return listOf(firstDueDateIso)
        }

        val utc = TimeZone.getTimeZone("UTC")
        val current = Calendar.getInstance(utc).apply {
            time = isoFormat.parse(firstDueDateIso) ?: return listOf(firstDueDateIso)
        }
        val end = Calendar.getInstance(utc).apply {
            time = current.time
            add(Calendar.YEAR, 1)
        }

        val dates = mutableListOf<String>()
        while (!current.after(end)) {
            dates.add(isoFormat.format(current.time))
            current.add(calendarUnit, interval)
        }
        return dates
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

    private companion object {
        const val CHORE_POLL_INTERVAL_MS = 2_500L
    }
}
