package com.example.roomiesync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.House
import com.example.roomiesync.data.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = true,
    val house: House? = null
) {
    val hasHouse: Boolean get() = house != null
}

class MainViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository())
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadHouse()
    }

    private fun loadHouse() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val house = houseRepository.getUserHouse()
            _uiState.update {
                it.copy(isLoading = false, house = house)
            }
        }
    }

    /** Call after user completes onboarding (creates or joins a house) so we refresh and navigate to home. */
    fun refreshHouse() {
        loadHouse()
    }
}
