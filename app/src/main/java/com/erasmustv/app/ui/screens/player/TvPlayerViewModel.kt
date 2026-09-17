package com.erasmustv.app.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.PlaybackProgress
import com.erasmustv.app.data.model.STREAM_SERVERS
import com.erasmustv.app.data.model.StreamServer
import com.erasmustv.app.data.model.SubtitleTrack
import com.erasmustv.app.data.model.TvEpisode
import com.erasmustv.app.data.model.TvSeason
import com.erasmustv.app.data.repository.MediaRepository
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
    private val mediaRepository: MediaRepository? = null,
    private val okHttpClient: okhttp3.OkHttpClient? = null
) : ViewModel() {

    private var cachedProfileId: String = "default-profile"

    private val _currentSeason = MutableStateFlow(season ?: 1)
    val currentSeason: StateFlow<Int> = _currentSeason.asStateFlow()

    private val _currentEpisode = MutableStateFlow(episode ?: 1)
    val currentEpisode: StateFlow<Int> = _currentEpisode.asStateFlow()

    private val _currentEpisodeTitle = MutableStateFlow<String?>(null)
    val currentEpisodeTitle: StateFlow<String?> = _currentEpisodeTitle.asStateFlow()

    private val _showSeasons = MutableStateFlow<List<TvSeason>>(emptyList())
    val showSeasons: StateFlow<List<TvSeason>> = _showSeasons.asStateFlow()

    private val _currentSeasonEpisodes = MutableStateFlow<List<TvEpisode>>(emptyList())
    val currentSeasonEpisodes: StateFlow<List<TvEpisode>> = _currentSeasonEpisodes.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

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
        viewModelScope.launch {
            cachedProfileId = profileManager.getActiveProfile()?.id ?: "default-profile"
        }
        resolveStream("lisbon")
        if (mediaType.equals("tv", ignoreCase = true)) {
            loadEpisodesForSeason(_currentSeason.value)
        }
    }

    fun loadEpisodesForSeason(seasonNumber: Int) {
        if (mediaRepository == null) return
        viewModelScope.launch {
            _isLoadingEpisodes.value = true
            if (_showSeasons.value.isEmpty()) {
                mediaRepository.getTvDetails(tmdbId).onSuccess { details ->
                    val regularSeasons = details.seasons.filter { it.seasonNumber > 0 }
                    _showSeasons.value = if (regularSeasons.isNotEmpty()) regularSeasons else details.seasons
                }
            }
            mediaRepository.getTvSeason(tmdbId, seasonNumber).onSuccess { seasonData ->
                _currentSeasonEpisodes.value = seasonData.episodes
                if (seasonNumber == _currentSeason.value) {
                    val ep = seasonData.episodes.find { it.episodeNumber == _currentEpisode.value }
                    if (ep != null) {
                        _currentEpisodeTitle.value = ep.name
                    }
                }
            }
            _isLoadingEpisodes.value = false
        }
    }

    fun switchEpisode(seasonNumber: Int, episodeNumber: Int, episodeTitle: String?) {
        if (_currentSeason.value == seasonNumber && _currentEpisode.value == episodeNumber) return
        _currentSeason.value = seasonNumber
        _currentEpisode.value = episodeNumber
        _currentEpisodeTitle.value = episodeTitle
        loadEpisodesForSeason(seasonNumber)
        resolveStream(_currentServerId.value)
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
            cachedProfileId = profileId
            val targetSeason = if (mediaType.equals("tv", ignoreCase = true)) _currentSeason.value else null
            val targetEpisode = if (mediaType.equals("tv", ignoreCase = true)) _currentEpisode.value else null
            val resumeSeconds = streamRepository.getResumePosition(
                profileId, mediaType, tmdbId, targetSeason, targetEpisode
            )
            val startPositionMs = resumeSeconds * 1000L

            streamRepository.extractStream(
                mediaType = mediaType,
                tmdbId = tmdbId,
                server = serverId,
                title = title,
                season = targetSeason,
                episode = targetEpisode
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
                            referer = result.referer ?: "",
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
                    track.url.contains("hakunaymatata.com", ignoreCase = true) -> null
                    track.url.contains("vidfast", ignoreCase = true) || track.url.contains("wyzie", ignoreCase = true) -> "https://vidfast.vc/"
                    track.url.contains("cinejoy", ignoreCase = true) -> "https://cinejoy.to/"
                    track.url.contains("strem.io", ignoreCase = true) -> "https://opensubtitles-v3.strem.io/"
                    else -> null
                }
                val reqBuilder = okhttp3.Request.Builder()
                    .url(track.url)
                    .header("User-Agent", com.erasmustv.app.core.config.AppConfig.STREAM_USER_AGENT)
                    .header("Accept", "*/*")
                if (!referer.isNullOrBlank()) {
                    reqBuilder.header("Referer", referer)
                }
                val req = reqBuilder.build()
                val resp = subtitleClient.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                if (body.isNotBlank()) {
                    val parsed = com.erasmustv.app.data.subtitle.SubtitleParser.parse(body)
                    subtitleTextCache[track.url] = parsed
                    _activeSubtitleCues.value = parsed

                    // If not already set and this is English, use as reference for sync
                    if (_referenceSubtitleCues.value == null &&
                        (track.language.startsWith("en", ignoreCase = true) || track.label.contains("English", ignoreCase = true))
                    ) {
                        _referenceSubtitleCues.value = parsed
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("TvPlayerVM", "Failed to fetch subtitle track: ${track.url} (${e.message})")
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
                    track.url.contains("hakunaymatata.com", ignoreCase = true) -> null
                    track.url.contains("vidfast", ignoreCase = true) || track.url.contains("wyzie", ignoreCase = true) -> "https://vidfast.vc/"
                    track.url.contains("cinejoy", ignoreCase = true) -> "https://cinejoy.to/"
                    track.url.contains("strem.io", ignoreCase = true) -> "https://opensubtitles-v3.strem.io/"
                    else -> null
                }
                val reqBuilder = okhttp3.Request.Builder()
                    .url(track.url)
                    .header("User-Agent", com.erasmustv.app.core.config.AppConfig.STREAM_USER_AGENT)
                    .header("Accept", "*/*")
                if (!referer.isNullOrBlank()) {
                    reqBuilder.header("Referer", referer)
                }
                val req = reqBuilder.build()
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

    fun getEpisodeProgressRatio(season: Int, episode: Int): Float {
        val progress = streamRepository.getPlaybackProgress(
            profileId = cachedProfileId,
            mediaType = "tv",
            tmdbId = tmdbId,
            season = season,
            episode = episode
        ) ?: return 0f
        return progress.progressRatio
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
                season = if (mediaType.equals("tv", ignoreCase = true)) _currentSeason.value else null,
                episode = if (mediaType.equals("tv", ignoreCase = true)) _currentEpisode.value else null,
                logoPath = logoPath
            )
        }
    }
}

