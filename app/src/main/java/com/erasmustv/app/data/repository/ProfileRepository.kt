package com.erasmustv.app.data.repository

import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.local.SessionManager
import com.erasmustv.app.data.model.CreateProfileRequest
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.remote.SupabaseApiService
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import java.util.UUID

class ProfileRepository(
    private val supabaseApi: SupabaseApiService,
    private val profileManager: ProfileManager,
    private val sessionManager: SessionManager
) {
    val activeProfileFlow: Flow<WatchProfile?> = profileManager.activeProfileFlow

    suspend fun getActiveProfile(): WatchProfile? {
        val active = profileManager.getActiveProfile() ?: return null
        val currentUserId = if (sessionManager.isGuest()) "guest_user_erasmus" else sessionManager.getUserId()
        if (currentUserId != null && active.userId != currentUserId) {
            profileManager.clearActiveProfile()
            return null
        }
        return active
    }

    suspend fun selectProfile(profile: WatchProfile) {
        profileManager.setActiveProfile(profile)
    }

    suspend fun clearActiveProfile() {
        profileManager.clearActiveProfile()
    }

    suspend fun getProfiles(): Result<List<WatchProfile>> {
        if (sessionManager.isGuest()) {
            val guestProfile = WatchProfile(
                id = "guest_profile",
                userId = "guest_user_erasmus",
                name = "Guest",
                avatarKey = "slate",
                birthYear = 2000
            )
            return Result.success(listOf(guestProfile))
        }

        val userId = sessionManager.getUserId()?.ifBlank { null } ?: "authenticated_user"

        // 1. Attempt to fetch from Supabase watch_profiles table
        try {
            val remoteProfiles = supabaseApi.getWatchProfiles()
            if (remoteProfiles.isNotEmpty()) {
                profileManager.saveProfilesForUser(userId, remoteProfiles)
                return Result.success(remoteProfiles)
            }
        } catch (_: Exception) {
            // Supabase watch_profiles table might not exist (e.g. 404 PGRST205) or device is offline.
            // Fall back gracefully to local storage.
        }

        // 2. Check local persistent storage for this user
        val localProfiles = profileManager.getProfilesForUser(userId)
        if (localProfiles.isNotEmpty()) {
            return Result.success(localProfiles)
        }

        // 3. First time login with no remote table: synthesize default profile from user credentials
        val userEmail = sessionManager.getUserEmail()
        val defaultName = userEmail
            ?.substringBefore("@")
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            ?.ifBlank { "Profile 1" }
            ?: "Profile 1"

        val defaultProfile = WatchProfile(
            id = "profile_${userId.take(8).replace("-", "")}",
            userId = userId,
            name = defaultName,
            avatarKey = "crimson",
            birthYear = 2000
        )
        val initialProfiles = listOf(defaultProfile)
        profileManager.saveProfilesForUser(userId, initialProfiles)
        return Result.success(initialProfiles)
    }

    suspend fun createProfile(name: String, avatarKey: String, birthYear: Int): Result<WatchProfile> {
        if (sessionManager.isGuest()) {
            val guestCustomProfile = WatchProfile(
                id = "guest_profile_${System.currentTimeMillis()}",
                userId = "guest_user_erasmus",
                name = name.trim(),
                avatarKey = avatarKey,
                birthYear = birthYear
            )
            return Result.success(guestCustomProfile)
        }

        val userId = sessionManager.getUserId()?.ifBlank { null } ?: "authenticated_user"

        var remoteCreated: WatchProfile? = null
        try {
            val request = CreateProfileRequest(
                userId = userId,
                name = name.trim(),
                avatarKey = avatarKey,
                birthYear = birthYear
            )
            remoteCreated = supabaseApi.createWatchProfile(request).firstOrNull()
        } catch (_: Exception) {
            // Remote call failed (e.g. table not found or offline). Fall back to local creation.
        }

        val newProfile = remoteCreated ?: WatchProfile(
            id = "profile_${UUID.randomUUID().toString().take(8).replace("-", "")}",
            userId = userId,
            name = name.trim(),
            avatarKey = avatarKey,
            birthYear = birthYear
        )

        val currentProfiles = profileManager.getProfilesForUser(userId).toMutableList()
        if (currentProfiles.none { it.id == newProfile.id }) {
            if (currentProfiles.size < 5) {
                currentProfiles.add(newProfile)
                profileManager.saveProfilesForUser(userId, currentProfiles)
            }
        }

        return Result.success(newProfile)
    }

    suspend fun updateProfile(profile: WatchProfile): Result<WatchProfile> {
        if (sessionManager.isGuest()) {
            if (profileManager.getActiveProfileId() == profile.id) {
                profileManager.setActiveProfile(profile)
            }
            return Result.success(profile)
        }

        val userId = sessionManager.getUserId()?.ifBlank { null } ?: "authenticated_user"

        var remoteUpdated: WatchProfile? = null
        try {
            remoteUpdated = supabaseApi.updateWatchProfile("eq.${profile.id}", profile).firstOrNull()
        } catch (_: Exception) {
            // Remote call failed; update locally
        }

        val finalProfile = remoteUpdated ?: profile
        val currentProfiles = profileManager.getProfilesForUser(userId).toMutableList()
        val index = currentProfiles.indexOfFirst { it.id == finalProfile.id }
        if (index != -1) {
            currentProfiles[index] = finalProfile
        } else {
            currentProfiles.add(finalProfile)
        }
        profileManager.saveProfilesForUser(userId, currentProfiles)

        if (profileManager.getActiveProfileId() == finalProfile.id) {
            profileManager.setActiveProfile(finalProfile)
        }
        return Result.success(finalProfile)
    }

    suspend fun deleteProfile(profileId: String): Result<Unit> {
        if (sessionManager.isGuest()) {
            if (profileManager.getActiveProfileId() == profileId) {
                profileManager.clearActiveProfile()
            }
            return Result.success(Unit)
        }

        val userId = sessionManager.getUserId()?.ifBlank { null } ?: "authenticated_user"

        try {
            supabaseApi.deleteWatchProfile("eq.$profileId")
        } catch (_: Exception) {
            // Remote call failed; delete locally
        }

        val currentProfiles = profileManager.getProfilesForUser(userId).toMutableList()
        currentProfiles.removeAll { it.id == profileId }
        profileManager.saveProfilesForUser(userId, currentProfiles)

        if (profileManager.getActiveProfileId() == profileId) {
            profileManager.clearActiveProfile()
        }
        return Result.success(Unit)
    }
}


