package com.example.roomiesync.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.ExpenseRepository
import com.example.roomiesync.data.HouseRepository
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
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
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CreateExpenseViewModel(
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository()),
    private val choreRepository: ChoreRepository = ChoreRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateExpenseState())
    val uiState: StateFlow<CreateExpenseState> = _uiState.asStateFlow()

    init {
        loadHouseAndMembers()
    }

    private fun loadHouseAndMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val house = houseRepository.getUserHouse()
                if (house == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No household found")
                    }
                    return@launch
                }
                val members = choreRepository.getHouseMembers(house.id)
                val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
                val defaultPaidBy = members.firstOrNull { it.id == currentUserId } ?: members.firstOrNull()
                _uiState.update {
                    it.copy(
                        householdMembers = members,
                        paidBy = defaultPaidBy,
                        splitBetween = members.map { p -> p.id }.toSet(),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message)
                }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }

    fun updateAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(amount = amount) }
        }
    }

    fun setPaidBy(profile: Profile) {
        _uiState.update { it.copy(paidBy = profile) }
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
        if (state.title.isBlank() || state.amount.isBlank() || state.amount.toDoubleOrNull() == null) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid title and amount.") }
            return
        }
        if (state.splitBetween.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Must split with at least one person.") }
            return
        }
        val paidBy = state.paidBy
        if (paidBy == null) {
            _uiState.update { it.copy(errorMessage = "Please select who paid.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val house = houseRepository.getUserHouse()
            if (house == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "No household found")
                }
                return@launch
            }
            val amount = state.amount.toDoubleOrNull() ?: 0.0
            val splitCount = state.splitBetween.size
            val amountPerUser = if (splitCount > 0) amount / splitCount else 0.0

            val result = expenseRepository.createExpenseWithSplits(
                houseId = house.id,
                paidById = paidBy.id,
                amount = amount,
                description = state.title,
                splitUserIds = state.splitBetween.toList(),
                amountPerUser = amountPerUser
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "Failed to save expense")
                    }
                }
            )
        }
    }
}
