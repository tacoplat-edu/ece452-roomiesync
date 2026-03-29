package com.example.roomiesync.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.data.ChatRepository
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.Message
import com.example.roomiesync.data.MessageWithProfile
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class ChatState(
    val isLoading: Boolean = true,
    val currentUserId: String = "",
    val currentHouseId: String = "",
    val currentUserProfile: Profile? = null,
    val messages: List<MessageWithProfile> = emptyList(),
    val inputText: String = ""
)

class ChatViewModel(
    private val chatRepository: ChatRepository = ChatRepository(),
    private val choreRepository: ChoreRepository = ChoreRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
            val house = choreRepository.getUserHouse(userId)
            val houseId = house?.id ?: return@launch
            val profile = profileRepository.getProfile(userId)

            _uiState.update { it.copy(currentUserId = userId, currentHouseId = houseId, currentUserProfile = profile) }

            val messages = chatRepository.getMessagesForHouse(houseId)
            _uiState.update { it.copy(isLoading = false, messages = messages) }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        if (currentState.inputText.isBlank()) return
        if (currentState.currentUserId.isEmpty() || currentState.currentHouseId.isEmpty()) return

        val content = currentState.inputText.trim()
        val profile = currentState.currentUserProfile ?: Profile(id = currentState.currentUserId)

        // Optimistic update
        val optimisticMessage = MessageWithProfile(
            message = Message(
                id = UUID.randomUUID().toString(),
                houseId = currentState.currentHouseId,
                senderId = currentState.currentUserId,
                content = content,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                    .format(Date())
            ),
            profile = profile
        )
        _uiState.update { it.copy(messages = it.messages + optimisticMessage, inputText = "") }

        viewModelScope.launch {
            chatRepository.sendMessage(
                houseId = currentState.currentHouseId,
                senderId = currentState.currentUserId,
                content = content
            )
        }
    }
}
