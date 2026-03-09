package com.example.roomiesync.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String = "",
    @SerialName("house_id") val houseId: String = "",
    val description: String? = null,
    val amount: Double = 0.0,
    @SerialName("paid_by_id") val paidById: String = "",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ExpenseSplit(
    val id: String = "",
    @SerialName("expense_id") val expenseId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("amount_owed") val amountOwed: Double = 0.0,
    @SerialName("is_paid") val isPaid: Boolean = false
)

// bundle the expense with its splits and profiles for the UI
data class ExpenseWithDetails(
    val expense: Expense,
    val paidByProfile: Profile,
    val splits: List<ExpenseSplitDetail>
)

data class ExpenseSplitDetail(
    val split: ExpenseSplit,
    val profile: Profile
)

// ── Payloads for inserting into Supabase ──

@Serializable
data class CreateExpensePayload(
    @SerialName("house_id") val houseId: String,
    @SerialName("paid_by_id") val paidById: String,
    val amount: Double,
    val description: String?
)

@Serializable
data class CreateExpenseSplitPayload(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double
)