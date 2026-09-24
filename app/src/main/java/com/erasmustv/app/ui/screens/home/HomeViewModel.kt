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

import com.erasmustv.app.data.model.HomeSection
import com.erasmustv.app.ui.navigation.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeData(
    val heroItem: MediaItem? = null,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
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

    companion object {
        private var memoryCachedData: HomeData? = null
    }

    init {
        if (memoryCachedData != null) {
            _uiState.value = HomeUiState.Success(memoryCachedData!!)
            // Refresh continue watching in background
            refreshContinueWatchingOnly()
        } else {
            loadHomeFeed()
        }
    }

    private fun refreshContinueWatchingOnly() {
        viewModelScope.launch {
            try {
                val activeProfile = profileManager.getActiveProfile()
                val profileId = activeProfile?.id ?: "default-profile"
                val rawCw = streamRepository.listContinueWatching(profileId)
                val current = memoryCachedData ?: return@launch
                val updated = current.copy(continueWatching = rawCw, activeProfile = activeProfile)
                memoryCachedData = updated
                _uiState.value = HomeUiState.Success(updated)
            } catch (_: Exception) {}
        }
    }

    fun loadHomeFeed(forceRefresh: Boolean = false) {
        if (!forceRefresh && memoryCachedData != null && memoryCachedData!!.sections.size > 10) {
            _uiState.value = HomeUiState.Success(memoryCachedData!!)
            return
        }

        viewModelScope.launch {
            if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }
            try {
                val activeProfile = profileManager.getActiveProfile()
                val profileId = activeProfile?.id ?: "default-profile"

                val rawContinueWatching = streamRepository.listContinueWatching(profileId)

                // STAGE 1: Fast above-the-fold queries for immediate <400ms TV render.
                // These cover every shelf in the page's *final* leading order, so
                // Stage 2 only ever appends — the user never sees a shelf inserted
                // above the one they are already looking at.
                val trendingDeferred = async { mediaRepository.getTrending().getOrDefault(emptyList()) }
                val top10MoviesDeferred = async { mediaRepository.getTop10Movies().getOrDefault(emptyList()) }
                val top10TvDeferred = async { mediaRepository.getTop10Tv().getOrDefault(emptyList()) }
                val tvDramasDeferred = async { mediaRepository.getDiscoverTv(withGenres = "18").getOrDefault(emptyList()) }
                val hitComediesDeferred = async { mediaRepository.getDiscoverMovies(withGenres = "35").getOrDefault(emptyList()) }
                val actionMoviesDeferred = async { mediaRepository.getDiscoverMovies(withGenres = "28,12").getOrDefault(emptyList()) }
                val popularMoviesDeferred = async { mediaRepository.getPopularMovies().getOrDefault(emptyList()) }
                val popularTvDeferred = async { mediaRepository.getPopularTv().getOrDefault(emptyList()) }
                val topRatedDeferred = async { mediaRepository.getTopRatedMovies().getOrDefault(emptyList()) }

                val trending = trendingDeferred.await()
                val top10Movies = top10MoviesDeferred.await()
                val top10Tv = top10TvDeferred.await()
                val tvDramas = tvDramasDeferred.await()
                val hitComedies = hitComediesDeferred.await()
                val actionMovies = actionMoviesDeferred.await()
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

                // Stage 1 shelves, in the page's canonical order.
                val initialSections = mutableListOf<HomeSection>()
                if (top10Movies.isNotEmpty()) {
                    initialSections.add(HomeSection("top_10_movies", "Top 10 Movies Today", top10Movies, isRanked = true, viewAllRoute = NavRoutes.MOVIES))
                }
                if (trendingWithLogos.isNotEmpty()) {
                    initialSections.add(HomeSection("trending_now", "Trending Now", trendingWithLogos, viewAllRoute = NavRoutes.MOVIES))
                }
                if (top10Tv.isNotEmpty()) {
                    initialSections.add(HomeSection("top_10_tv", "Top 10 TV Shows Today", top10Tv, isRanked = true, viewAllRoute = NavRoutes.TV))
                }
                if (tvDramas.isNotEmpty()) {
                    initialSections.add(HomeSection("binge_tv_dramas", "Binge-Worthy TV Dramas", tvDramas, viewAllRoute = NavRoutes.TV))
                }
                if (hitComedies.isNotEmpty()) {
                    initialSections.add(HomeSection("hit_comedies", "Hit Comedies", hitComedies, viewAllRoute = NavRoutes.MOVIES))
                }
                if (actionMovies.isNotEmpty()) {
                    initialSections.add(HomeSection("action_blockbusters", "Action & Adventure Blockbusters", actionMovies, viewAllRoute = NavRoutes.MOVIES))
                }
                if (popularMovies.isNotEmpty()) {
                    initialSections.add(HomeSection("new_movies", "New Movies", popularMovies, showNewBadge = true, viewAllRoute = NavRoutes.MOVIES))
                }
                if (popularTv.isNotEmpty()) {
                    initialSections.add(HomeSection("popular_tv", "Popular TV Shows", popularTv, viewAllRoute = NavRoutes.TV))
                }
                if (topRated.isNotEmpty()) {
                    initialSections.add(HomeSection("top_rated", "Top Rated", topRated, viewAllRoute = NavRoutes.MOVIES))
                }

                val stage1Data = HomeData(
                    heroItem = heroItem,
                    continueWatching = continueWatching,
                    sections = initialSections,
                    trending = trendingWithLogos,
                    popularMovies = popularMovies,
                    popularTv = popularTv,
                    topRated = topRated,
                    activeProfile = activeProfile
                )

                // Emit Stage 1 immediately so user has zero wait time
                _uiState.value = HomeUiState.Success(stage1Data)
                memoryCachedData = stage1Data

                // STAGE 2: Asynchronously hydrate extended curated categories.
                withContext(Dispatchers.IO) {
                    val sciFiDef = async { mediaRepository.getDiscoverMovies(withGenres = "878,14").getOrDefault(emptyList()) }
                    val animeHitsDef = async { mediaRepository.getTrendingAnime().getOrDefault(emptyList()) }
                    val kdramasDef = async { mediaRepository.getKdramas().getOrDefault(emptyList()) }
                    val thrillersDef = async { mediaRepository.getDiscoverMovies(withGenres = "53").getOrDefault(emptyList()) }
                    val crimeMysteryDef = async { mediaRepository.getDiscoverTv(withGenres = "80,9648").getOrDefault(emptyList()) }
                    val romanceDef = async { mediaRepository.getDiscoverMovies(withGenres = "10749").getOrDefault(emptyList()) }
                    val horrorDef = async { mediaRepository.getDiscoverMovies(withGenres = "27").getOrDefault(emptyList()) }
                    val documentariesDef = async { mediaRepository.getDiscoverMovies(withGenres = "99").getOrDefault(emptyList()) }
                    val familyDef = async { mediaRepository.getDiscoverMovies(withGenres = "10751").getOrDefault(emptyList()) }
                    val acclaimedDef = async { mediaRepository.getCriticallyAcclaimedMovies().getOrDefault(emptyList()) }
                    val nowPlayingDef = async { mediaRepository.getNowPlayingMovies().getOrDefault(emptyList()) }
                    val topRatedTvDef = async { mediaRepository.getTopRatedTv().getOrDefault(emptyList()) }
                    val upcomingDef = async { mediaRepository.getUpcomingMovies().getOrDefault(emptyList()) }
                    val tvActionDef = async { mediaRepository.getDiscoverTv(withGenres = "10759").getOrDefault(emptyList()) }
                    val tvComediesDef = async { mediaRepository.getDiscoverTv(withGenres = "35").getOrDefault(emptyList()) }
                    val animeMoviesDef = async { mediaRepository.getAnimeMovies().getOrDefault(emptyList()) }

                    // Stage 1's shelves keep their exact identity and position;
                    // everything below is appended after them.
                    val fullSections = mutableListOf<HomeSection>()
                    fullSections.addAll(initialSections)

                    val sciFi = sciFiDef.await()
                    if (sciFi.isNotEmpty()) {
                        fullSections.add(HomeSection("sci_fi_fantasy", "Mind-Bending Sci-Fi & Fantasy", sciFi, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val animeHits = animeHitsDef.await()
                    if (animeHits.isNotEmpty()) {
                        fullSections.add(HomeSection("anime_hits", "Japanese Anime Hits", animeHits, viewAllRoute = NavRoutes.ANIME))
                    }
                    val kdramas = kdramasDef.await()
                    if (kdramas.isNotEmpty()) {
                        fullSections.add(HomeSection("k_dramas", "Trending Korean Dramas", kdramas, viewAllRoute = NavRoutes.TV))
                    }
                    val thrillers = thrillersDef.await()
                    if (thrillers.isNotEmpty()) {
                        fullSections.add(HomeSection("thrillers", "Edge-of-Your-Seat Thrillers", thrillers, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val crimeMystery = crimeMysteryDef.await()
                    if (crimeMystery.isNotEmpty()) {
                        fullSections.add(HomeSection("crime_mystery", "Dark Crime & Mystery", crimeMystery, viewAllRoute = NavRoutes.TV))
                    }
                    val romance = romanceDef.await()
                    if (romance.isNotEmpty()) {
                        fullSections.add(HomeSection("romance", "Romantic Favorites", romance, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val horror = horrorDef.await()
                    if (horror.isNotEmpty()) {
                        fullSections.add(HomeSection("horror", "Chilling Horror", horror, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val documentaries = documentariesDef.await()
                    if (documentaries.isNotEmpty()) {
                        fullSections.add(HomeSection("documentaries", "Fascinating Documentaries", documentaries, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val family = familyDef.await()
                    if (family.isNotEmpty()) {
                        fullSections.add(HomeSection("family_movies", "Family Movie Night", family, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val acclaimed = acclaimedDef.await()
                    if (acclaimed.isNotEmpty()) {
                        fullSections.add(HomeSection("acclaimed", "Critically Acclaimed Masterpieces", acclaimed, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val nowPlaying = nowPlayingDef.await()
                    if (nowPlaying.isNotEmpty()) {
                        fullSections.add(HomeSection("now_playing", "Now Playing in Theaters", nowPlaying, showNewBadge = true, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val topRatedTv = topRatedTvDef.await()
                    if (topRatedTv.isNotEmpty()) {
                        fullSections.add(HomeSection("top_rated_tv", "Top Rated TV Shows", topRatedTv, viewAllRoute = NavRoutes.TV))
                    }
                    val upcoming = upcomingDef.await()
                    if (upcoming.isNotEmpty()) {
                        fullSections.add(HomeSection("upcoming", "Upcoming Releases", upcoming, viewAllRoute = NavRoutes.MOVIES))
                    }
                    val tvAction = tvActionDef.await()
                    if (tvAction.isNotEmpty()) {
                        fullSections.add(HomeSection("tv_action", "TV Action & Adventure", tvAction, viewAllRoute = NavRoutes.TV))
                    }
                    val tvComedies = tvComediesDef.await()
                    if (tvComedies.isNotEmpty()) {
                        fullSections.add(HomeSection("tv_comedies", "Laugh-Out-Loud TV Comedies", tvComedies, viewAllRoute = NavRoutes.TV))
                    }
                    val animeMovies = animeMoviesDef.await()
                    if (animeMovies.isNotEmpty()) {
                        fullSections.add(HomeSection("anime_movies", "Anime Feature Films", animeMovies, viewAllRoute = NavRoutes.ANIME))
                    }
                    if (topRated.isNotEmpty()) {
                        fullSections.add(HomeSection("classics", "All-Time Classic Movies", topRated, viewAllRoute = NavRoutes.MOVIES))
                    }

                    val fullData = stage1Data.copy(sections = fullSections)
                    memoryCachedData = fullData
                    withContext(Dispatchers.Main) {
                        _uiState.value = HomeUiState.Success(fullData)
                    }
                }
            } catch (e: Exception) {
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(e.message ?: "Failed to load home content")
                }
            }
        }
    }
}
