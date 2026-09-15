package com.erasmustv.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.STREAM_SERVERS
import com.erasmustv.app.data.model.StreamServer
import com.erasmustv.app.data.model.SubtitleTrack
import com.erasmustv.app.data.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerStreamState {
    data object Resolving : PlayerStreamState
    data class Ready(
        val streamUrl: String,
        val serverName: String,
        val startPositionMs: Long,
        val referer: String,
        val captions: List<SubtitleTrack>
    ) : PlayerStreamState
    data class Error(val message: String) : PlayerStreamState
}

class TvPlayerViewModel(
    val mediaType: String,
    val tmdbId: String,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val logoPath: String? = null,
    private val streamRepository: StreamRepository,
    private val profileManager: ProfileManager,
    private val okHttpClient: okhttp3.OkHttpClient? = null
) : ViewModel() {

    private val _activeSubtitleCues = MutableStateFlow<List<com.erasmustv.app.data.subtitle.SubtitleCue>>(emptyList())
    val activeSubtitleCues: StateFlow<List<com.erasmustv.app.data.subtitle.SubtitleCue>> = _activeSubtitleCues.asStateFlow()

    private val _referenceSubtitleCues = MutableStateFlow<List<com.erasmustv.app.data.subtitle.SubtitleCue>?>(null)
    val referenceSubtitleCues: StateFlow<List<com.erasmustv.app.data.subtitle.SubtitleCue>?> = _referenceSubtitleCues.asStateFlow()

    private val subtitleTextCache = java.util.concurrent.ConcurrentHashMap<String, List<com.erasmustv.app.data.subtitle.SubtitleCue>>()

    private val subtitleClient: okhttp3.OkHttpClient = okHttpClient?.newBuilder()
        ?.followRedirects(true)
        ?.followSslRedirects(true)
        ?.connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        ?.readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        ?.build() ?: okhttp3.OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()


    private val _streamState = MutableStateFlow<PlayerStreamState>(PlayerStreamState.Resolving)
    val streamState: StateFlow<PlayerStreamState> = _streamState.asStateFlow()

    private val _currentServerId = MutableStateFlow("lisbon")
    val currentServerId: StateFlow<String> = _currentServerId.asStateFlow()

    val availableServers: List<StreamServer> = STREAM_SERVERS

    init {
        resolveStream("lisbon")
    }

    fun selectServer(serverId: String) {
        _currentServerId.value = serverId
        resolveStream(serverId)
    }

    fun retry() {
        resolveStream(_currentServerId.value)
    }

    private fun resolveStream(serverId: String) {
        viewModelScope.launch {
            _streamState.value = PlayerStreamState.Resolving

            val profile = profileManager.getActiveProfile()
            val profileId = profile?.id ?: "default-profile"
            val resumeSeconds = streamRepository.getResumePosition(
                profileId, mediaType, tmdbId, season, episode
            )
            val startPositionMs = resumeSeconds * 1000L

            streamRepository.extractStream(
                mediaType = mediaType,
                tmdbId = tmdbId,
                server = serverId,
                title = title,
                season = season,
                episode = episode
            ).fold(
                onSuccess = { result ->
                    val server = result.servers.firstOrNull()
                    if (result.ok && server != null && server.url.isNotBlank()) {
                        val subTracks = result.captions.map {
                            SubtitleTrack(label = it.label, language = it.language, url = it.url, mimeType = it.mimeType)
                        }
                        _streamState.value = PlayerStreamState.Ready(
                            streamUrl = server.url,
                            serverName = server.name,
                            startPositionMs = startPositionMs,
                            referer = result.referer ?: "https://cinejoy.to/",
                            captions = subTracks
                        )
                        // Select primary English caption track by default
                        val primaryTrack = subTracks.firstOrNull {
                            it.language.equals("en", ignoreCase = true) ||
                                    it.language.equals("eng", ignoreCase = true) ||
                                    it.language.startsWith("en-", ignoreCase = true) ||
                                    it.label.contains("English", ignoreCase = true)
                        } ?: subTracks.firstOrNull()
                        if (primaryTrack != null) {
                            loadSubtitleTrack(primaryTrack)
                        }
                    } else {
                        val errorMsg = result.error ?: "Stream resolution failed for $title across cluster servers."
                        _streamState.value = PlayerStreamState.Error(errorMsg)
                    }
                },
                onFailure = { error ->
                    _streamState.value = PlayerStreamState.Error(
                        error.message ?: "Failed to connect to stream cluster. Please select another server."
                    )
                }
            )
        }
    }

    fun loadSubtitleTrack(track: SubtitleTrack?) {
        if (track == null || track.url.isBlank()) {
            _activeSubtitleCues.value = emptyList()
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cached = subtitleTextCache[track.url]
            if (cached != null) {
                _activeSubtitleCues.value = cached
                return@launch
            }

            try {
                val referer = when {
                    track.url.contains("vidfast", ignoreCase = true) || track.url.contains("wyzie", ignoreCase = true) -> "https://vidfast.vc/"
                    track.url.contains("cinejoy", ignoreCase = true) -> "https://cinejoy.to/"
                    track.url.contains("strem.io", ignoreCase = true) -> "https://opensubtitles-v3.strem.io/"
                    else -> "https://cinejoy.to/"
                }
                val req = okhttp3.Request.Builder()
                    .url(track.url)
                    .header("User-Agent", com.erasmustv.app.core.config.AppConfig.STREAM_USER_AGENT)
                    .header("Referer", referer)
                    .header("Accept", "*/*")
                    .build()
                val resp = subtitleClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    android.util.Log.w("TvPlayerViewModel", "Subtitle fetch failed with HTTP ${resp.code} for ${track.url}")
                    return@launch
                }
                val body = resp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val parsed = com.erasmustv.app.data.subtitle.SubtitleParser.parse(body)
                    android.util.Log.d("TvPlayerViewModel", "Successfully parsed ${parsed.size} cues for ${track.label} (${track.language}) from ${track.url}")
                    subtitleTextCache[track.url] = parsed
                    _activeSubtitleCues.value = parsed
                }
            } catch (e: Exception) {
                android.util.Log.w("TvPlayerViewModel", "Failed to fetch subtitle text: ${e.message}")
            }
        }
    }

    private fun loadReferenceTrack(track: SubtitleTrack) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cached = subtitleTextCache[track.url]
            if (cached != null) {
                _referenceSubtitleCues.value = cached
                return@launch
            }

            try {
                val referer = when {
                    track.url.contains("vidfast", ignoreCase = true) || track.url.contains("wyzie", ignoreCase = true) -> "https://vidfast.vc/"
                    track.url.contains("cinejoy", ignoreCase = true) -> "https://cinejoy.to/"
                    track.url.contains("strem.io", ignoreCase = true) -> "https://opensubtitles-v3.strem.io/"
                    else -> "https://cinejoy.to/"
                }
                val req = okhttp3.Request.Builder()
                    .url(track.url)
                    .header("User-Agent", com.erasmustv.app.core.config.AppConfig.STREAM_USER_AGENT)
                    .header("Referer", referer)
                    .header("Accept", "*/*")
                    .build()
                val resp = subtitleClient.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val parsed = com.erasmustv.app.data.subtitle.SubtitleParser.parse(body)
                    subtitleTextCache[track.url] = parsed
                    _referenceSubtitleCues.value = parsed
                }
            } catch (e: Exception) {
                android.util.Log.w("TvPlayerViewModel", "Failed to fetch reference subtitle: ${e.message}")
            }
        }
    }

    fun onPlaybackError(message: String) {
        _streamState.value = PlayerStreamState.Error(message)
    }

    fun persistProgress(currentSeconds: Long, durationSeconds: Long?) {
        viewModelScope.launch {
            val profile = profileManager.getActiveProfile() ?: return@launch
            streamRepository.savePlaybackProgress(
                profileId = profile.id,
                mediaType = mediaType,
                tmdbId = tmdbId,
                title = title,
                posterPath = posterPath,
                backdropPath = backdropPath,
                seconds = currentSeconds,
                duration = durationSeconds,
                season = season,
                episode = episode,
                logoPath = logoPath
            )
        }
    }
}

