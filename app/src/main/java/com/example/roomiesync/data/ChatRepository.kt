package com.example.roomiesync.data

import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatRepository(
    private val profileRepository: ProfileRepository = ProfileRepository()
) {

    private val client get() = SupabaseClient.client

    /**
     * Fetch all messages for a house, ordered oldest-first, with sender profiles attached.
     */
    suspend fun getMessagesForHouse(houseId: String): List<MessageWithProfile> = withContext(Dispatchers.IO) {
        try {
            val messages = client.postgrest.from("messages")
                .select() {
                    filter { eq("house_id", houseId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<Message>()

            if (messages.isEmpty()) return@withContext emptyList()

            val senderIds = messages.map { it.senderId }.distinct()
            val profiles = profileRepository.getProfiles(senderIds).associateBy { it.id }

            messages.map { msg ->
                val profile = profiles[msg.senderId] ?: Profile(msg.senderId, null, null)
                MessageWithProfile(msg, profile)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    private suspend fun attachProfiles(messages: List<Message>): List<MessageWithProfile> {
        val sortedMessages = messages.sortedBy { it.createdAt }
        val senderIds = sortedMessages.map { it.senderId }.distinct()
        val profiles = profileRepository.getProfiles(senderIds).associateBy { it.id }

        return sortedMessages.map { message ->
            val profile = profiles[message.senderId] ?: Profile(message.senderId, null, null)
            MessageWithProfile(message, profile)
        }
    }

    @OptIn(SupabaseExperimental::class)
    fun observeMessagesForHouse(houseId: String): Flow<List<MessageWithProfile>> {
        return channelFlow {
            var lastEmitted: List<MessageWithProfile>? = null

            suspend fun emitIfChanged(messages: List<MessageWithProfile>) {
                if (messages != lastEmitted) {
                    lastEmitted = messages
                    trySend(messages)
                }
            }

            launch {
                client.postgrest.from("messages")
                    .selectAsFlow<Message, String>(
                        primaryKey = Message::id,
                        filter = FilterOperation("house_id", FilterOperator.EQ, houseId)
                    )
                    .map { messages: List<Message> -> attachProfiles(messages) }
                    .collect { messages ->
                        emitIfChanged(messages)
                    }
            }

            launch {
                while (isActive) {
                    emitIfChanged(getMessagesForHouse(houseId))
                    delay(MESSAGE_POLL_INTERVAL_MS)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * Insert a new message. Returns the created Message row.
     */
    suspend fun sendMessage(houseId: String, senderId: String, content: String): Result<Message> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.postgrest.from("messages")
                    .insert(CreateMessagePayload(houseId, senderId, content)) {
                        select()
                    }
                    .decodeSingle<Message>()
            }
        }

    private companion object {
        const val MESSAGE_POLL_INTERVAL_MS = 2_500L
    }
}
