package com.erasmustv.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.ContinueWatchingItem
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.MediaRepository
import com.erasmustv.app.data.repository.StreamRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val heroItem: MediaItem? = null,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val trending: List<MediaItem> = emptyList(),
    val popularMovies: List<MediaItem> = emptyList(),
    val popularTv: List<MediaItem> = emptyList(),
    val topRated: List<MediaItem> = emptyList(),
    val activeProfile: WatchProfile? = null
)

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val streamRepository: StreamRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val activeProfile = profileManager.getActiveProfile()
                val profileId = activeProfile?.id ?: "default-profile"

                val rawContinueWatching = streamRepository.listContinueWatching(profileId)

                val trendingDeferred = async { mediaRepository.getTrending().getOrDefault(emptyList()) }
                val popularMoviesDeferred = async { mediaRepository.getPopularMovies().getOrDefault(emptyList()) }
                val popularTvDeferred = async { mediaRepository.getPopularTv().getOrDefault(emptyList()) }
                val topRatedDeferred = async { mediaRepository.getTopRatedMovies().getOrDefault(emptyList()) }

                val trending = trendingDeferred.await()
                val popularMovies = popularMoviesDeferred.await()
                val popularTv = popularTvDeferred.await()
                val topRated = topRatedDeferred.await()

                // Hydrate Continue Watching artwork & logos
                val continueWatching = rawContinueWatching.map { item ->
                    var backdrop = item.backdropPath
                    var poster = item.posterPath
                    var logo = item.logoPath

                    if (backdrop.isNullOrBlank() && poster.isNullOrBlank()) {
                        val foundMedia = (trending + popularMovies + popularTv + topRated).firstOrNull {
                            it.id == item.tmdbId && (it.mediaType == item.mediaType || it.mediaType.isBlank())
                        }
                        backdrop = foundMedia?.backdropPath
                        poster = foundMedia?.posterPath

                        if (backdrop == null && poster == null) {
                            try {
                                if (item.mediaType.equals("tv", ignoreCase = true)) {
                                    val tvDetails = mediaRepository.getTvDetails(item.tmdbId).getOrNull()
                                    backdrop = tvDetails?.backdropPath
                                    poster = tvDetails?.posterPath
                                    if (logo == null) logo = tvDetails?.logoPath
                                } else {
                                    val movieDetails = mediaRepository.getMovieDetails(item.tmdbId).getOrNull()
                                    backdrop = movieDetails?.backdropPath
                                    poster = movieDetails?.posterPath
                                    if (logo == null) logo = movieDetails?.logoPath
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    if (logo.isNullOrBlank()) {
                        logo = mediaRepository.getMediaLogo(item.mediaType, item.tmdbId).getOrNull()
                    }

                    if (backdrop != item.backdropPath || poster != item.posterPath || logo != item.logoPath) {
                        streamRepository.savePlaybackProgress(
                            profileId = profileId,
                            mediaType = item.mediaType,
                            tmdbId = item.tmdbId,
                            title = item.title,
                            posterPath = poster ?: item.posterPath,
                            backdropPath = backdrop ?: item.backdropPath,
                            seconds = item.seconds,
                            duration = item.duration,
                            season = item.season,
                            episode = item.episode,
                            logoPath = logo ?: item.logoPath
                        )
                    }

                    item.copy(
                        backdropPath = backdrop ?: item.backdropPath,
                        posterPath = poster ?: item.posterPath,
                        logoPath = logo ?: item.logoPath
                    )
                }

                // Hydrate featured Hero billboard items with logos
                val trendingWithLogos = trending.mapIndexed { index, mediaItem ->
                    if (index < 5 && mediaItem.logoPath == null) {
                        val logo = mediaRepository.getMediaLogo(mediaItem.mediaType, mediaItem.id).getOrNull()
                        mediaItem.copy(logoPath = logo)
                    } else {
                        mediaItem
                    }
                }

                val heroItem = trendingWithLogos.firstOrNull() ?: popularMovies.firstOrNull()?.let {
                    val logo = mediaRepository.getMediaLogo(it.mediaType, it.id).getOrNull()
                    it.copy(logoPath = logo)
                }

                _uiState.value = HomeUiState.Success(
                    HomeData(
                        heroItem = heroItem,
                        continueWatching = continueWatching,
                        trending = trendingWithLogos,
                        popularMovies = popularMovies,
                        popularTv = popularTv,
                        topRated = topRated,
                        activeProfile = activeProfile
                    )
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load home content")
            }
        }
    }
}
