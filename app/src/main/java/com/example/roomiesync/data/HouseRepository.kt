package com.example.roomiesync.data

import com.example.roomiesync.auth.AuthRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HouseRepository(
    private val authRepository: AuthRepository
) {

    private val client get() = SupabaseClient.client

    suspend fun createHouse(name: String, address: String): Result<House> = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUser()?.id ?: return@withContext Result.failure(
            IllegalStateException("Not authenticated")
        )
        runCatching {
            client.postgrest.from("houses").insert(
                CreateHousePayload(name = name, address = address, createdBy = userId)
            ) {
                select()
            }.decodeSingle<House>()
        }
    }

    suspend fun joinHouseByCode(joinCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        val code = joinCode.trim().uppercase()
        if (code.isBlank()) return@withContext Result.failure(IllegalArgumentException("Code is empty"))
        runCatching {
            val result = client.postgrest.rpc(
                "join_house_by_code",
                buildJsonObject { put("p_join_code", code) }
            )
            val response = Json.decodeFromString<JoinHouseByCodeResponse>(result.data)
            when {
                response.ok -> Unit
                response.error == "invalid_code" -> throw InvalidJoinCodeException()
                response.error == "not_authenticated" -> throw IllegalStateException("Not authenticated")
                else -> throw RuntimeException(response.error ?: "Unknown error")
            }
        }
    }

    suspend fun getUserHouse(): House? = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUser()?.id ?: return@withContext null
        try {
            val response = client.postgrest.from("house_members")
                .select(Columns.list("houses(*)")) {
                    filter {
                        eq("user_id", userId)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<HouseMemberWithHouse>()
            response?.houses
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class InvalidJoinCodeException : Exception("Invalid or expired invite code")
