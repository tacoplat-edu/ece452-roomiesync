package com.example.roomiesync.chat

import androidx.lifecycle.viewModelScope
import com.example.roomiesync.BuildConfig
import androidx.lifecycle.ViewModel
import com.example.roomiesync.data.ChatRepository
import com.example.roomiesync.data.ChoreRepository
import com.example.roomiesync.data.MessageWithProfile
import com.example.roomiesync.data.Profile
import com.example.roomiesync.data.ProfileRepository
import com.example.roomiesync.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private var messageObservationJob: Job? = null

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val house = choreRepository.getUserHouse(userId)
            val houseId = house?.id
            if (houseId == null) {
                _uiState.update { it.copy(isLoading = false, currentUserId = userId) }
                return@launch
            }

            val profile = profileRepository.getProfile(userId)

            _uiState.update { it.copy(currentUserId = userId, currentHouseId = houseId, currentUserProfile = profile) }
            observeMessages(houseId)
        }
    }

    private fun observeMessages(houseId: String) {
        messageObservationJob?.cancel()
        messageObservationJob = viewModelScope.launch {
            chatRepository.observeMessagesForHouse(houseId)
                .catch { throwable ->
                    if (BuildConfig.DEBUG) {
                        throwable.printStackTrace()
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { messages ->
                    _uiState.update { it.copy(isLoading = false, messages = messages) }
                }
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
        _uiState.update { it.copy(inputText = "") }

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                houseId = currentState.currentHouseId,
                senderId = currentState.currentUserId,
                content = content
            )

            val sentMessage = result.getOrNull()
            if (sentMessage != null) {
                val profile = currentState.currentUserProfile ?: Profile(id = currentState.currentUserId)
                mergeMessage(
                    MessageWithProfile(
                        message = sentMessage,
                        profile = profile
                    )
                )
            } else {
                if (BuildConfig.DEBUG) {
                    result.exceptionOrNull()?.printStackTrace()
                }
                _uiState.update { state ->
                    state.copy(
                        inputText = if (state.inputText.isBlank()) content else state.inputText
                    )
                }
            }
        }
    }

    private fun mergeMessage(message: MessageWithProfile) {
        _uiState.update { state ->
            val alreadyPresent = state.messages.any { it.message.id == message.message.id }
            if (alreadyPresent) {
                state
            } else {
                state.copy(
                    messages = (state.messages + message).sortedBy { it.message.createdAt }
                )
            }
        }
    }
}
