package com.erasmustv.app.ui.screens.studios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StudiosUiState {
    object Hub : StudiosUiState
    object Loading : StudiosUiState
    data class StudioCatalog(
        val studio: StudioInfo,
        val trendingItems: List<MediaItem>,
        val movieItems: List<MediaItem>,
        val tvItems: List<MediaItem>,
        val isShowingMovies: Boolean = true
    ) : StudiosUiState
}

data class StudioInfo(
    val id: String,
    val name: String,
    val brandText: String,
    val queryTerm: String,
    val providerId: Int = 8
)

class StudiosViewModel(
    private val mediaRepository: MediaRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudiosUiState>(StudiosUiState.Hub)
    val uiState: StateFlow<StudiosUiState> = _uiState.asStateFlow()

    private val _activeProfile = MutableStateFlow<WatchProfile?>(null)
    val activeProfile: StateFlow<WatchProfile?> = _activeProfile.asStateFlow()

    var lastSelectedStudioId: String = "netflix"
        private set

    init {
        viewModelScope.launch {
            _activeProfile.value = profileManager.getActiveProfile()
        }
    }

    fun selectStudio(studio: StudioInfo) {
        lastSelectedStudioId = studio.id
        viewModelScope.launch {
            _uiState.value = StudiosUiState.Loading
            val studioResult = mediaRepository.getStudioContent(studio.providerId).getOrNull()
            val movies = studioResult?.first ?: emptyList()
            val series = studioResult?.second ?: emptyList()

            // Fallback to name search or popular if provider content is empty
            val finalMovies = if (movies.isNotEmpty()) movies else {
                val searchResults = mediaRepository.search(studio.queryTerm).getOrDefault(emptyList()).filter { !it.isTv }
                if (searchResults.isNotEmpty()) searchResults else mediaRepository.getPopularMovies().getOrDefault(emptyList())
            }

            val finalSeries = if (series.isNotEmpty()) series else {
                val searchResults = mediaRepository.search(studio.queryTerm).getOrDefault(emptyList()).filter { it.isTv }
                if (searchResults.isNotEmpty()) searchResults else mediaRepository.getPopularTv().getOrDefault(emptyList())
            }

            val combinedTrending = (finalMovies.take(6) + finalSeries.take(6)).shuffled()

            _uiState.value = StudiosUiState.StudioCatalog(
                studio = studio,
                trendingItems = combinedTrending,
                movieItems = finalMovies,
                tvItems = finalSeries,
                isShowingMovies = true
            )
        }
    }

    fun toggleFilter(showMovies: Boolean) {
        val current = _uiState.value
        if (current is StudiosUiState.StudioCatalog) {
            _uiState.value = current.copy(isShowingMovies = showMovies)
        }
    }

    fun backToHub() {
        _uiState.value = StudiosUiState.Hub
    }
}
