package com.example.roomiesync.auth

import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository {

    private val auth: Auth
        get() = SupabaseClient.client.auth

    val sessionFlow: Flow<AuthState> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> status.session.user?.let { AuthState.Authenticated(it) } ?: AuthState.NotAuthenticated
            is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
            SessionStatus.Initializing -> AuthState.Initializing
            is SessionStatus.RefreshFailure -> AuthState.NotAuthenticated
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

    fun currentUser(): UserInfo? = auth.currentUserOrNull()
}

sealed class AuthState {
    data object Initializing : AuthState()
    data object NotAuthenticated : AuthState()
    data class Authenticated(val user: UserInfo) : AuthState()
}
