package com.example.roomiesync.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.BuildConfig
import com.example.roomiesync.auth.AuthRepository
import com.example.roomiesync.data.ChatRepository
import com.example.roomiesync.data.HouseRepository
import com.example.roomiesync.data.Message
import com.example.roomiesync.data.MessageWithProfile
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class ChatState(
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val currentHouseId: String = "",
    val messages: List<MessageWithProfile> = emptyList(),
    val inputText: String = "",
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val houseRepository: HouseRepository = HouseRepository(AuthRepository()),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    // Cache profiles to avoid re-fetching for each realtime message
    private val profileCache = mutableMapOf<String, Profile>()

    private val realtimeChannel by lazy {
        SupabaseClient.client.channel("house-chat-${_uiState.value.currentHouseId}")
    }

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Not authenticated") }
                return@launch
            }

            val house = houseRepository.getUserHouse()
            if (house == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No household found") }
                return@launch
            }

            _uiState.update { it.copy(currentUserId = userId, currentHouseId = house.id) }

            try {
                val messagesWithProfiles = chatRepository.getMessagesForHouse(house.id)

                // Populate profile cache
                messagesWithProfiles.forEach { mwp ->
                    profileCache[mwp.profile.id] = mwp.profile
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = messagesWithProfiles
                    )
                }

                // Start listening for new messages
                subscribeToRealtime(house.id)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load messages")
                }
            }
        }
    }

    private fun subscribeToRealtime(houseId: String) {
        viewModelScope.launch {
            try {
                val changeFlow = realtimeChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter("house_id", FilterOperator.EQ, houseId)
                }

                realtimeChannel.subscribe()

                changeFlow.collect { change ->
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val record = change.record
                        val newMessage = json.decodeFromJsonElement(Message.serializer(), record)

                        // Don't duplicate messages we already optimistically added
                        val alreadyExists = _uiState.value.messages.any { it.message.id == newMessage.id }
                        if (alreadyExists) return@collect

                        val profile = profileCache[newMessage.senderId]
                            ?: profileRepository.getProfile(newMessage.senderId)
                            ?: Profile(newMessage.senderId, null, null)

                        profileCache[newMessage.senderId] = profile

                        val newMessageWithProfile = MessageWithProfile(newMessage, profile)
                        _uiState.update { state ->
                            state.copy(messages = state.messages + newMessageWithProfile)
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e("ChatViewModel", "Error processing realtime message", e)
                        }
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("ChatViewModel", "Realtime subscription error", e)
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        if (currentState.inputText.isBlank()) return
        if (currentState.currentHouseId.isEmpty() || currentState.currentUserId.isEmpty()) return

        val content = currentState.inputText.trim()

        // Clear input immediately
        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                houseId = currentState.currentHouseId,
                senderId = currentState.currentUserId,
                content = content
            )

            result.onSuccess { savedMessage ->
                // Optimistically add if not already added via realtime
                val alreadyExists = _uiState.value.messages.any { it.message.id == savedMessage.id }
                if (!alreadyExists) {
                    val myProfile = profileCache[currentState.currentUserId]
                        ?: Profile(currentState.currentUserId, null, null)
                    val messageWithProfile = MessageWithProfile(savedMessage, myProfile)
                    _uiState.update { state ->
                        state.copy(messages = state.messages + messageWithProfile)
                    }
                }
            }

            result.onFailure { e ->
                if (BuildConfig.DEBUG) {
                    Log.e("ChatViewModel", "Failed to send message", e)
                }
                // Restore the text so the user can retry
                _uiState.update { it.copy(inputText = content) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                realtimeChannel.unsubscribe()
                SupabaseClient.client.realtime.removeChannel(realtimeChannel)
            } catch (_: Exception) {}
        }
    }
}