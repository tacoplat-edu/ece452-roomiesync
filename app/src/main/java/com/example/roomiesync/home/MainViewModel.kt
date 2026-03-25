package com.example.roomiesync.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository())
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkUserHouse()
    }

    fun checkUserHouse() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val house = houseRepository.getUserHouse()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userHasHouse = house != null
                )
            }
        }
    }
}

data class MainUiState(
    val isLoading: Boolean = true,
    val userHasHouse: Boolean = false
)
