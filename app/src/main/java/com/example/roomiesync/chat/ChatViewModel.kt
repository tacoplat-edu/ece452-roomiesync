package com.example.roomiesync.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomiesync.data.Message
import com.example.roomiesync.data.MessageWithProfile
import com.example.roomiesync.data.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatState(
    val isLoading: Boolean = true,
    val currentUserId: String = "user-1",
    val currentHouseId: String = "house-1",
    val messages: List<MessageWithProfile> = emptyList(),
    val inputText: String = ""
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Mock data
            val me = Profile("user-1", "user1@example.com", "Tom")
            val bob = Profile("user-2", "user2@example.com", "Bob")

            val now = System.currentTimeMillis()

            val mockMessages = listOf(
                MessageWithProfile(
                    Message("m1", "house-1", "user-2", "Did someone take the trash out?", now - 3600000),
                    bob
                ),
                MessageWithProfile(
                    Message("m2", "house-1", "user-1", "Yeah I did", now - 3500000),
                    me
                ),
                MessageWithProfile(
                    Message("m3", "house-1", "user-2", "Thanks!", now - 3400000),
                    bob
                ),
                MessageWithProfile(
                    Message("m4", "house-1", "user-1", "No problem.", now - 60000),
                    me
                )
            )

            _uiState.update {
                it.copy(
                    isLoading = false,
                    messages = mockMessages
                )
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        if (currentState.inputText.isBlank()) return

        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            houseId = currentState.currentHouseId,
            senderId = currentState.currentUserId,
            content = currentState.inputText.trim(),
            createdAt = System.currentTimeMillis()
        )

        val myProfile = Profile(currentState.currentUserId, "user1@example.com", "Tom")
        val newMessageWithProfile = MessageWithProfile(newMessage, myProfile)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + newMessageWithProfile,
                inputText = ""
            )
        }

        // TODO: Add to database here
    }
}