package com.example.roomiesync.data

import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseRow(
    val id: String,
    @SerialName("house_id") val houseId: String,
    @SerialName("description") val description: String,
    val amount: Double,
    @SerialName("paid_by_id") val paidById: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ExpenseSplitRow(
    val id: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double,
    @SerialName("is_paid") val isPaid: Boolean? = null  // DB may have NULL; treat as unpaid
)

@Serializable
data class CreateExpensePayload(
    @SerialName("house_id") val houseId: String,
    @SerialName("paid_by_id") val paidById: String,
    val amount: Double,
    val description: String
)

@Serializable
data class CreateExpenseSplitPayload(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double,
    @SerialName("is_paid") val isPaid: Boolean = false
)

class ExpenseRepository(
    private val profileRepository: ProfileRepository = ProfileRepository()
) {

    private val client get() = SupabaseClient.client

    suspend fun createExpenseWithSplits(
        houseId: String,
        paidById: String,
        amount: Double,
        description: String,
        splitUserIds: List<String>,
        amountPerUser: Double
    ): Result<ExpenseRow> = withContext(Dispatchers.IO) {
        runCatching {
            val expense = client.postgrest.from("expenses")
                .insert(CreateExpensePayload(houseId, paidById, amount, description)) {
                    select()
                }
                .decodeSingle<ExpenseRow>()

            if (splitUserIds.isNotEmpty()) {
                val splits = splitUserIds.map { userId ->
                    CreateExpenseSplitPayload(expense.id, userId, amountPerUser, userId == paidById)
                }
                client.postgrest.from("expense_splits").insert(splits)
            }
            expense
        }
    }

    suspend fun getExpensesForHouse(houseId: String): List<ExpenseRow> = withContext(Dispatchers.IO) {
        try {
            client.postgrest.from("expenses")
                .select() {
                    filter { eq("house_id", houseId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<ExpenseRow>()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    suspend fun getSplitsForExpenses(expenseIds: List<String>): List<ExpenseSplitRow> = withContext(Dispatchers.IO) {
        if (expenseIds.isEmpty()) return@withContext emptyList()
        try {
            client.postgrest.from("expense_splits")
                .select() {
                    filter { isIn("expense_id", expenseIds) }
                }
                .decodeList<ExpenseSplitRow>()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    /**
     * Mark a single expense split as paid.
     * RLS: any house member can update (see schema.sql). To restrict to only the debtor
     * marking their own split, change the "Expense splits: update if member" policy to
     * use (user_id = auth.uid()).
     */
    suspend fun markSplitPaid(splitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("expense_splits")
                .update({
                    set("is_paid", true)
                }) {
                    filter { eq("id", splitId) }
                }
            Unit
        }
    }

    suspend fun getExpensesWithDetails(houseId: String): List<ExpenseWithDetails> = withContext(Dispatchers.IO) {
        val expenses = getExpensesForHouse(houseId)
        if (expenses.isEmpty()) return@withContext emptyList()
        val expenseIds = expenses.map { it.id }
        val splits = getSplitsForExpenses(expenseIds)
        val userIds = (expenses.map { it.paidById } + splits.map { it.userId }).distinct()
        val profiles = profileRepository.getProfiles(userIds).associateBy { it.id }

        expenses.map { row ->
            val paidByProfile = profiles[row.paidById] ?: Profile(row.paidById, null, null)
            val splitRows = splits.filter { it.expenseId == row.id }
            val splitDetails = splitRows.map { s ->
                val profile = profiles[s.userId] ?: Profile(s.userId, null, null)
                ExpenseSplitDetail(
                    split = ExpenseSplit(s.id, s.expenseId, s.userId, s.amountOwed, s.isPaid ?: false),
                    profile = profile
                )
            }
            ExpenseWithDetails(
                expense = Expense(
                    id = row.id,
                    houseId = row.houseId,
                    title = row.description,
                    amount = row.amount,
                    paidById = row.paidById,
                    createdAt = row.createdAt ?: ""
                ),
                paidByProfile = paidByProfile,
                splits = splitDetails
            )
        }
    }
}
