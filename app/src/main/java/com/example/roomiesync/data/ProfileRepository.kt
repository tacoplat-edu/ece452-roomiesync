package com.example.roomiesync.data

import com.example.roomiesync.BuildConfig
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository {

    /**
     * Upload avatar image bytes to Supabase Storage and return the public URL.
     * Uses bucket "avatars" with path "{userId}.jpg" (overwrites on each upload).
     * Create a public bucket named "avatars" in Supabase Dashboard if it doesn't exist.
     */
    suspend fun uploadAvatar(userId: String, bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "$userId.jpg"
            SupabaseClient.client.storage.from("avatars").upload(path, bytes) {
                upsert = true
            }
            // Build public URL (bucket must be public).
            // Note: Some supabase-kt versions expose getPublicUrl(), but constructing the URL
            // is the most compatible approach across versions.
            val baseUrl = if (BuildConfig.SUPABASE_S3_URL.isNotBlank()) {
                BuildConfig.SUPABASE_S3_URL.trimEnd('/')
            } else {
                BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public"
            }
            "$baseUrl/avatars/$path"
        }
    }

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
            e.printStackTrace()
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
            e.printStackTrace()
            false
        }
    }
}
