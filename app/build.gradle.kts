import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.erasmustv.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.erasmustv.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val envFile = rootProject.file(".env.local")
        val envProps = Properties()
        if (envFile.exists()) {
            envFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val idx = trimmed.indexOf('=')
                    val k = trimmed.substring(0, idx).trim()
                    val v = trimmed.substring(idx + 1).trim().removeSurrounding("\"")
                    envProps.setProperty(k, v)
                }
            }
        }
        val supabaseUrl = envProps.getProperty("NEXT_PUBLIC_SUPABASE_URL") ?: "https://jnxflxtizbezqclxfmzc.supabase.co"
        val supabaseAnonKey = envProps.getProperty("NEXT_PUBLIC_SUPABASE_ANON_KEY") ?: "sb_publishable_unVVFGR4uZmN9AODsAqLLw_8fKeC0-a"
        val tmdbApiKey = envProps.getProperty("TMDB_API_KEY") ?: "7123e5f4df40d602a3109ea8df11e320"
        val omdbApiKey = envProps.getProperty("OMDB_API_KEY") ?: "trilogy"
        val backendBaseUrl = envProps.getProperty("NEXT_PUBLIC_APP_URL") ?: "https://api.erasmustv.app"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
        buildConfigField("String", "OMDB_API_KEY", "\"$omdbApiKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"394731738519-8pru43jda4t1c57a1gp0umagupabka02.apps.googleusercontent.com\"")
    }

    buildTypes {
        release {
            // Deliberately left unminified.
            //
            // R8 would shrink the APK but does nothing for scroll smoothness,
            // and this app's Retrofit + kotlinx-serialization surface is exactly
            // the kind of reflective code that breaks silently under shrinking.
            // The scroll win from a release build comes from it being
            // non-debuggable and from ART applying the Compose baseline
            // profiles, both of which happen without minification.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed with the debug key so a release-quality build is
            // side-loadable onto a TV for testing without key management.
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // TV Compose
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    // Media3 ExoPlayer for TV video streaming
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Network & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Installs the Compose baseline profiles bundled with the AndroidX
    // libraries, so ART has the hot scroll/composition paths compiled ahead of
    // time instead of interpreting them until the JIT catches up. This is the
    // single largest scroll-jank reduction available on low-end TV chipsets,
    // and it only takes effect in non-debuggable builds.
    implementation(libs.androidx.profileinstaller)

    // Image loading with TV caching
    implementation(libs.coil.compose)
    // A large share of TMDB title logos are published as SVG. Without a vector
    // decoder those requests simply fail, which is why many titles fell back to
    // rendering their name as plain text instead of their logo.
    implementation(libs.coil.svg)

    // Palette extraction for hero-artwork-derived accent colors
    implementation(libs.androidx.palette.ktx)

    // DataStore for session & active profile
    implementation(libs.androidx.datastore.preferences)

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
