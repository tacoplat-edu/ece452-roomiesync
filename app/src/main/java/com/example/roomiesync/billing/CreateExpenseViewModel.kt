package com.example.roomiesync.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.ExpenseRepository
import com.example.roomiesync.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateExpenseState(
    val title: String = "",
    val amount: String = "",
    val paidBy: Profile? = null,
    val splitBetween: Set<String> = emptySet(),
    val householdMembers: List<Profile> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class CreateExpenseViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val choreRepository: ChoreRepository = ChoreRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateExpenseState())
    val uiState: StateFlow<CreateExpenseState> = _uiState.asStateFlow()

    private var houseId: String? = null

    init {
        loadHouseholdMembers()
    }

    private fun loadHouseholdMembers() {
        viewModelScope.launch {
            val userId = authRepository.currentUser()?.id ?: return@launch
            val house = choreRepository.getUserHouse(userId) ?: return@launch
            houseId = house.id

            val members = choreRepository.getHouseMembers(house.id)
            val me = members.firstOrNull { it.id == userId }

            _uiState.update {
                it.copy(
                    householdMembers = members,
                    paidBy = me,
                    splitBetween = members.map { p -> p.id }.toSet(),
                    isLoading = false
                )
            }
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }

    fun updateAmount(amount: String) {
        // validation to only allow numbers and decimals
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amount = amount) }
        }
    }

    fun toggleSplitMember(userId: String) {
        _uiState.update { state ->
            val newSet = if (state.splitBetween.contains(userId)) {
                state.splitBetween - userId
            } else {
                state.splitBetween + userId
            }
            state.copy(splitBetween = newSet)
        }
    }

    fun saveExpense(onSuccess: () -> Unit) {
        val state = _uiState.value
        val totalAmount = state.amount.toDoubleOrNull()

        if (state.title.isBlank() || totalAmount == null || totalAmount <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid title and amount.") }
            return
        }
        if (state.splitBetween.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Must split with at least one person.") }
            return
        }
        val currentHouseId = houseId
        val payerId = state.paidBy?.id
        if (currentHouseId == null || payerId == null) {
            _uiState.update { it.copy(errorMessage = "Could not determine household. Try again.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val splitAmount = totalAmount / state.splitBetween.size
            val splits = state.splitBetween.associateWith { splitAmount }

            expenseRepository.createExpense(
                houseId = currentHouseId,
                paidById = payerId,
                amount = totalAmount,
                description = state.title,
                splits = splits
            ).onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = err.message ?: "Failed to save expense."
                    )
                }
            }
        }
    }
}