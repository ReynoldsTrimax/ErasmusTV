package com.erasmustv.app.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WatchlistUiState {
    data object Loading : WatchlistUiState
    data class Success(val items: List<MediaItem>, val activeProfile: WatchProfile?) : WatchlistUiState
    data class Error(val message: String) : WatchlistUiState
}

class WatchlistViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchlistUiState>(WatchlistUiState.Loading)
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    // NOTE: the initial load is driven by WatchlistScreen's LaunchedEffect(Unit),
    // which also re-reads on every screen entry (so a title removed from a detail
    // page disappears on return). Loading here in init as well would fire a second,
    // redundant Supabase getWatchlist on first open — so it is intentionally omitted.

    fun loadWatchlist() {
        viewModelScope.launch {
            _uiState.value = WatchlistUiState.Loading
            val profile = profileManager.getActiveProfile()
            val profileId = profile?.id ?: "default-profile"

            watchlistRepository.getWatchlist(profileId).fold(
                onSuccess = { items ->
                    _uiState.value = WatchlistUiState.Success(items, profile)
                },
                onFailure = { error ->
                    _uiState.value = WatchlistUiState.Error(error.message ?: "Failed to load watchlist")
                }
            )
        }
    }
}
