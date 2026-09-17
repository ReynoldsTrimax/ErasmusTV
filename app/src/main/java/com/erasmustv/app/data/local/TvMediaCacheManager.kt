package com.erasmustv.app.data.local

import android.content.Context
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * High-performance, storage-safe media cache manager for ErasmusTV.
 *
 * Provides a managed Media3 [SimpleCache] with an LRU eviction quota (2.0 GB)
 * stored in the app's cache directory so TV devices never suffer from memory exhaustion
 * (OOM) while buffering up to 30 minutes of 4K/1080p stream content ahead.
 *
 * Automatically evicts cache on movie completion or player teardown.
 */
object TvMediaCacheManager {
    private const val TAG = "TvMediaCacheManager"
    private const val CACHE_DIR_NAME = "tv_media_cache"
    private const val MAX_CACHE_BYTES = 2L * 1024 * 1024 * 1024 // 2.0 GB LRU quota

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        val existing = simpleCache
        if (existing != null) return existing

        val cacheDir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        val newCache = SimpleCache(cacheDir, evictor, databaseProvider)
        simpleCache = newCache
        Log.i(TAG, "Initialized SimpleCache at ${cacheDir.absolutePath} with max size ${MAX_CACHE_BYTES / (1024 * 1024)} MB")
        return newCache
    }

    /**
     * Creates a [CacheDataSource.Factory] wrapping the given upstream HTTP factory.
     * Uses FLAG_IGNORE_CACHE_ON_ERROR so any temporary cache read/write issue
     * seamlessly falls back to direct network streaming without user interruption.
     */
    fun createCacheDataSourceFactory(
        context: Context,
        upstreamFactory: DataSource.Factory
    ): DataSource.Factory {
        val cache = getCache(context)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Clears all cached media segments asynchronously on an IO thread.
     * Invoked when a movie/episode completes playback or when exiting the player.
     */
    fun clearCacheAsync(context: Context, onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                val cache = getCache(context)
                val keys = cache.keys.toList()
                Log.d(TAG, "Clearing media cache across ${keys.size} cached resources...")
                for (key in keys) {
                    try {
                        cache.removeResource(key)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed removing cached key $key: ${e.message}")
                    }
                }
                Log.d(TAG, "Media cache cleanup complete.")
            } catch (e: Exception) {
                Log.w(TAG, "Error during media cache cleanup: ${e.message}")
            } finally {
                onComplete?.invoke()
            }
        }
    }
}
