package com.example.roomiesync.data

import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
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
}
