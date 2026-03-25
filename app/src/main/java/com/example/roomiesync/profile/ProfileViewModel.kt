package com.example.roomiesync.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.BuildConfig
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.HouseRepository
import com.example.roomiesync.data.House
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository()),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val user = authRepository.currentUser()
            if (user != null) {
                if (BuildConfig.DEBUG) {
                    Log.d("ProfileViewModel", "Current user ID: ${user.id}")
                }
                val profile = profileRepository.getProfile(user.id)
                if (BuildConfig.DEBUG) {
                    Log.d("ProfileViewModel", "Fetched profile: $profile")
                }
                val house = houseRepository.getUserHouse()
                
                _uiState.update { 
                    it.copy(
                        profile = profile, 
                        house = house, 
                        isLoading = false
                    ) 
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e("ProfileViewModel", "Current user is null!")
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateAvatarUrl(url: String) {
        val currentProfile = _uiState.value.profile ?: return
        val updatedProfile = currentProfile.copy(avatarUrl = url)
        
        viewModelScope.launch {
            // Optimistically update UI
            _uiState.update { it.copy(profile = updatedProfile) }
            // Save to database
            val success = profileRepository.updateProfile(updatedProfile)
            if (BuildConfig.DEBUG) {
                Log.d("ProfileViewModel", "Avatar update to DB success: $success for URL: $url")
            }
        }
    }

    fun uploadAvatar(bytes: ByteArray) {
        viewModelScope.launch {
            try {
                val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val publicUrl = withContext(Dispatchers.IO) {
                    val bucket = SupabaseClient.client.storage.from("avatars")
                    val response = bucket.upload(fileName, bytes) {
                        upsert = true
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d("ProfileViewModel", "Upload response: $response")
                    }
                    
                    // Let the Supabase SDK generate the correct public URL automatically
                    bucket.publicUrl(fileName)
                }

                if (BuildConfig.DEBUG) {
                    Log.d("ProfileViewModel", "Generated public URL: $publicUrl")
                }
                updateAvatarUrl(publicUrl)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("ProfileViewModel", "Error uploading avatar", e)
                }
            }
        }
    }
}

data class ProfileUiState(
    val profile: Profile? = null,
    val house: House? = null,
    val isLoading: Boolean = false
)
