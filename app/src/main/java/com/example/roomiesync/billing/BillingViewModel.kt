package com.example.roomiesync.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.BuildConfig
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.ExpenseSplitDetail
import com.example.roomiesync.data.ExpenseWithDetails
import com.example.roomiesync.data.ExpenseRepository
import com.example.roomiesync.data.HouseRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingState(
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val overallBalance: Double = 0.0,
    val expenses: List<ExpenseWithDetails> = emptyList()
)

class BillingViewModel(
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository()),
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BillingState())
    val uiState: StateFlow<BillingState> = _uiState.asStateFlow()

    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
            val house = houseRepository.getUserHouse()
            if (house == null) {
                _uiState.update {
                    it.copy(isLoading = false, currentUserId = userId, expenses = emptyList(), overallBalance = 0.0)
                }
                return@launch
            }
            val houseId = house.id
            try {
                val expenses = expenseRepository.getExpensesWithDetails(houseId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUserId = userId,
                        expenses = expenses,
                        overallBalance = calculateBalance(userId, expenses)
                    )
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                _uiState.update {
                    it.copy(isLoading = false, currentUserId = userId, expenses = emptyList(), overallBalance = 0.0)
                }
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

            currentExpenses[expenseIndex] = expenseDetails.copy(splits = updatedSplits)

            _uiState.update {
                it.copy(
                    expenses = currentExpenses,
                    overallBalance = calculateBalance(it.currentUserId, currentExpenses)
                )
            }

            // Persist: any house member can mark a split paid (RLS in schema.sql).
            // To restrict to only the debtor, change the "Expense splits: update if member" policy
            // to use (user_id = auth.uid()) so only the user who owes can mark their own split.
            viewModelScope.launch {
                expenseRepository.markSplitPaid(splitId)
                // Optionally reload to sync if another client changed data: loadExpenses()
            }
        }
    }

    private fun calculateBalance(userId: String, expenses: List<ExpenseWithDetails>): Double {
        var balance = 0.0
        for (detail in expenses) {
            val iPaid = detail.expense.paidById == userId

            if (iPaid) {
                val othersOweMe = detail.splits
                    .filter { it.profile.id != userId && !it.split.isPaid }
                    .sumOf { it.split.amountOwed }
                balance += othersOweMe
            } else {
                val iOwe = detail.splits
                    .firstOrNull { it.profile.id == userId && !it.split.isPaid }
                    ?.split?.amountOwed ?: 0.0
                balance -= iOwe
            }
        }
        return balance
    }
}
