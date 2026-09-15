package com.erasmustv.app.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.Genre
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.MediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MoviesData(
    val heroMovie: MediaItem? = null,
    val popular: List<MediaItem> = emptyList(),
    val nowPlaying: List<MediaItem> = emptyList(),
    val topRated: List<MediaItem> = emptyList(),
    val upcoming: List<MediaItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val activeProfile: WatchProfile? = null
)

sealed interface MoviesUiState {
    data object Loading : MoviesUiState
    data class Success(val data: MoviesData) : MoviesUiState
    data class Error(val message: String) : MoviesUiState
}

class MoviesViewModel(
    private val mediaRepository: MediaRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoviesUiState>(MoviesUiState.Loading)
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        loadMoviesFeed()
    }

    fun loadMoviesFeed() {
        viewModelScope.launch {
            _uiState.value = MoviesUiState.Loading
            try {
                val activeProfile = profileManager.getActiveProfile()

                val popularDeferred = async { mediaRepository.getPopularMovies().getOrDefault(emptyList()) }
                val nowPlayingDeferred = async { mediaRepository.getNowPlayingMovies().getOrDefault(emptyList()) }
                val topRatedDeferred = async { mediaRepository.getTopRatedMovies().getOrDefault(emptyList()) }
                val upcomingDeferred = async { mediaRepository.getUpcomingMovies().getOrDefault(emptyList()) }
                val genresDeferred = async { mediaRepository.getMovieGenres().getOrDefault(emptyList()) }

                val popular = popularDeferred.await()
                val nowPlaying = nowPlayingDeferred.await()
                val topRated = topRatedDeferred.await()
                val upcoming = upcomingDeferred.await()
                val genres = genresDeferred.await()

                val hero = popular.firstOrNull()?.let {
                    val logo = mediaRepository.getMediaLogo(it.mediaType, it.id).getOrNull()
                    it.copy(logoPath = logo)
                }

                _uiState.value = MoviesUiState.Success(
                    MoviesData(
                        heroMovie = hero,
                        popular = popular,
                        nowPlaying = nowPlaying,
                        topRated = topRated,
                        upcoming = upcoming,
                        genres = genres,
                        activeProfile = activeProfile
                    )
                )
            } catch (e: Exception) {
                _uiState.value = MoviesUiState.Error(e.message ?: "Failed to load movies")
            }
        }
    }
}
