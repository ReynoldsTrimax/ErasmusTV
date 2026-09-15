package com.erasmustv.app.data.repository

import com.erasmustv.app.data.local.SessionManager
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchlistItemEntity
import com.erasmustv.app.data.remote.SupabaseApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class WatchlistRepository(
    private val supabaseApi: SupabaseApiService,
    private val sessionManager: SessionManager
) {
    private val _watchlistKeys = MutableStateFlow<Set<String>>(emptySet())
    val watchlistKeys: StateFlow<Set<String>> = _watchlistKeys.asStateFlow()

    // Local in-memory cache keyed by profileId -> (itemKey -> MediaItem)
    private val localProfileWatchlists = ConcurrentHashMap<String, MutableMap<String, MediaItem>>()

    private fun itemKey(mediaType: String, tmdbId: String) = "${mediaType.lowercase()}:$tmdbId"

    private fun getProfileMap(profileId: String): MutableMap<String, MediaItem> {
        return localProfileWatchlists.getOrPut(profileId) { ConcurrentHashMap() }
    }

    suspend fun getWatchlist(profileId: String): Result<List<MediaItem>> {
        val profileMap = getProfileMap(profileId)
        return try {
            val remoteItems = supabaseApi.getWatchlist("eq.$profileId")
            val keys = remoteItems.map { itemKey(it.mediaType, it.tmdbId) }.toSet()
            _watchlistKeys.value = _watchlistKeys.value + keys

            for (it in remoteItems) {
                val key = itemKey(it.mediaType, it.tmdbId)
                profileMap[key] = MediaItem(
                    id = it.tmdbId,
                    title = it.title,
                    posterPath = it.posterPath,
                    backdropPath = it.backdropPath,
                    mediaType = it.mediaType,
                    releaseDate = it.releaseDate
                )
            }
            Result.success(profileMap.values.toList())
        } catch (e: Exception) {
            // Gracefully fall back to locally saved watchlist items (e.g. for guest mode or offline)
            _watchlistKeys.value = _watchlistKeys.value + profileMap.keys
            Result.success(profileMap.values.toList())
        }
    }

    suspend fun addToWatchlist(
        profileId: String,
        mediaItem: MediaItem
    ): Result<Unit> {
        val profileMap = getProfileMap(profileId)
        val key = itemKey(mediaItem.mediaType, mediaItem.id)
        profileMap[key] = mediaItem
        _watchlistKeys.value = _watchlistKeys.value + key

        return runCatching {
            val userId = sessionManager.getUserId() ?: ""
            val entity = WatchlistItemEntity(
                userId = userId,
                profileId = profileId,
                mediaType = mediaItem.mediaType,
                tmdbId = mediaItem.id,
                title = mediaItem.title,
                posterPath = mediaItem.posterPath,
                backdropPath = mediaItem.backdropPath,
                releaseDate = mediaItem.releaseDate
            )
            supabaseApi.addToWatchlist(entity)
        }
    }

    suspend fun removeFromWatchlist(
        profileId: String,
        mediaType: String,
        tmdbId: String
    ): Result<Unit> {
        val profileMap = getProfileMap(profileId)
        val key = itemKey(mediaType, tmdbId)
        profileMap.remove(key)
        _watchlistKeys.value = _watchlistKeys.value - key

        return runCatching {
            supabaseApi.removeFromWatchlist(
                profileIdFilter = "eq.$profileId",
                mediaTypeFilter = "eq.$mediaType",
                tmdbIdFilter = "eq.$tmdbId"
            )
        }
    }

    fun isItemInWatchlist(mediaType: String, tmdbId: String): Boolean {
        return _watchlistKeys.value.contains(itemKey(mediaType, tmdbId))
    }
}
