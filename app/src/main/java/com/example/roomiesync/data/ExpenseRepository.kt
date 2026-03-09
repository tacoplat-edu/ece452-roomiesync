package com.example.roomiesync.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExpenseRepository {

    private val client get() = SupabaseClient.client

    // ── READ ────────────────────────────────────────────────────────────

    /**
     * Fetch all expenses for a house together with their splits.
     * Profiles are loaded separately so we can attach display names.
     */
    suspend fun getExpensesWithDetails(
        houseId: String,
        members: List<Profile>
    ): List<ExpenseWithDetails> = withContext(Dispatchers.IO) {
        try {
            val profileMap = members.associateBy { it.id }

            // Fetch expenses with nested splits
            val rows = client.postgrest.from("expenses")
                .select(columns = Columns.raw("*, expense_splits(*)")) {
                    filter { eq("house_id", houseId) }
                    // newest first
                }
                .decodeList<ExpenseWithSplitsRow>()

            rows.map { row ->
                val payer = profileMap[row.paidById]
                    ?: Profile(id = row.paidById, displayName = "Unknown")

                val splitDetails = (row.expenseSplits ?: emptyList()).map { split ->
                    val profile = profileMap[split.userId]
                        ?: Profile(id = split.userId, displayName = "Unknown")
                    ExpenseSplitDetail(split = split, profile = profile)
                }

                ExpenseWithDetails(
                    expense = Expense(
                        id = row.id,
                        houseId = row.houseId,
                        description = row.description,
                        amount = row.amount,
                        paidById = row.paidById,
                        createdAt = row.createdAt
                    ),
                    paidByProfile = payer,
                    splits = splitDetails
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ── CREATE ──────────────────────────────────────────────────────────

    /**
     * Insert an expense and its splits in sequence.
     * @param splits map of userId → amountOwed
     */
    suspend fun createExpense(
        houseId: String,
        paidById: String,
        amount: Double,
        description: String?,
        splits: Map<String, Double>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Insert the expense row and get the generated id back
            val expenseRow = client.postgrest.from("expenses")
                .insert(
                    CreateExpensePayload(
                        houseId = houseId,
                        paidById = paidById,
                        amount = amount,
                        description = description
                    )
                ) {
                    select()
                }
                .decodeSingle<Expense>()

            // 2. Insert all splits
            val splitPayloads = splits.map { (userId, owed) ->
                CreateExpenseSplitPayload(
                    expenseId = expenseRow.id,
                    userId = userId,
                    amountOwed = owed
                )
            }
            client.postgrest.from("expense_splits")
                .insert(splitPayloads)

            Unit
        }
    }

    // ── DELETE ──────────────────────────────────────────────────────────

    suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("expenses")
                .delete { filter { eq("id", expenseId) } }
            Unit
        }
    }

    // ── UPDATE (mark split as paid) ────────────────────────────────────

    suspend fun markSplitAsPaid(splitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.postgrest.from("expense_splits")
                .update({ set("is_paid", true) }) {
                    filter { eq("id", splitId) }
                }
            Unit
        }
    }

    // ── BALANCE CALCULATION ────────────────────────────────────────────

    /**
     * Compute net balances for each pair of users in a household.
     * Returns a list of [Balance] objects describing "from owes to X amount".
     */
    fun calculateBalances(expenses: List<ExpenseWithDetails>): List<Balance> {
        // net[A][B] = positive means A owes B that amount
        val net = mutableMapOf<String, MutableMap<String, Double>>()

        for (detail in expenses) {
            val payerId = detail.expense.paidById
            for (split in detail.splits) {
                if (split.split.isPaid) continue          // already settled
                if (split.profile.id == payerId) continue // payer's own share

                val debtor = split.profile.id
                val amount = split.split.amountOwed

                // debtor owes payer
                net.getOrPut(debtor) { mutableMapOf() }
                    .merge(payerId, amount) { a, b -> a + b }
                // reverse direction (payer is owed by debtor)
                net.getOrPut(payerId) { mutableMapOf() }
                    .merge(debtor, -amount) { a, b -> a + b }
            }
        }

        // Collapse to positive-only (from owes to)
        val balances = mutableListOf<Balance>()
        val visited = mutableSetOf<Pair<String, String>>()

        for ((a, debts) in net) {
            for ((b, amount) in debts) {
                val pair = if (a < b) a to b else b to a
                if (pair in visited) continue
                visited.add(pair)

                if (amount > 0.01) {
                    balances.add(Balance(fromUserId = a, toUserId = b, amount = amount))
                } else if (amount < -0.01) {
                    balances.add(Balance(fromUserId = b, toUserId = a, amount = -amount))
                }
            }
        }
        return balances
    }

    /**
     * Compute the overall balance for a single user.
     * Positive = others owe you, negative = you owe others.
     */
    fun calculateUserBalance(userId: String, expenses: List<ExpenseWithDetails>): Double {
        var balance = 0.0
        for (detail in expenses) {
            val iPaid = detail.expense.paidById == userId
            if (iPaid) {
                balance += detail.splits
                    .filter { it.profile.id != userId && !it.split.isPaid }
                    .sumOf { it.split.amountOwed }
            } else {
                balance -= detail.splits
                    .firstOrNull { it.profile.id == userId && !it.split.isPaid }
                    ?.split?.amountOwed ?: 0.0
            }
        }
        return balance
    }
}

// ── Helper row types for Supabase deserialization ────────────────────

@kotlinx.serialization.Serializable
data class ExpenseWithSplitsRow(
    val id: String,
    @kotlinx.serialization.SerialName("house_id") val houseId: String,
    @kotlinx.serialization.SerialName("paid_by_id") val paidById: String,
    val amount: Double,
    val description: String? = null,
    @kotlinx.serialization.SerialName("created_at") val createdAt: String? = null,
    @kotlinx.serialization.SerialName("expense_splits") val expenseSplits: List<ExpenseSplit>? = null
)

data class Balance(
    val fromUserId: String,
    val toUserId: String,
    val amount: Double
)
