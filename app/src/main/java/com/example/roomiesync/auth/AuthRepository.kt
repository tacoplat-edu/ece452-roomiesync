package com.example.roomiesync.auth

import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class AuthRepository(
    private val profileRepository: ProfileRepository = ProfileRepository()
) {

    private val auth: Auth
        get() = SupabaseClient.client.auth

    val sessionFlow: Flow<AuthState> = auth.sessionStatus.transform { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                if (user != null) {
                    val profile = profileRepository.getProfile(user.id)
                    emit(AuthState.Authenticated(user, profile))
                } else {
                    emit(AuthState.NotAuthenticated)
                }
            }
            is SessionStatus.NotAuthenticated -> emit(AuthState.NotAuthenticated)
            SessionStatus.Initializing -> emit(AuthState.Initializing)
            is SessionStatus.RefreshFailure -> emit(AuthState.NotAuthenticated)
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        auth.signInWith(Google)
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        auth.updateUser {
            email = newEmail
        }
    }

    fun currentUser(): UserInfo? = auth.currentUserOrNull()
}

sealed class AuthState {
    data object Initializing : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(val user: UserInfo, val profile: Profile? = null) : AuthState()
}
