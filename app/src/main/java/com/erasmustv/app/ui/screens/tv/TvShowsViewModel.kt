package com.erasmustv.app.ui.screens.tv

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

data class TvShowsData(
    val heroShow: MediaItem? = null,
    val popular: List<MediaItem> = emptyList(),
    val topRated: List<MediaItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val activeProfile: WatchProfile? = null
)

sealed interface TvShowsUiState {
    data object Loading : TvShowsUiState
    data class Success(val data: TvShowsData) : TvShowsUiState
    data class Error(val message: String) : TvShowsUiState
}

class TvShowsViewModel(
    private val mediaRepository: MediaRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvShowsUiState>(TvShowsUiState.Loading)
    val uiState: StateFlow<TvShowsUiState> = _uiState.asStateFlow()

    init {
        loadTvFeed()
    }

    fun loadTvFeed() {
        viewModelScope.launch {
            _uiState.value = TvShowsUiState.Loading
            try {
                val activeProfile = profileManager.getActiveProfile()

                val popularDeferred = async { mediaRepository.getPopularTv().getOrDefault(emptyList()) }
                val topRatedDeferred = async { mediaRepository.getTopRatedTv().getOrDefault(emptyList()) }
                val genresDeferred = async { mediaRepository.getTvGenres().getOrDefault(emptyList()) }

                val popular = popularDeferred.await()
                val topRated = topRatedDeferred.await()
                val genres = genresDeferred.await()

                val hero = popular.firstOrNull()?.let {
                    val logo = mediaRepository.getMediaLogo(it.mediaType, it.id).getOrNull()
                    it.copy(logoPath = logo)
                }

                _uiState.value = TvShowsUiState.Success(
                    TvShowsData(
                        heroShow = hero,
                        popular = popular,
                        topRated = topRated,
                        genres = genres,
                        activeProfile = activeProfile
                    )
                )
            } catch (e: Exception) {
                _uiState.value = TvShowsUiState.Error(e.message ?: "Failed to load TV shows")
            }
        }
    }
}
