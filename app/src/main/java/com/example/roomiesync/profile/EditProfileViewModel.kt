package com.example.roomiesync.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val displayName: String = "",
    val email: String = "", // Added Email
    val avatarUrl: String = ""
)

class EditProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileState())
    val uiState: StateFlow<EditProfileState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authRepository.currentUser()
            if (user != null) {
                val profile = profileRepository.getProfile(user.id)
                _uiState.update { 
                    it.copy(
                        profile = profile, 
                        displayName = profile?.displayName ?: "",
                        email = user.email ?: "",
                        avatarUrl = profile?.avatarUrl ?: "",
                        isLoading = false
                    ) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "User not found") }
            }
        }
    }

    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val currentProfile = currentState.profile ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val newProfile = currentProfile.copy(displayName = currentState.displayName)
            val profileSuccess = profileRepository.updateProfile(newProfile)
            if (!profileSuccess) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to update profile") }
                return@launch
            }

            val currentEmail = authRepository.currentUser()?.email
            if (currentState.email.isNotBlank() && currentState.email != currentEmail) {
                authRepository.updateEmail(currentState.email).onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to update email: ${e.message}") }
                    return@launch
                }
            }

            _uiState.update { it.copy(isSaving = false, profile = newProfile) }
            onSuccess()
        }
    }
}
