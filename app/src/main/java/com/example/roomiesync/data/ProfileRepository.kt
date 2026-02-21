package com.example.roomiesync.data

import io.github.jan.supabase.postgrest.from

class ProfileRepository {

    suspend fun getProfile(userId: String): Profile? {
        return try {
            SupabaseClient.client.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
            e.printStackTrace()
            false
        }
    }
}
