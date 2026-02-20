package com.example.roomiesync.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateHousePayload(
    val name: String,
    val address: String,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class House(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("join_code") val joinCode: String? = null, // join_code can be null in DB but schema says unique, implied nullable? Schema: join_code text unique. It is not NOT NULL.
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class JoinHouseByCodeParams(
    @SerialName("p_join_code") val joinCode: String
)

@Serializable
data class JoinHouseByCodeResponse(
    val ok: Boolean,
    val error: String? = null
)

@Serializable
data class HouseMemberWithHouse(
    val houses: House
)
