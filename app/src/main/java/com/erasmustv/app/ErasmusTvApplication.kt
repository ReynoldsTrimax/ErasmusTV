package com.erasmustv.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

/**
 * ErasmusTV Application class.
 * Initializes optimized image caching for high-definition TV backdrops and posters.
 */
class ErasmusTvApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Use 25% of available app memory for TV poster caching
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // 250MB disk cache for TV backdrops and artwork
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            // TMDB serves many title logos as SVG; without this decoder those
            // requests fail and the UI falls back to plain text.
            .components { add(coil.decode.SvgDecoder.Factory()) }
            // Logging is wired up only in debug builds. The logger formats a
            // message for *every* image request and Coil's disk/memory cache
            // hits are requests too, so on a scrolling poster wall this was
            // string-building work on the main thread in shipped builds.
            .apply { if (BuildConfig.DEBUG) logger(DebugLogger()) }
            .okHttpClient { com.erasmustv.app.core.network.NetworkClient.createOkHttpClient() }
            .build()
    }

    companion object {
        lateinit var instance: ErasmusTvApplication
            private set
    }
}
