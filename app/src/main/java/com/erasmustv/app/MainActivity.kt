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

                        AppContainer(
                            sessionManager = sessionManager,
                            profileManager = profileManager,
                            progressStore = progressStore,
                            tmdbApi = tmdbApi,
                            supabaseApi = supabaseApi,
                            cinejoyResolver = cinejoyResolver,
                            subtitleResolver = subtitleResolver,
                            streamApi = streamApi,
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
                            AppNavigation(
                                navController = navController,
                                container = container,
                                startDestination = NavRoutes.PROFILES
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

