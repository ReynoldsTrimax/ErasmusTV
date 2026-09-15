# ErasmusTV — Native Android TV Application

**ErasmusTV** is a premium, native Android TV entertainment application built with modern Kotlin, Jetpack Compose for TV, and AndroidX Media3 (ExoPlayer).

---

## Highlights & Features

- **TV-First Remote UX**: Fully optimized for Android TV remote controls (D-pad Up/Down/Left/Right, Center/Select/OK, Back).
- **Erasmus Design Language**: True pitch-black OLED background (`#000000`), elevated dark surfaces (`#0D0D0D`), vivid electric blue accent (`#1D90F5`), and signature Bostone wordmark.
- **Hero Billboard**: Cinematic spotlight on trending movies and series with backdrop art, ratings, synopsis, and 1-click Watch Now.
- **Profiles System**: Integrated with Supabase `watch_profiles` table, supporting multiple household profiles with 8 avatar color themes and age-based maturity filters.
- **Catalog & Discovery**: Powered by TMDB & OMDb API integrations:
  - Trending Today
  - Popular Movies & Series
  - Now Playing in Theaters & Upcoming Releases
  - Top Rated Movies & Shows
  - Search with debounced multi-search and poster grid
  - Watchlist synchronized per profile
  - TV Shows with Season switcher and Episode still cards
  - "Where to Watch" regional legal streaming providers (Netflix, Prime, Disney+, etc.)
- **Native Video Player (Media3 / ExoPlayer)**:
  - Direct HLS (`.m3u8`) and MP4 video playback
  - Custom HTTP headers (`Referer`, `Origin`, `User-Agent`) for stream extraction
  - Multi-server cluster switcher (Lisbon flagship, Sakura, Nebula, Solara, Athens, Joy, Castle, Canaias)
  - Subtitle track switching (Wyzie and Cinejoy captions)
  - D-pad media key handling: OK to Play/Pause, Left/Right for 10s scrub, Up/Down for HUD overlay
  - Profile-scoped playback resume tracking and "Continue Watching" carousel

---

## Tech Stack

| Layer | Choice |
|---|---|
| **Platform** | Native Android TV (`minSdk 26`, `targetSdk 35`) |
| **Language** | Kotlin 2.1.0 + Coroutines |
| **UI Framework** | Jetpack Compose for TV (`androidx.tv:tv-material`, `tv-foundation`) |
| **Video Engine** | AndroidX Media3 (ExoPlayer 1.5.1 + HLS module) |
| **Image Loading** | Coil Compose (with 250MB disk & 25% RAM cache) |
| **Networking** | Retrofit 2 + OkHttp 4 + Kotlinx Serialization |
| **Persistence** | Jetpack DataStore Preferences + SharedPreferences |
| **Architecture** | Modern Android Clean Architecture (UI -> ViewModel -> Repository -> Remote/Local Data Sources) |

---

## Project Structure

```
ErasmusTV/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/erasmustv/app/
│       │   │   ├── ErasmusTvApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/
│       │   │   │   ├── config/AppConfig.kt
│       │   │   │   ├── network/NetworkClient.kt
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt
│       │   │   │       ├── Theme.kt
│       │   │   │       └── Type.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── PlaybackProgressStore.kt
│       │   │   │   │   ├── ProfileManager.kt
│       │   │   │   │   └── SessionManager.kt
│       │   │   │   ├── model/
│       │   │   │   │   ├── AuthModels.kt
│       │   │   │   │   ├── MediaModels.kt
│       │   │   │   │   ├── ProfileModels.kt
│       │   │   │   │   └── StreamModels.kt
│       │   │   │   ├── remote/
│       │   │   │   │   ├── ErasmusStreamApiService.kt
│       │   │   │   │   ├── SupabaseApiService.kt
│       │   │   │   │   └── TmdbApiService.kt
│       │   │   │   └── repository/
│       │   │   │       ├── AuthRepository.kt
│       │   │   │       ├── MediaRepository.kt
│       │   │   │       ├── ProfileRepository.kt
│       │   │   │       ├── StreamRepository.kt
│       │   │   │       └── WatchlistRepository.kt
│       │   │   └── ui/
│       │   │       ├── components/
│       │   │       │   ├── ContinueWatchingRow.kt
│       │   │       │   ├── HeroBillboard.kt
│       │   │       │   ├── MediaPosterCard.kt
│       │   │       │   ├── MediaSectionRow.kt
│       │   │       │   ├── TvFocusableCard.kt
│       │   │       │   └── TvNavigationDrawer.kt
│       │   │       ├── navigation/
│       │   │       │   ├── AppNavigation.kt
│       │   │       │   └── NavRoutes.kt
│       │   │       └── screens/
│       │   │           ├── auth/
│       │   │           ├── details/
│       │   │           ├── home/
│       │   │           ├── movies/
│       │   │           ├── player/
│       │   │           ├── profiles/
│       │   │           ├── search/
│       │   │           ├── tv/
│       │   │           └── watchlist/
│       │   └── res/
│       │       ├── drawable/
│       │       ├── font/bostone.ttf
│       │       ├── mipmap-*/
│       │       └── values/
│       └── test/
│           └── java/com/erasmustv/app/ErasmusTvUnitTest.kt
├── database/
│   └── migrations/ (001–007 SQL migrations)
├── docs/ (subsystem documentation)
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── ANDROID_TV_MIGRATION_PLAN.md
```

---

## Build & Run

### Prerequisites
- JDK 17 (`JAVA_HOME` pointing to OpenJDK 17)
- Android SDK with platform `android-35` and `build-tools;35.0.0` (configured in `local.properties`)

### Commands

```bash
# Compile Kotlin code
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileDebugKotlin

# Run unit tests
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest

# Assemble Debug APK
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug

# Install on Android TV device or emulator (via adb)
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## TV Remote Navigation Controls

| Key | Context | Action |
|---|---|---|
| **D-pad Center / OK** | General UI | Select / open highlighted item |
| **D-pad Center / OK** | Video Player | Toggle Play / Pause |
| **D-pad Left / Right** | Video Player | Seek -10s / +10s with scrub indicator |
| **D-pad Up / Down** | Video Player | Show / hide On-Screen HUD and Server switcher |
| **Back Button** | Video Player | Save current progress and exit smoothly to details |
| **Back Button** | General UI | Navigate back / dismiss modal |
