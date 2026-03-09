package com.example.roomiesync.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.Balance
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.ExpenseRepository
import com.example.roomiesync.data.ExpenseWithDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BillingState(
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val overallBalance: Double = 0.0,
    val expenses: List<ExpenseWithDetails> = emptyList(),
    val balances: List<Balance> = emptyList()
)

class BillingViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val choreRepository: ChoreRepository = ChoreRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BillingState())
    val uiState: StateFlow<BillingState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = authRepository.currentUser()?.id ?: return@launch
            _uiState.update { it.copy(currentUserId = userId) }

            val house = choreRepository.getUserHouse(userId) ?: return@launch
            val members = choreRepository.getHouseMembers(house.id)
            val expenses = expenseRepository.getExpensesWithDetails(house.id, members)
            val balance = expenseRepository.calculateUserBalance(userId, expenses)
            val balances = expenseRepository.calculateBalances(expenses)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    expenses = expenses,
                    overallBalance = balance,
                    balances = balances
                )
            }
        }
    }

    fun markSplitAsPaid(expenseId: String, splitId: String) {
        viewModelScope.launch {
            expenseRepository.markSplitAsPaid(splitId).onSuccess {
                // Optimistically update the UI
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
                            overallBalance = expenseRepository.calculateUserBalance(
                                it.currentUserId, currentExpenses
                            ),
                            balances = expenseRepository.calculateBalances(currentExpenses)
                        )
                    }
                }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId).onSuccess {
                val updated = _uiState.value.expenses.filter { it.expense.id != expenseId }
                _uiState.update {
                    it.copy(
                        expenses = updated,
                        overallBalance = expenseRepository.calculateUserBalance(
                            it.currentUserId, updated
                        ),
                        balances = expenseRepository.calculateBalances(updated)
                    )
                }
            }
        }
    }
}