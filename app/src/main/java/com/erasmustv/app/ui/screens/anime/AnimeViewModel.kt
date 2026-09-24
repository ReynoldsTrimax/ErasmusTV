package com.erasmustv.app.ui.screens.anime

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

data class AnimeFeedData(
    val heroAnime: MediaItem,
    val featuredList: List<MediaItem>,
    val trendingAnime: List<MediaItem>,
    val topRatedAnime: List<MediaItem>,
    val shonenAnime: List<MediaItem>,
    val animeMovies: List<MediaItem>,
    val activeProfile: WatchProfile?
)

sealed interface AnimeUiState {
    data object Loading : AnimeUiState
    data class Success(val data: AnimeFeedData) : AnimeUiState
    data class Error(val message: String) : AnimeUiState
}

// Fallback seed anime items matching the bingr.one/anime reference screenshot
val SEED_ATTACK_ON_TITAN = MediaItem(
    id = "1429",
    title = "Attack on Titan",
    overview = "Several hundred years ago, humans were nearly exterminated by titans. Titans are typically several stories tall, seem to have no intelligence, devour human beings and, worst of all, seem to do it for the pleasure rather than as a food...",
    posterPath = "/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg",
    backdropPath = "/84XPpjGvxNyExjSuLQe0GIO9Nu.jpg",
    mediaType = "tv",
    voteAverage = 8.5,
    releaseDate = "2013-04-07",
    isAnime = true
)

val SEED_DEMON_SLAYER = MediaItem(
    id = "85937",
    title = "Demon Slayer: Kimetsu no Yaiba",
    overview = "It is the Taisho Period in Japan. Tanjiro, a kindhearted boy who sells charcoal for a living, finds his family slaughtered by a demon.",
    posterPath = "/xUfRZu2mi8jH6SzQEJGP6tjBuYj.jpg",
    backdropPath = "/3GQYaGopbrT2VRvgJbPtEWyhV5h.jpg",
    mediaType = "tv",
    voteAverage = 8.7,
    releaseDate = "2019-04-06",
    isAnime = true
)

val SEED_JUJUTSU_KAISEN = MediaItem(
    id = "95479",
    title = "Jujutsu Kaisen",
    overview = "Yuji Itadori is a boy with tremendous physical strength, though he lives a completely ordinary high school life.",
    posterPath = "/hFWqug56iF8H4i96k767T8p04jP.jpg",
    backdropPath = "/9xxLWtnFxr297i9lS996vmrijPf.jpg",
    mediaType = "tv",
    voteAverage = 8.6,
    releaseDate = "2020-10-03",
    isAnime = true
)

val SEED_BLEACH = MediaItem(
    id = "30984",
    title = "Bleach: Thousand-Year Blood War",
    overview = "The peace is suddenly broken when warning sirens resound through the Soul Society.",
    posterPath = "/2EewsolmgvSG9608jFwhf97h9og.jpg",
    backdropPath = "/5DUMPBSnHOZsbBv81GFXZZvv70S.jpg",
    mediaType = "tv",
    voteAverage = 8.4,
    releaseDate = "2022-10-11",
    isAnime = true
)

val SEED_DEATH_NOTE = MediaItem(
    id = "13916",
    title = "Death Note",
    overview = "Light Yagami is a genius high school student who discovers the 'Death Note', a notebook that kills anyone whose name is written in it.",
    posterPath = "/iigTJJskR1PcjjPLi713ioqaA05.jpg",
    backdropPath = "/70YPtE3h1B1hZ34yv1yS4B4t36m.jpg",
    mediaType = "tv",
    voteAverage = 8.6,
    releaseDate = "2006-10-04",
    isAnime = true
)

val SEED_CHAINSAW_MAN = MediaItem(
    id = "114410",
    title = "Chainsaw Man",
    overview = "Denji is a teenage boy living with a Chainsaw Devil named Pochita. Due to the debt his father left behind, he has been living a rock-bottom life.",
    posterPath = "/yVtxvMWII5IkA8vs5R34qQePpdQ.jpg",
    backdropPath = "/y4aH7v3eN88l0pB0fH2K4k61R8.jpg",
    mediaType = "tv",
    voteAverage = 8.4,
    releaseDate = "2022-10-12",
    isAnime = true
)

val SEED_FEATURED_ANIME: List<MediaItem> = listOf(
    SEED_ATTACK_ON_TITAN,
    SEED_DEMON_SLAYER,
    SEED_JUJUTSU_KAISEN,
    SEED_BLEACH,
    SEED_DEATH_NOTE,
    SEED_CHAINSAW_MAN
)

class AnimeViewModel(
    private val mediaRepository: MediaRepository,
    private val profileManager: ProfileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnimeUiState>(AnimeUiState.Loading)
    val uiState: StateFlow<AnimeUiState> = _uiState.asStateFlow()

    init {
        loadAnimeFeed()
    }

    fun loadAnimeFeed() {
        viewModelScope.launch {
            _uiState.value = AnimeUiState.Loading

            val activeProfile = profileManager.getActiveProfile()

            val trendingResult = mediaRepository.getTrendingAnime()
            val topRatedResult = mediaRepository.getTopRatedAnime()
            val shonenResult = mediaRepository.getShonenAnime()
            val moviesResult = mediaRepository.getAnimeMovies()

            val trending = trendingResult.getOrDefault(emptyList()).ifEmpty { SEED_FEATURED_ANIME }
            val topRated = topRatedResult.getOrDefault(emptyList()).ifEmpty { SEED_FEATURED_ANIME.reversed() }
            val shonen = shonenResult.getOrDefault(emptyList()).ifEmpty { SEED_FEATURED_ANIME }
            val movies = moviesResult.getOrDefault(emptyList())

            // ----------------------------------------------------------------
            // Featured carousel, sourced live from TMDB.
            //
            // It used to be six hardcoded titles with hardcoded artwork paths,
            // every one of which had rotted upstream. Taking the head of the
            // trending list instead means the page follows what is actually
            // popular and can never again point at artwork that no longer exists.
            //
            // The rating floor exists because raw "popular Japanese animation"
            // includes thin, barely-rated entries that have no business being a
            // full-screen billboard; a backdrop is required for the same reason.
            // ----------------------------------------------------------------
            val featuredCandidates = trending
                .filter {
                    !it.backdropPath.isNullOrBlank() &&
                        (it.voteAverage ?: 0.0) >= 7.5 &&
                        !it.overview.isNullOrBlank()
                }
                .take(5)

            val featuredList = if (featuredCandidates.isNotEmpty()) {
                // Trending entries already carry live artwork from the list
                // endpoint, so only logos need fetching.
                mediaRepository.withLogos(featuredCandidates, limit = featuredCandidates.size)
            } else {
                // Offline or an upstream failure: fall back to the curated seeds,
                // refreshing their artwork since their hardcoded paths are dead.
                val seeds = SEED_FEATURED_ANIME.take(5)
                mediaRepository.withLogos(
                    mediaRepository.refreshArtwork(seeds),
                    limit = seeds.size
                )
            }

            val heroItem = featuredList.firstOrNull() ?: SEED_ATTACK_ON_TITAN

            _uiState.value = AnimeUiState.Success(
                AnimeFeedData(
                    heroAnime = heroItem,
                    featuredList = featuredList,
                    trendingAnime = trending,
                    topRatedAnime = topRated,
                    shonenAnime = shonen,
                    animeMovies = movies,
                    activeProfile = activeProfile
                )
            )
        }
    }
}
