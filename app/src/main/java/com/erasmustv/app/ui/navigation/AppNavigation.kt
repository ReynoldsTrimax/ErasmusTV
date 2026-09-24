package com.erasmustv.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.erasmustv.app.data.local.PlaybackProgressStore
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.local.SessionManager
import com.erasmustv.app.data.remote.BingrStreamResolver
import com.erasmustv.app.data.remote.CinejoyStreamResolver
import com.erasmustv.app.data.remote.ErasmusStreamApiService
import com.erasmustv.app.data.remote.OmdbApiService
import com.erasmustv.app.data.remote.SubtitleResolver
import com.erasmustv.app.data.remote.SupabaseApiService
import com.erasmustv.app.data.remote.TmdbApiService
import com.erasmustv.app.data.repository.AuthRepository
import com.erasmustv.app.data.repository.MediaRepository
import com.erasmustv.app.data.repository.ProfileRepository
import com.erasmustv.app.data.repository.StreamRepository
import com.erasmustv.app.data.repository.WatchlistRepository
import com.erasmustv.app.ui.screens.auth.LoginScreen
import com.erasmustv.app.ui.screens.auth.LoginViewModel
import com.erasmustv.app.ui.screens.details.MediaDetailScreen
import com.erasmustv.app.ui.screens.details.MediaDetailViewModel
import com.erasmustv.app.ui.screens.home.HomeScreen
import com.erasmustv.app.ui.screens.home.HomeViewModel
import com.erasmustv.app.ui.screens.movies.MoviesScreen
import com.erasmustv.app.ui.screens.movies.MoviesViewModel
import com.erasmustv.app.ui.screens.player.TvPlayerScreen
import com.erasmustv.app.ui.screens.player.TvPlayerViewModel
import com.erasmustv.app.ui.screens.profiles.ProfilePickerScreen
import com.erasmustv.app.ui.screens.profiles.ProfileViewModel
import com.erasmustv.app.ui.screens.search.SearchScreen
import com.erasmustv.app.ui.screens.search.SearchViewModel
import com.erasmustv.app.ui.screens.tv.TvShowsScreen
import com.erasmustv.app.ui.screens.tv.TvShowsViewModel
import com.erasmustv.app.ui.screens.studios.StudiosScreen
import com.erasmustv.app.ui.screens.studios.StudiosViewModel
import com.erasmustv.app.ui.screens.categories.CategoriesScreen
import com.erasmustv.app.ui.screens.categories.CategoriesViewModel
import com.erasmustv.app.ui.screens.anime.AnimeScreen
import com.erasmustv.app.ui.screens.anime.AnimeViewModel
import com.erasmustv.app.ui.screens.watchlist.WatchlistScreen
import com.erasmustv.app.ui.screens.watchlist.WatchlistViewModel

class AppContainer(
    val sessionManager: SessionManager,
    val profileManager: ProfileManager,
    val progressStore: PlaybackProgressStore,
    val tmdbApi: TmdbApiService,
    val supabaseApi: SupabaseApiService,
    val cinejoyResolver: CinejoyStreamResolver,
    val subtitleResolver: SubtitleResolver,
    val streamApi: ErasmusStreamApiService? = null,
    val omdbApi: OmdbApiService? = null,
    val okHttpClient: okhttp3.OkHttpClient? = null,
    val bingrResolver: BingrStreamResolver = BingrStreamResolver()
) {
    val authRepository = AuthRepository(supabaseApi, sessionManager)
    val profileRepository = ProfileRepository(supabaseApi, profileManager, sessionManager)
    val mediaRepository = MediaRepository(tmdbApi, omdbApi)
    val streamRepository = StreamRepository(cinejoyResolver, bingrResolver, subtitleResolver, progressStore)
    val watchlistRepository = WatchlistRepository(supabaseApi, sessionManager)
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    container: AppContainer,
    startDestination: String = NavRoutes.PROFILES
) {
    // ------------------------------------------------------------------
    // Top-level navigation.
    //
    // `saveState` + `restoreState` are what make every screen's focus memory and
    // scroll position survive a tab switch: without them Navigation Compose
    // discards the destination's saved state, and the screen comes back as if
    // visited for the first time.
    //
    // The pop is anchored on Home, but only when Home is genuinely on the back
    // stack. `popUpTo` a route that is not present is a silent no-op, and with it
    // `saveState`/`restoreState` never engage and every tab switch leaks an
    // entry — so the fallback below does the plain single-top navigate rather
    // than pretending the pop worked.
    // ------------------------------------------------------------------
    val safeNavigate: (String) -> Unit = remember(navController) {
        { route: String ->
            try {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != route) {
                    val homeIsOnStack = navController.currentBackStack.value.any {
                        it.destination.route == NavRoutes.HOME
                    }
                    navController.navigate(route) {
                        if (homeIsOnStack) {
                            popUpTo(NavRoutes.HOME) { saveState = true }
                            restoreState = true
                        }
                        launchSingleTop = true
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Opens the profile picker without stacking duplicates.
     *
     * Every screen's profile avatar called a bare `navigate(PROFILES)`, so
     * pressing it twice pushed two identical entries and BACK had to be pressed
     * twice to escape one of them.
     */
    val openProfiles: () -> Unit = remember(navController) {
        {
            try {
                navController.navigate(NavRoutes.PROFILES) { launchSingleTop = true }
            } catch (_: Exception) {}
        }
    }
    val isReducedMotion = com.erasmustv.app.core.theme.rememberReducedMotion()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (isReducedMotion) androidx.compose.animation.EnterTransition.None
            else androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_MEDIUM,
                    easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                )
            )
        },
        exitTransition = {
            if (isReducedMotion) androidx.compose.animation.ExitTransition.None
            else androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_MEDIUM,
                    easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                )
            )
        },
        popEnterTransition = {
            if (isReducedMotion) androidx.compose.animation.EnterTransition.None
            else androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_MEDIUM,
                    easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                )
            )
        },
        popExitTransition = {
            if (isReducedMotion) androidx.compose.animation.ExitTransition.None
            else androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = com.erasmustv.app.core.theme.TvMotion.DURATION_MEDIUM,
                    easing = com.erasmustv.app.core.theme.TvMotion.EasingSilk
                )
            )
        }
    ) {
        // Login
        composable(NavRoutes.LOGIN) {
            val vm = remember { LoginViewModel(container.authRepository) }
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(NavRoutes.PROFILES) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // Profile Selection
        composable(NavRoutes.PROFILES) {
            val vm = remember {
                ProfileViewModel(container.profileRepository, container.authRepository)
            }
            ProfilePickerScreen(
                viewModel = vm,
                onProfileSelected = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.PROFILES) { inclusive = true }
                        // Without this, selecting a profile while Home is already
                        // on the stack pushed a *second* Home entry with empty
                        // saveable state: focus memory and scroll position both
                        // started blank, and BACK no longer exited the app.
                        launchSingleTop = true
                    }
                },
                onSignOut = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(NavRoutes.HOME) {
            val vm = remember {
                HomeViewModel(container.mediaRepository, container.streamRepository, container.profileManager)
            }
            HomeScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onPlayClick = { item ->
                    navController.navigate(
                        NavRoutes.player(
                            item.mediaType,
                            item.id,
                            item.title,
                            posterPath = item.posterPath,
                            backdropPath = item.backdropPath,
                            logoPath = item.logoPath,
                            tagline = item.tagline
                        )
                    )
                },
                onResumeClick = { cw ->
                    navController.navigate(
                        NavRoutes.player(
                            cw.mediaType,
                            cw.tmdbId,
                            cw.title,
                            cw.season,
                            cw.episode,
                            posterPath = cw.posterPath,
                            backdropPath = cw.backdropPath,
                            logoPath = cw.logoPath,
                            tagline = cw.tagline
                        )
                    )
                },
                onProfileClick = openProfiles
            )
        }

        // Movies Screen
        composable(NavRoutes.MOVIES) {
            val vm = remember {
                MoviesViewModel(container.mediaRepository, container.profileManager)
            }
            MoviesScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details("movie", item.id))
                },
                onPlayClick = { item ->
                    navController.navigate(
                        NavRoutes.player(
                            "movie",
                            item.id,
                            item.title,
                            posterPath = item.posterPath,
                            backdropPath = item.backdropPath,
                            logoPath = item.logoPath,
                            tagline = item.tagline
                        )
                    )
                },
                onProfileClick = openProfiles
            )
        }

        // TV Shows Screen
        composable(NavRoutes.TV) {
            val vm = remember {
                TvShowsViewModel(container.mediaRepository, container.profileManager)
            }
            TvShowsScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details("tv", item.id))
                },
                onPlayClick = { item ->
                    navController.navigate(
                        NavRoutes.player(
                            "tv",
                            item.id,
                            item.title,
                            1,
                            1,
                            posterPath = item.posterPath,
                            backdropPath = item.backdropPath,
                            logoPath = item.logoPath,
                            tagline = item.tagline
                        )
                    )
                },
                onProfileClick = openProfiles
            )
        }

        // Anime Screen
        composable(NavRoutes.ANIME) {
            val vm = remember {
                AnimeViewModel(
                    container.mediaRepository,
                    container.profileManager
                )
            }
            AnimeScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onPlayClick = { item ->
                    navController.navigate(
                        NavRoutes.player(
                            item.mediaType,
                            item.id,
                            item.title,
                            posterPath = item.posterPath,
                            backdropPath = item.backdropPath,
                            logoPath = item.logoPath,
                            tagline = item.tagline
                        )
                    )
                },
                onProfileClick = openProfiles
            )
        }

        // Categories Screen (Replaces Studios, with Studios inside)
        composable(NavRoutes.CATEGORIES) {
            val vm = remember {
                CategoriesViewModel(
                    container.mediaRepository,
                    container.profileManager
                )
            }
            CategoriesScreen(
                viewModel = vm,
                // Routed through safeNavigate like every other screen, so it
                // cannot destroy sibling tabs' saved state or stack duplicates.
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onProfileClick = openProfiles
            )
        }

        // Studios Screen (Dedicated Hub & Catalog)
        composable(NavRoutes.STUDIOS) {
            val vm = remember {
                StudiosViewModel(
                    container.mediaRepository,
                    container.profileManager
                )
            }
            StudiosScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onProfileClick = openProfiles
            )
        }
        composable(
            route = NavRoutes.DETAILS,
            arguments = listOf(
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val vm = remember(mediaType, id) {
                MediaDetailViewModel(
                    mediaType = mediaType,
                    tmdbId = id,
                    mediaRepository = container.mediaRepository,
                    streamRepository = container.streamRepository,
                    watchlistRepository = container.watchlistRepository,
                    profileManager = container.profileManager
                )
            }

            MediaDetailScreen(
                viewModel = vm,
                onBackClick = { navController.popBackStack() },
                onPlayClick = { mType, mId, mTitle, s, e, poster, backdrop, logo, tagline ->
                    navController.navigate(NavRoutes.player(mType, mId, mTitle, s, e, poster, backdrop, logo, tagline))
                },
                onSimilarClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onNavigate = safeNavigate,
                onProfileClick = openProfiles
            )
        }

        // Search Screen
        composable(NavRoutes.SEARCH) {
            val vm = remember {
                SearchViewModel(container.mediaRepository, container.profileManager)
            }
            SearchScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onProfileClick = openProfiles
            )
        }

        // Watchlist Screen
        composable(NavRoutes.WATCHLIST) {
            val vm = remember {
                WatchlistViewModel(container.watchlistRepository, container.profileManager)
            }
            WatchlistScreen(
                viewModel = vm,
                onNavigate = safeNavigate,
                onMediaClick = { item ->
                    navController.navigate(NavRoutes.details(item.mediaType, item.id))
                },
                onProfileClick = openProfiles
            )
        }

        // Native Video Player
        composable(
            route = NavRoutes.PLAYER,
            arguments = listOf(
                navArgument("mediaType") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("season") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("episode") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("posterPath") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("backdropPath") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("logoPath") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("tagline") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "movie"
            val id = backStackEntry.arguments?.getString("id") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Video"
            val season = backStackEntry.arguments?.getInt("season")
            val episode = backStackEntry.arguments?.getInt("episode")
            val rawPoster = backStackEntry.arguments?.getString("posterPath")
            val rawBackdrop = backStackEntry.arguments?.getString("backdropPath")
            val rawLogo = backStackEntry.arguments?.getString("logoPath")
            val rawTagline = backStackEntry.arguments?.getString("tagline")
            val posterPath = if (rawPoster.isNullOrBlank()) null else rawPoster
            val backdropPath = if (rawBackdrop.isNullOrBlank()) null else rawBackdrop
            val logoPath = if (rawLogo.isNullOrBlank()) null else rawLogo
            val tagline = if (rawTagline.isNullOrBlank()) null else rawTagline

            val vm = remember(mediaType, id, season, episode, posterPath, backdropPath, logoPath, tagline) {
                TvPlayerViewModel(
                    mediaType = mediaType,
                    tmdbId = id,
                    title = title,
                    season = season,
                    episode = episode,
                    posterPath = posterPath,
                    backdropPath = backdropPath,
                    logoPath = logoPath,
                    tagline = tagline,
                    streamRepository = container.streamRepository,
                    profileManager = container.profileManager,
                    mediaRepository = container.mediaRepository,
                    okHttpClient = container.okHttpClient
                )
            }

            TvPlayerScreen(
                viewModel = vm,
                onExit = { navController.popBackStack() }
            )
        }
    }
}
