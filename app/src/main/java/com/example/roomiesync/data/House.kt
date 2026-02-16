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
data class HouseRow(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("join_code") val joinCode: String,
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
