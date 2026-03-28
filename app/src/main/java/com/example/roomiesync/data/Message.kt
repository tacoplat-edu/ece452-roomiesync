package com.example.roomiesync.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    @SerialName("house_id") val houseId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateMessagePayload(
    @SerialName("house_id") val houseId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String
)

data class MessageWithProfile(
    val message: Message,
    val profile: Profile
)