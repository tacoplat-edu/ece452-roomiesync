package com.example.roomiesync.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.HouseRepository
import com.example.roomiesync.data.House
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository())
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val user = authRepository.currentUser()
            if (user != null) {
                val profile = ProfileRepository().getProfile(user.id)
                val house = houseRepository.getUserHouse()
                
                _uiState.update { 
                    it.copy(
                        profile = profile, 
                        house = house, 
                        isLoading = false
                    ) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class ProfileUiState(
    val profile: Profile? = null,
    val house: House? = null,
    val isLoading: Boolean = false
)
