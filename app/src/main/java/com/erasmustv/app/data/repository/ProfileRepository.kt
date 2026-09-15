package com.erasmustv.app.data.repository

import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.local.SessionManager
import com.erasmustv.app.data.model.CreateProfileRequest
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.remote.SupabaseApiService
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val supabaseApi: SupabaseApiService,
    private val profileManager: ProfileManager,
    private val sessionManager: SessionManager
) {
    val activeProfileFlow: Flow<WatchProfile?> = profileManager.activeProfileFlow

    suspend fun getActiveProfile(): WatchProfile? = profileManager.getActiveProfile()

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

        return try {
            val profiles = supabaseApi.getWatchProfiles()
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

        val userId = sessionManager.getUserId() ?: ""
        if (userId.isBlank()) {
            return Result.failure(IllegalStateException("Cannot create profile without an authenticated session."))
        }

        return try {
            val request = CreateProfileRequest(
                userId = userId,
                name = name.trim(),
                avatarKey = avatarKey,
                birthYear = birthYear
            )
            val created = supabaseApi.createWatchProfile(request).first()
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: WatchProfile): Result<WatchProfile> {
        if (sessionManager.isGuest()) {
            if (profileManager.getActiveProfileId() == profile.id) {
                profileManager.setActiveProfile(profile)
            }
            return Result.success(profile)
        }

        return try {
            val updated = supabaseApi.updateWatchProfile("eq.${profile.id}", profile).first()
            if (profileManager.getActiveProfileId() == profile.id) {
                profileManager.setActiveProfile(updated)
            }
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProfile(profileId: String): Result<Unit> {
        if (sessionManager.isGuest()) {
            if (profileManager.getActiveProfileId() == profileId) {
                profileManager.clearActiveProfile()
            }
            return Result.success(Unit)
        }

        return try {
            supabaseApi.deleteWatchProfile("eq.$profileId")
            if (profileManager.getActiveProfileId() == profileId) {
                profileManager.clearActiveProfile()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

