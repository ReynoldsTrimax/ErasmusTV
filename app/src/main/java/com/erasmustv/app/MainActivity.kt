package com.erasmustv.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.erasmustv.app.core.config.AppConfig
import com.erasmustv.app.core.network.NetworkClient
import com.erasmustv.app.core.theme.ErasmusTvTheme
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.data.local.PlaybackProgressStore
import com.erasmustv.app.data.local.ProfileManager
import com.erasmustv.app.data.local.SessionManager
import com.erasmustv.app.data.remote.CinejoyStreamResolver
import com.erasmustv.app.data.remote.ErasmusStreamApiService
import com.erasmustv.app.data.remote.SubtitleResolver
import com.erasmustv.app.data.remote.SupabaseApiService
import com.erasmustv.app.data.remote.TmdbApiService
import com.erasmustv.app.ui.navigation.AppContainer
import com.erasmustv.app.ui.navigation.AppNavigation
import com.erasmustv.app.ui.navigation.NavRoutes

/**
 * Routes an external launch intent is allowed to open directly.
 *
 * Deliberately a small allow-list of argument-free browse destinations. Routes
 * that take arguments, and the login/player flows, are excluded: they either
 * cannot be constructed from a bare route string or would land the user in a
 * half-initialised state.
 */
private val LAUNCH_SAFE_ROUTES = setOf(
    NavRoutes.PROFILES,
    NavRoutes.HOME,
    NavRoutes.MOVIES,
    NavRoutes.TV,
    NavRoutes.ANIME,
    NavRoutes.SEARCH,
    NavRoutes.WATCHLIST
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TV immersive full-screen setup
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            ErasmusTvTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PitchBlack)
                ) {
                    val context = this@MainActivity
                    val container = remember {
                        val sessionManager = SessionManager(context)
                        val profileManager = ProfileManager(context)
                        val progressStore = PlaybackProgressStore(context)
                        val okHttpClient = NetworkClient.createOkHttpClient(sessionManager)

                        val tmdbApi = NetworkClient.createService(
                            TmdbApiService::class.java,
                            AppConfig.TMDB_BASE_URL,
                            okHttpClient
                        )
                        val supabaseApi = NetworkClient.createService(
                            SupabaseApiService::class.java,
                            if (AppConfig.SUPABASE_URL.endsWith("/")) AppConfig.SUPABASE_URL else "${AppConfig.SUPABASE_URL}/",
                            okHttpClient
                        )
                        val cinejoyResolver = CinejoyStreamResolver(okHttpClient)
                        val subtitleResolver = SubtitleResolver(okHttpClient)
                        val streamApi = NetworkClient.createService(
                            ErasmusStreamApiService::class.java,
                            if (AppConfig.BACKEND_BASE_URL.endsWith("/")) AppConfig.BACKEND_BASE_URL else "${AppConfig.BACKEND_BASE_URL}/",
                            okHttpClient
                        )
                        val omdbApi = NetworkClient.createService(
                            com.erasmustv.app.data.remote.OmdbApiService::class.java,
                            if (AppConfig.OMDB_BASE_URL.endsWith("/")) AppConfig.OMDB_BASE_URL else "${AppConfig.OMDB_BASE_URL}/",
                            okHttpClient
                        )

                        AppContainer(
                            sessionManager = sessionManager,
                            profileManager = profileManager,
                            progressStore = progressStore,
                            tmdbApi = tmdbApi,
                            supabaseApi = supabaseApi,
                            cinejoyResolver = cinejoyResolver,
                            subtitleResolver = subtitleResolver,
                            streamApi = streamApi,
                            omdbApi = omdbApi,
                            okHttpClient = okHttpClient
                        )
                    }

                    var sessionState by remember {
                        mutableStateOf<com.erasmustv.app.data.model.SessionCheckResult>(
                            com.erasmustv.app.data.model.SessionCheckResult.Checking
                        )
                    }

                    LaunchedEffect(Unit) {
                        sessionState = container.authRepository.checkSession()
                    }

                    when (val state = sessionState) {
                        is com.erasmustv.app.data.model.SessionCheckResult.Checking -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PitchBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "E R A S M U S",
                                        style = ErasmusTvTypography.Wordmark
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    CircularProgressIndicator(
                                        color = FocusWhite,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        is com.erasmustv.app.data.model.SessionCheckResult.Authenticated,
                        is com.erasmustv.app.data.model.SessionCheckResult.Guest -> {
                            val navController = rememberNavController()
                            // ------------------------------------------------------
                            // `target_route` arrives from an intent on an exported
                            // activity, so it is untrusted input. Two things go
                            // wrong if it is used as-is:
                            //
                            //  - a value with no registered composable crashes
                            //    during graph construction, outside any try/catch;
                            //  - any non-Profiles value bypasses the profile gate
                            //    and starts the graph somewhere Home is absent, so
                            //    `saveState`/`restoreState` never engage and every
                            //    tab switch leaks a back stack entry.
                            //
                            // Only routes that are genuinely safe entry points are
                            // honoured; anything else falls back to the normal
                            // start destination.
                            // ------------------------------------------------------
                            val startDestination = remember {
                                intent?.getStringExtra("target_route")
                                    ?.takeIf { it in LAUNCH_SAFE_ROUTES }
                                    ?: NavRoutes.PROFILES
                            }
                            AppNavigation(
                                navController = navController,
                                container = container,
                                startDestination = startDestination
                            )
                        }
                        is com.erasmustv.app.data.model.SessionCheckResult.Unauthenticated -> {
                            val navController = rememberNavController()
                            AppNavigation(
                                navController = navController,
                                container = container,
                                startDestination = NavRoutes.LOGIN
                            )
                        }
                    }
                }
            }
        }
    }
}

