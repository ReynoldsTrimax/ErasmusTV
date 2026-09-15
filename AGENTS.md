# ErasmusTV — Agent Notes

## Product

ErasmusTV is the native Android TV application for the Erasmus streaming platform.

It consumes the existing Erasmus/Argus backend infrastructure (Supabase Auth/PostgreSQL database, TMDB/OMDb catalog services, Shegu/Cinejoy stream extraction, and Wyzie subtitles).

The legacy web application has been completely replaced with native Kotlin + Jetpack Compose for TV.

## Stack

- Kotlin 2.1.0, Coroutines
- Jetpack Compose for TV (`androidx.tv:tv-material`, `androidx.tv:tv-foundation`)
- AndroidX Media3 (ExoPlayer 1.5.1 + HLS module)
- Retrofit 2 + OkHttp 4 + Kotlinx Serialization
- Coil Compose for TV image loading & memory/disk caching
- Jetpack DataStore Preferences & SharedPreferences
- Target SDK: 35 (Android 15), Min SDK: 26 (Android 8.0)

## Conventions

- Architecture: Clean Architecture (UI -> ViewModel -> Repository -> Remote / Local Data Sources)
- TV Interaction: 100% remote D-pad operable (no touch, no mouse pointer dependencies)
- Design Language: Erasmus OLED black (`#000000`), elevated dark surfaces (`#0D0D0D`), Electric Blue accent (`#1D90F5`), signature Bostone wordmark
- Playback: AndroidX Media3 with custom HTTP headers (`Referer: https://cinejoy.to/`), multi-server fallback cluster, and profile-isolated resume tracking
- Database & Backend: Source of truth is PostgreSQL under `database/migrations/` (001 → 007) and Supabase GoTrue Auth / PostgREST APIs

## Commands

```bash
# Compile Kotlin code
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileDebugKotlin

# Run unit tests
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest

# Assemble Debug APK
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug
```
