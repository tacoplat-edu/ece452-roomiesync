package com.example.roomiesync.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    init {
        authRepository.sessionFlow
            .onEach { authState ->
                _uiState.update {
                    when (authState) {
                        is AuthState.Initializing -> AuthUiState.Loading
                        is AuthState.NotAuthenticated -> AuthUiState.ShowAuthScreen
                        is AuthState.Authenticated -> AuthUiState.Authenticated(authState.user)
                    }
                }
            }
            .catch { _ -> _uiState.update { AuthUiState.ShowAuthScreen } }
            .launchIn(viewModelScope)
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        _authMessage.value = null
        when {
            email.isBlank() -> _authMessage.value = "Email is required"
            password.isBlank() -> _authMessage.value = "Password is required"
            password != confirmPassword -> _authMessage.value = "Passwords do not match"
            password.length < 6 -> _authMessage.value = "Password must be at least 6 characters"
            else -> viewModelScope.launch {
                authRepository.signUp(email, password)
                    .onSuccess { _authMessage.value = "Account created. You can sign in." }
                    .onFailure { _authMessage.value = it.message ?: "Sign up failed" }
            }
        }
    }

    fun signIn(email: String, password: String) {
        _authMessage.value = null
        when {
            email.isBlank() -> _authMessage.value = "Email is required"
            password.isBlank() -> _authMessage.value = "Password is required"
            else -> viewModelScope.launch {
                authRepository.signIn(email, password)
                    .onSuccess { }
                    .onFailure { _authMessage.value = it.message ?: "Sign in failed" }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun signInWithGoogle() {
        _authMessage.value = null
        viewModelScope.launch {
            authRepository.signInWithGoogle()
                .onFailure { _authMessage.value = it.message ?: "Google sign-in failed" }
        }
    }

    fun clearMessage() {
        _authMessage.value = null
    }
}

sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object ShowAuthScreen : AuthUiState()
    data class Authenticated(val user: io.github.jan.supabase.auth.user.UserInfo) : AuthUiState()
}
