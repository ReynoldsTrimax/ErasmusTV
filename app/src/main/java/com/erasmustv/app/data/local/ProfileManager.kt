package com.erasmustv.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erasmustv.app.data.model.WatchProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.profileDataStore by preferencesDataStore(name = "erasmus_profile")

class ProfileManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val ACTIVE_PROFILE_JSON = stringPreferencesKey("active_profile_json")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    val activeProfileFlow: Flow<WatchProfile?> = context.profileDataStore.data.map { prefs ->
        val raw = prefs[Keys.ACTIVE_PROFILE_JSON] ?: return@map null
        try {
            json.decodeFromString<WatchProfile>(raw)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getActiveProfile(): WatchProfile? {
        val raw = context.profileDataStore.data.first()[Keys.ACTIVE_PROFILE_JSON] ?: return null
        return try {
            json.decodeFromString<WatchProfile>(raw)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getActiveProfileId(): String? {
        return context.profileDataStore.data.first()[Keys.ACTIVE_PROFILE_ID]
    }

    suspend fun setActiveProfile(profile: WatchProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE_ID] = profile.id
            prefs[Keys.ACTIVE_PROFILE_JSON] = json.encodeToString(profile)
        }
    }

    suspend fun clearActiveProfile() {
        context.profileDataStore.edit { prefs ->
            prefs.remove(Keys.ACTIVE_PROFILE_ID)
            prefs.remove(Keys.ACTIVE_PROFILE_JSON)
        }
    }

    suspend fun getProfilesForUser(userId: String): List<WatchProfile> {
        val key = stringPreferencesKey("profiles_$userId")
        val raw = context.profileDataStore.data.first()[key] ?: return emptyList()
        return try {
            json.decodeFromString<List<WatchProfile>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveProfilesForUser(userId: String, profiles: List<WatchProfile>) {
        val key = stringPreferencesKey("profiles_$userId")
        context.profileDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(profiles)
        }
    }
}

