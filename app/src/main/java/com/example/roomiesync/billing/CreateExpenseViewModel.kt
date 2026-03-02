package com.example.roomiesync.billing

import androidx.lifecycle.ViewModel
import com.example.roomiesync.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CreateExpenseState(
    val title: String = "",
    val amount: String = "",
    val paidBy: Profile? = null,
    val splitBetween: Set<String> = emptySet(),
    val householdMembers: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CreateExpenseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateExpenseState())
    val uiState: StateFlow<CreateExpenseState> = _uiState.asStateFlow()

    init {
        val me = Profile("user-1", "alice@example.com", "Alice")
        val members = listOf(
            me,
            Profile("user-2", "bob@example.com", "Bob"),
            Profile("user-3", "charlie@example.com", "Charlie")
        )
        _uiState.update {
            it.copy(
                householdMembers = members,
                paidBy = me,
                splitBetween = members.map { p -> p.id }.toSet() // split between everyone by default
            )
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
        if (state.title.isBlank() || state.amount.isBlank() || state.amount.toDoubleOrNull() == null) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid title and amount.") }
            return
        }
        if (state.splitBetween.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Must split with at least one person.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // TODO: Map to Expense and ExpenseSplit models and save to Supabase
        _uiState.update { it.copy(isLoading = false) }
        onSuccess()
    }
}