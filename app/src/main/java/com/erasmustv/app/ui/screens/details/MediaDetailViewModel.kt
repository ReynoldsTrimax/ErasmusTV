package com.erasmustv.app.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.MovieDetails
import com.erasmustv.app.data.model.TvDetails
import com.erasmustv.app.data.model.TvSeason
import com.erasmustv.app.data.repository.MediaRepository
import com.erasmustv.app.data.repository.StreamRepository
import com.erasmustv.app.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class MovieSuccess(
        val movie: MovieDetails,
        val resumePosition: Long,
        val inWatchlist: Boolean
    ) : DetailUiState
    data class TvSuccess(
        val tv: TvDetails,
        val selectedSeason: TvSeason?,
        val resumePosition: Long,
        val inWatchlist: Boolean
    ) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class MediaDetailViewModel(
    private val mediaType: String,
    private val tmdbId: String,
    private val mediaRepository: MediaRepository,
    private val streamRepository: StreamRepository,
    private val watchlistRepository: WatchlistRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _activeProfile = MutableStateFlow<com.erasmustv.app.data.model.WatchProfile?>(null)
    val activeProfile: StateFlow<com.erasmustv.app.data.model.WatchProfile?> = _activeProfile.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            val profile = profileManager.getActiveProfile()
            _activeProfile.value = profile
            val profileId = profile?.id ?: "default-profile"

            val inWatchlist = watchlistRepository.isItemInWatchlist(mediaType, tmdbId)
            val resumeSeconds = streamRepository.getResumePosition(profileId, mediaType, tmdbId)

            if (mediaType.equals("tv", ignoreCase = true)) {
                mediaRepository.getTvDetails(tmdbId).fold(
                    onSuccess = { tv ->
                        val firstSeasonNumber = tv.seasons.firstOrNull { it.seasonNumber > 0 }?.seasonNumber ?: 1
                        val season = mediaRepository.getTvSeason(tmdbId, firstSeasonNumber).getOrNull()
                        _uiState.value = DetailUiState.TvSuccess(
                            tv = tv,
                            selectedSeason = season,
                            resumePosition = resumeSeconds,
                            inWatchlist = inWatchlist
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = DetailUiState.Error(error.message ?: "Failed to load TV details")
                    }
                )
            } else {
                mediaRepository.getMovieDetails(tmdbId).fold(
                    onSuccess = { movie ->
                        _uiState.value = DetailUiState.MovieSuccess(
                            movie = movie,
                            resumePosition = resumeSeconds,
                            inWatchlist = inWatchlist
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = DetailUiState.Error(error.message ?: "Failed to load movie details")
                    }
                )
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val current = _uiState.value
        if (current !is DetailUiState.TvSuccess) return

        viewModelScope.launch {
            mediaRepository.getTvSeason(tmdbId, seasonNumber).onSuccess { season ->
                _uiState.value = current.copy(selectedSeason = season)
            }
        }
    }

    fun toggleWatchlist() {
        val current = _uiState.value
        viewModelScope.launch {
            val profile = profileManager.getActiveProfile() ?: return@launch
            when (current) {
                is DetailUiState.MovieSuccess -> {
                    val m = current.movie
                    if (current.inWatchlist) {
                        watchlistRepository.removeFromWatchlist(profile.id, "movie", m.id)
                        _uiState.value = current.copy(inWatchlist = false)
                    } else {
                        watchlistRepository.addToWatchlist(
                            profile.id,
                            MediaItem(m.id, m.title, m.posterPath, m.backdropPath, "movie", m.voteAverage, m.releaseDate)
                        )
                        _uiState.value = current.copy(inWatchlist = true)
                    }
                }
                is DetailUiState.TvSuccess -> {
                    val t = current.tv
                    if (current.inWatchlist) {
                        watchlistRepository.removeFromWatchlist(profile.id, "tv", t.id)
                        _uiState.value = current.copy(inWatchlist = false)
                    } else {
                        watchlistRepository.addToWatchlist(
                            profile.id,
                            MediaItem(t.id, t.title, t.posterPath, t.backdropPath, "tv", t.voteAverage, t.firstAirDate)
                        )
                        _uiState.value = current.copy(inWatchlist = true)
                    }
                }
                else -> Unit
            }
        }
    }
}
