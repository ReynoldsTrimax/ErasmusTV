package com.erasmustv.app.ui.screens.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TvPlayerFrameRetriever(
    private val streamUrl: String,
    private val referer: String = "https://cinejoy.to/"
) {
    companion object {
        private const val TAG = "TvFrameRetriever"
        private const val CACHE_SIZE = 50 // Store up to 50 preview frames in memory
        private const val FRAME_WIDTH = 320
        private const val FRAME_HEIGHT = 180
    }

    private val frameCache = LruCache<Long, Bitmap>(CACHE_SIZE)
    private var retriever: MediaMetadataRetriever? = null
    private var isInitialized = false
    private var initFailed = false

    private fun ensureRetriever() {
        if (isInitialized || initFailed) return
        try {
            val headers = mapOf(
                "Referer" to referer,
                "Origin" to "https://cinejoy.to",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
            )
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(streamUrl, headers)
            retriever = mmr
            isInitialized = true
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize MediaMetadataRetriever for $streamUrl: ${e.message}")
            initFailed = true
        }
    }

    suspend fun getFrameAt(timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        val normalizedKey = (timeMs / 5000L) * 5000L // 5s bucket for caching
        frameCache.get(normalizedKey)?.let { return@withContext it }

        ensureRetriever()
        val mmr = retriever ?: return@withContext null

        try {
            val timeUs = timeMs * 1000L
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                mmr.getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    FRAME_WIDTH,
                    FRAME_HEIGHT
                )
            } else {
                mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { original ->
                    Bitmap.createScaledBitmap(original, FRAME_WIDTH, FRAME_HEIGHT, true)
                }
            }

            if (bitmap != null) {
                frameCache.put(normalizedKey, bitmap)
            }
            bitmap
        } catch (e: Throwable) {
            Log.d(TAG, "Frame extraction failed at ${timeMs}ms: ${e.message}")
            null
        }
    }

    fun release() {
        try {
            retriever?.release()
            retriever = null
            frameCache.evictAll()
        } catch (_: Throwable) {}
    }
}
