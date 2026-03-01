package com.example.roomiesync.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.data.Expense
import com.example.roomiesync.data.ExpenseSplit
import com.example.roomiesync.data.ExpenseSplitDetail
import com.example.roomiesync.data.ExpenseWithDetails
import com.example.roomiesync.data.Profile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingState(
    val isLoading: Boolean = true,
    val currentUserId: String = "user-1",
    val overallBalance: Double = 0.0,
    val expenses: List<ExpenseWithDetails> = emptyList()
)

class BillingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BillingState())
    val uiState: StateFlow<BillingState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(600)

            val me = Profile("user-1", "alice@example.com", "Alice")
            val bob = Profile("user-2", "bob@example.com", "Bob")
            val charlie = Profile("user-3", "charlie@example.com", "Charlie")

            val now = System.currentTimeMillis()

            val exp1 = Expense("e1", "house-1", "Groceries", 90.0, "user-1", now - 86400000)
            val splits1 = listOf(
                ExpenseSplitDetail(ExpenseSplit("s1", "e1", "user-1", 30.0, true), me),
                ExpenseSplitDetail(ExpenseSplit("s2", "e1", "user-2", 30.0, false), bob),
                ExpenseSplitDetail(ExpenseSplit("s3", "e1", "user-3", 30.0, false), charlie)
            )

            val exp2 = Expense("e2", "house-1", "Internet", 60.0, "user-2", now - 172800000)
            val splits2 = listOf(
                ExpenseSplitDetail(ExpenseSplit("s4", "e2", "user-1", 20.0, false), me),
                ExpenseSplitDetail(ExpenseSplit("s5", "e2", "user-2", 20.0, true), bob),
                ExpenseSplitDetail(ExpenseSplit("s6", "e2", "user-3", 20.0, true), charlie)
            )

            val mockExpenses = listOf(
                ExpenseWithDetails(exp1, me, splits1),
                ExpenseWithDetails(exp2, bob, splits2)
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    expenses = mockExpenses,
                    overallBalance = calculateBalance("user-1", mockExpenses)
                )
            }
        }
    }

    fun markSplitAsPaid(expenseId: String, splitId: String) {
        val currentExpenses = _uiState.value.expenses.toMutableList()
        val expenseIndex = currentExpenses.indexOfFirst { it.expense.id == expenseId }

        if (expenseIndex != -1) {
            val expenseDetails = currentExpenses[expenseIndex]

            val updatedSplits = expenseDetails.splits.map { detail ->
                if (detail.split.id == splitId) {
                    detail.copy(split = detail.split.copy(isPaid = true))
                } else {
                    detail
                }
            }

            // update expense
            currentExpenses[expenseIndex] = expenseDetails.copy(splits = updatedSplits)

            _uiState.update {
                it.copy(
                    expenses = currentExpenses,
                    overallBalance = calculateBalance(it.currentUserId, currentExpenses)
                )
            }

            // TODO: make Supabase UPDATE query
        }
    }

    private fun calculateBalance(userId: String, expenses: List<ExpenseWithDetails>): Double {
        var balance = 0.0
        for (detail in expenses) {
            val iPaid = detail.expense.paidById == userId

            if (iPaid) {
                val othersOweMe = detail.splits
                    .filter { it.profile.id != userId && !it.split.isPaid }
                    .sumOf { it.split.amountOowed }
                balance += othersOweMe
            } else {
                val iOwe = detail.splits
                    .firstOrNull { it.profile.id == userId && !it.split.isPaid }
                    ?.split?.amountOowed ?: 0.0
                balance -= iOwe
            }
        }
        return balance
    }
}