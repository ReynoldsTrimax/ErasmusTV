package com.erasmustv.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.model.MediaItem
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.data.repository.MediaRepository
import com.erasmustv.app.ui.navigation.NavRoutes
import com.erasmustv.app.ui.screens.studios.STUDIOS_DATA
import com.erasmustv.app.ui.screens.studios.StudioInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseCategoryItem(
    val id: String,
    val title: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val route: String? = null
)

data class LanguageCategoryItem(
    val id: String,
    val localTitle: String,
    val englishTitle: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val characterSubtitle: String,
    val iso639: String
)

sealed interface CategoriesUiState {
    data class Overview(
        val browseItems: List<BrowseCategoryItem>,
        val studios: List<StudioInfo>,
        val languages: List<LanguageCategoryItem>
    ) : CategoriesUiState

    data object Loading : CategoriesUiState

    data class StudioCatalog(
        val studio: StudioInfo,
        val trendingItems: List<MediaItem>,
        val movieItems: List<MediaItem>,
        val tvItems: List<MediaItem>,
        val isShowingMovies: Boolean = true
    ) : CategoriesUiState
}

val BROWSE_CATEGORIES = listOf(
    BrowseCategoryItem("anime", "Anime", 0xFF19163A, 0xFF35276E, NavRoutes.ANIME),
    BrowseCategoryItem("tv", "TV", 0xFF142036, 0xFF243657, NavRoutes.TV),
    BrowseCategoryItem("movies", "Movies", 0xFF28133E, 0xFF471C6D, NavRoutes.MOVIES),
    BrowseCategoryItem("news", "News", 0xFF30132E, 0xFF581A4E, null)
)

val LANGUAGE_CATEGORIES = listOf(
    LanguageCategoryItem("en", "English", "English", 0xFF7D3B31, 0xFF4A1F18, "Scarlett Johansson", "en"),
    LanguageCategoryItem("ja", "日本", "Japanese", 0xFF635E42, 0xFF3A3622, "Tanjiro Kamado", "ja"),
    LanguageCategoryItem("ko", "한국어", "Korean", 0xFF2B3A50, 0xFF172233, "Korean Cinema", "ko"),
    LanguageCategoryItem("hi", "हिन्दी", "Hindi", 0xFF403554, 0xFF241C33, "Ranbir Kapoor", "hi"),
    LanguageCategoryItem("pt", "Português", "Portuguese", 0xFF1F523D, 0xFF0E2E20, "Cristiano Ronaldo", "pt"),
    LanguageCategoryItem("es", "Español", "Spanish", 0xFF752020, 0xFF421010, "Spanish Cinema", "es")
)

class CategoriesViewModel(
    private val mediaRepository: MediaRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(
        CategoriesUiState.Overview(
            browseItems = BROWSE_CATEGORIES,
            studios = STUDIOS_DATA,
            languages = LANGUAGE_CATEGORIES
        )
    )
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private val _activeProfile = MutableStateFlow<WatchProfile?>(null)
    val activeProfile: StateFlow<WatchProfile?> = _activeProfile.asStateFlow()

    init {
        viewModelScope.launch {
            _activeProfile.value = profileManager.getActiveProfile()
        }
    }

    fun selectStudio(studio: StudioInfo) {
        viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading
            val studioResult = mediaRepository.getStudioContent(studio.providerId).getOrNull()
            val movies = studioResult?.first ?: emptyList()
            val series = studioResult?.second ?: emptyList()

            val finalMovies = if (movies.isNotEmpty()) movies else {
                val searchResults = mediaRepository.search(studio.queryTerm).getOrDefault(emptyList()).filter { !it.isTv }
                if (searchResults.isNotEmpty()) searchResults else mediaRepository.getPopularMovies().getOrDefault(emptyList())
            }

            val finalSeries = if (series.isNotEmpty()) series else {
                val searchResults = mediaRepository.search(studio.queryTerm).getOrDefault(emptyList()).filter { it.isTv }
                if (searchResults.isNotEmpty()) searchResults else mediaRepository.getPopularTv().getOrDefault(emptyList())
            }

            val combinedTrending = (finalMovies.take(6) + finalSeries.take(6)).shuffled()

            _uiState.value = CategoriesUiState.StudioCatalog(
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
        if (current is CategoriesUiState.StudioCatalog) {
            _uiState.value = current.copy(isShowingMovies = showMovies)
        }
    }

    fun backToOverview() {
        _uiState.value = CategoriesUiState.Overview(
            browseItems = BROWSE_CATEGORIES,
            studios = STUDIOS_DATA,
            languages = LANGUAGE_CATEGORIES
        )
    }
}
