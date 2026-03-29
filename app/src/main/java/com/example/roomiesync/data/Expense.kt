package com.example.roomiesync.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    @SerialName("house_id") val houseId: String,
    @SerialName("description") val title: String,
    val amount: Double,
    @SerialName("paid_by_id") val paidById: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ExpenseSplit(
    val id: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double,
    @SerialName("is_paid") val isPaid: Boolean
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