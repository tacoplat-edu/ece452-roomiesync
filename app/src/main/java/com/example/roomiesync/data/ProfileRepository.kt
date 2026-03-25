package com.example.roomiesync.data

import android.util.Log
import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.postgrest.from

class ProfileRepository {

    suspend fun getProfile(userId: String): Profile? {
        return try {
            val response = SupabaseClient.client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
            if (BuildConfig.DEBUG) {
                Log.d("ProfileRepository", "Raw getProfile JSON: ${response.data}")
            }
            response.decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("ProfileRepository", "Error decoding profile", e)
            }
            null
        }
    }

    suspend fun getProfiles(userIds: List<String>): List<Profile> {
        if (userIds.isEmpty()) return emptyList()
        return try {
            SupabaseClient.client.from("profiles")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<Profile>()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            emptyList()
        }
    }

    suspend fun updateProfile(profile: Profile): Boolean {
        return try {
            SupabaseClient.client.from("profiles")
                .update(
                    {
                        set("display_name", profile.displayName)
                        set("avatar_url", profile.avatarUrl)
                    }
                ) {
                    filter {
                        eq("id", profile.id)
                    }
                }
            true
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            false
        }
    }
}
