package com.erasmustv.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchCategory(
    val name: String,
    val movieGenreId: Int?,
    val tvGenreId: Int?
)

sealed interface SearchUiState {
    data object Empty : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val query: String,
        val results: List<MediaItem>,
        val trendingSuggestions: List<MediaItem>,
        val isGenreCategory: Boolean = false
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val mediaRepository: MediaRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    val activeProfile: StateFlow<WatchProfile?> = profileManager.activeProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Empty)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow<SearchCategory?>(null)
    val selectedCategory: StateFlow<SearchCategory?> = _selectedCategory.asStateFlow()

    private var searchJob: Job? = null
    private var trendingCache = emptyList<MediaItem>()

    init {
        loadTrendingSuggestions()
    }

    private fun loadTrendingSuggestions() {
        viewModelScope.launch {
            mediaRepository.getTrending().onSuccess {
                trendingCache = it
                if (_query.value.isBlank() && _selectedCategory.value == null) {
                    _uiState.value = SearchUiState.Success("", emptyList(), trendingCache)
                }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _selectedCategory.value = null
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _uiState.value = SearchUiState.Success("", emptyList(), trendingCache)
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce typing
            _uiState.value = SearchUiState.Loading
            mediaRepository.search(newQuery).fold(
                onSuccess = { results ->
                    _uiState.value = SearchUiState.Success(newQuery, results, trendingCache, isGenreCategory = false)
                },
                onFailure = { error ->
                    _uiState.value = SearchUiState.Error(error.message ?: "Search failed")
                }
            )
        }
    }

    fun selectCategory(category: SearchCategory) {
        _selectedCategory.value = category
        _query.value = ""
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            mediaRepository.discoverByGenre(category.movieGenreId, category.tvGenreId).fold(
                onSuccess = { results ->
                    _uiState.value = SearchUiState.Success(category.name, results, trendingCache, isGenreCategory = true)
                },
                onFailure = { error ->
                    _uiState.value = SearchUiState.Error(error.message ?: "Genre discover failed")
                }
            )
        }
    }

    suspend fun getActiveProfile(): WatchProfile? = profileManager.getActiveProfile()
}
