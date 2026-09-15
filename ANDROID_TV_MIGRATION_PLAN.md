# ErasmusTV — Android TV Migration & Architecture Plan

## Executive Summary

This document performs the deep audit of the existing **Erasmus** streaming project before transforming this repository into **ErasmusTV**, the native Android TV application.

The existing Erasmus/Argus backend services, database schema (Supabase), content catalog (TMDB + OMDb), and streaming resolver infrastructure (Shegu / Cinejoy direct stream extraction + multi-server fallback + Wyzie subtitles) serve as the foundation. The legacy Next.js web application, React web components, Tailwind v4 web styling, and browser-specific code will be completely removed and replaced with a clean, high-performance, native Android TV application.

---

## 1. Deep Audit of Existing Architecture

### 1.1 Project Structure Overview
| Path | Description | Classification |
|---|---|---|
| `src/app/` | Next.js App Router pages, layouts, web routes | **C. Web-only** (safe to remove) |
| `src/app/api/stream/direct/` | Direct HLS stream extractor endpoint | **D. Backend** / Useful contract |
| `src/app/api/stream/hls/` | HLS proxy & header rewriter | **D. Backend** / Useful proxy |
| `src/app/api/stream/subs/` | Wyzie subtitle tracks endpoint | **D. Backend** / Useful contract |
| `src/app/api/media/` | Search, allowed maturity, season data | **D. Backend** / Useful contract |
| `src/lib/media/` | TMDB client, catalog, maturity filters, formatters | **B. Useful logic** & models |
| `src/lib/streaming/` | Cinejoy stream resolver, Wyzie, progress tracking | **B. Useful logic** & models |
| `src/lib/supabase/` | Supabase SSR and server client | **B. Useful auth/DB config** |
| `src/lib/watch-profiles/` | Watch profile service & models | **B. Useful profile logic** |
| `src/lib/watchlist/` | Watchlist service & models | **B. Useful watchlist logic** |
| `src/types/` | Media, database, and profile TypeScript models | **B. Useful data contracts** |
| `database/migrations/` | Supabase PostgreSQL migrations (001–007) | **A. Backend source of truth** |
| `my-app/` | Unused Next.js scratch directory | **C. Safe to delete** |

---

## 2. Complete Flow Analysis & Categorization

Each component is categorized according to:
- **A**: Required by the existing backend
- **B**: Useful to the Android TV client
- **C**: Web-only and safe to remove
- **D**: Backend/server-only and must remain deployed remotely
- **E**: Sensitive and must NOT be placed inside the Android APK
- **F**: Recreated natively for Android TV

### Flow 1: Authentication
- **Current Flow**: Web users sign in via email/password or OAuth. Cookies store Supabase session tokens via `@supabase/ssr`.
- **Backend Source**: Supabase Auth GoTrue API (`POST /auth/v1/token?grant_type=password`).
- **Classification**:
  - **A**: Supabase Auth server (`https://<project-id>.supabase.co/auth/v1`).
  - **B**: Supabase URL, Supabase Anon Key, JWT Access Token, Refresh Token, User ID.
  - **C**: Next.js cookies, middleware session proxy (`src/proxy.ts`), web OAuth redirects.
  - **D**: Supabase Auth backend.
  - **E**: `SUPABASE_SERVICE_ROLE_KEY` (MUST NEVER be in APK). `NEXT_PUBLIC_SUPABASE_ANON_KEY` is public and intended for client use.
  - **F**: Native Android TV login screen with D-pad navigable input, EncryptedSharedPreferences / Jetpack DataStore session storage, automatic token refresh.

### Flow 2: Profiles
- **Current Flow**: A single authenticated user can create up to 5 watch profiles (`watch_profiles` table). Web uses an `argus_profile` cookie to scope preferences and maturity level.
- **Backend Source**: Supabase table `public.watch_profiles` with RLS.
- **Classification**:
  - **A**: `public.watch_profiles` table, constraints, and RLS policies (006_watch_profiles.sql).
  - **B**: Profile data model (ID, Name, Avatar Key, Birth Year / Maturity Age, Preferences), 8 Avatar color themes.
  - **C**: `src/lib/watch-profiles/cookie.ts`, web profile modal.
  - **D**: Supabase Postgres DB & RLS triggers (`watch_profiles_enforce_limit`).
  - **E**: None on client (RLS enforces security).
  - **F**: TV "Who's Watching?" profile selection screen, Profile switching from top menu, active profile persisted in Android DataStore.

### Flow 3: Movies & TV Shows (Catalog & Discovery)
- **Current Flow**: Next.js server components query TMDB API via `src/lib/media/providers/tmdb` and assemble rails in `src/lib/media/catalog.ts`. Ratings are enriched with IMDb / Rotten Tomatoes via OMDb API.
- **Backend Source**: TMDB API v3 (`api.themoviedb.org/3`) and OMDb API (`omdbapi.com`).
- **Classification**:
  - **A**: TMDB API v3 endpoints, OMDb API.
  - **B**: MediaSummary, MovieDetails, TvDetails, TvSeason, TvEpisode, CastMember, MediaRating data models. TMDB image CDN URLs (`https://image.tmdb.org/t/p/{size}`).
  - **C**: Next.js React Server Components, Tailwind classes, Framer Motion web effects.
  - **D**: Remote TMDB / OMDb APIs or Erasmus backend proxy.
  - **E**: API keys configured securely via `local.properties` / `BuildConfig` (not hardcoded in source control).
  - **F**: Native Android TV Home screen with Hero Billboard + horizontal carousels (Trending Today, Popular Movies, Popular TV, Top Rated, Continue Watching). Dedicated Movies and TV Shows browse screens with genre filtering. Rich Movie & TV detail screens with cast rails, season & episode selectors.

### Flow 4: Search
- **Current Flow**: Web uses `cmdk` command palette, calling `/api/media/search?q=...`, querying TMDB `/search/multi`.
- **Backend Source**: TMDB multi-search.
- **Classification**:
  - **A**: TMDB Search endpoint.
  - **B**: Query parameters, debounce interval (300ms), result mapping.
  - **C**: `cmdk` palette, Lucide web icons.
  - **D**: TMDB search service.
  - **E**: None.
  - **F**: TV-first Search screen with on-screen keyboard / D-pad grid navigation and instant poster grid.

### Flow 5: Streaming Availability
- **Current Flow**: Calls TMDB `/movie/{id}/watch/providers` and `/tv/{id}/watch/providers` to show regional streaming platforms (Netflix, Prime Video, Disney+, etc.).
- **Backend Source**: TMDB Watch Providers.
- **Classification**:
  - **A**: TMDB watch providers API.
  - **B**: Provider logos, platform names, offer types (`flatrate`, `rent`, `buy`).
  - **C**: Web `StreamingProviders` UI component.
  - **D**: TMDB API.
  - **E**: None.
  - **F**: "Where to Watch" horizontal row on Movie & TV detail screens.

### Flow 6: Playback & Streaming Engine
- **Current Flow**:
  1. Primary Direct Stream: Shegu API (`https://api.shegu.st`) resolves direct HLS `.m3u8` or MP4 streams with captions.
  2. Multi-Server Roster: Lisbon, Sakura, Nebula, Solara, Athens, Joy, Castle, Canaias.
  3. Subtitles: Wyzie API (`/api/stream/subs`) + OpenSubtitles v3.
  4. Playback Progress: Tracks resume position (`seconds`, `duration`) per profile.
- **Backend Source**: Erasmus `/api/stream/direct`, `/api/stream/hls`, `/api/stream/subs` and Shegu resolver.
- **Classification**:
  - **A**: Shegu stream resolution & Wyzie subtitle endpoints.
  - **B**: Server definitions, stream hit data format (`url`, `kind`, `captions`, `referer`), progress thresholds (`MIN_RESUME_SECONDS = 15`, `COMPLETE_RATIO = 0.9`).
  - **C**: `hls.js` JavaScript library, HTML5 `<video>`, web fullscreen API.
  - **D**: Stream resolver backend service and HLS proxy.
  - **E**: None.
  - **F**: Native Android TV Video Player built with **AndroidX Media3 (ExoPlayer)**:
    - Native HLS and MP4 streaming.
    - Custom HTTP headers (`Referer: https://cinejoy.to/`, `User-Agent: ...`) configured on `DefaultHttpDataSource.Factory`.
    - D-pad TV controls: Play/Pause (Select/OK), Seek +/-10s (Left/Right), OSD HUD overlay (Up/Down), Back button exit.
    - Automatic resume position restoration and progress persistence per profile.
    - Subtitle track selector and server switcher.

### Flow 7: Watchlist
- **Current Flow**: Supabase table `public.watchlist_items` stores saved items per user and profile.
- **Backend Source**: Supabase PostgREST table `watchlist_items`.
- **Classification**:
  - **A**: `public.watchlist_items` table and RLS policies (007_watchlist_and_profile_age.sql).
  - **B**: Watchlist CRUD API contract, item data structure.
  - **C**: Web watchlist page.
  - **D**: Supabase database.
  - **E**: None.
  - **F**: Watchlist tab in Android TV navigation and "Add/Remove from Watchlist" button on detail screens.

---

## 3. Erasmus TV Design Language System

| Element | Web Specification | Android TV Translation |
|---|---|---|
| **Background** | Pitch Black (`#000000`, `hsl(0,0%,0%)`) | `Color(0xFF000000)` OLED-true black |
| **Card Surface** | Near-black (`#0A0A0A`, `hsl(0,0%,4%)`) | `Color(0xFF0D0D0D)` elevated surface |
| **Borders** | Hairline (`hsl(0,0%,14%)`) | `1.dp` border `Color(0xFF262626)` |
| **Primary Accent** | Electric Blue (`#1D90F5`) | `Color(0xFF1D90F5)` glow & active tokens |
| **Hot Accent** | Electric Hot (`#38BDF8`) | `Color(0xFF38BDF8)` spark & highlight |
| **Nav Active** | Active Deep Blue (`#0969DA`) | `Color(0xFF0969DA)` selected tab/filter |
| **Typography** | Bostone wordmark + Sans display | Custom Erasmus TV Wordmark + Clean Modern Sans |
| **Focus State** | Hover glow & border | **Scale 1.06x**, `2.dp` border `#1D90F5`, focus glow |
| **Navigation** | Top header + sidebar | **TV Navigation Drawer / Top Rail**: Home, Movies, TV, Search, Watchlist, Profiles |

---

## 4. Native Android TV Project Architecture

```
ErasmusTV/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/erasmustv/app/
│           │   ├── ErasmusTvApplication.kt
│           │   ├── core/
│           │   │   ├── config/AppConfig.kt
│           │   │   ├── network/
│           │   │   │   ├── SupabaseAuthInterceptor.kt
│           │   │   │   └── NetworkClient.kt
│           │   │   └── theme/
│           │   │       ├── Color.kt
│           │   │       ├── Theme.kt
│           │   │       └── Type.kt
│           │   ├── data/
│           │   │   ├── model/
│           │   │   │   ├── MediaModels.kt
│           │   │   │   ├── ProfileModels.kt
│           │   │   │   ├── StreamModels.kt
│           │   │   │   └── AuthModels.kt
│           │   │   ├── local/
│           │   │   │   ├── SessionManager.kt
│           │   │   │   └── PlaybackProgressStore.kt
│           │   │   ├── remote/
│           │   │   │   ├── TmdbApiService.kt
│           │   │   │   ├── SupabaseApiService.kt
│           │   │   │   ├── ErasmusStreamApiService.kt
│           │   │   │   └── OmdbApiService.kt
│           │   │   └── repository/
│           │   │       ├── AuthRepository.kt
│           │   │       ├── ProfileRepository.kt
│           │   │       ├── MediaRepository.kt
│           │   │       ├── WatchlistRepository.kt
│           │   │       └── StreamRepository.kt
│           │   └── ui/
│           │       ├── components/
│           │       │   ├── TvFocusableCard.kt
│           │       │   ├── MediaPosterCard.kt
│           │       │   ├── HeroBillboard.kt
│           │       │   ├── MediaSectionRow.kt
│           │       │   └── TvNavBar.kt
│           │       ├── screens/
│           │       │   ├── auth/LoginScreen.kt
│           │       │   ├── profiles/ProfilePickerScreen.kt
│           │       │   ├── home/HomeScreen.kt
│           │       │   ├── movies/MoviesScreen.kt
│           │       │   ├── tv/TvShowsScreen.kt
│           │       │   ├── details/MediaDetailScreen.kt
│           │       │   ├── search/SearchScreen.kt
│           │       │   ├── watchlist/WatchlistScreen.kt
│           │       │   └── player/TvPlayerScreen.kt
│           │       ├── navigation/AppNavigation.kt
│           │       └── MainActivity.kt
│           └── res/
│               ├── drawable/
│               ├── values/
│               └── mipmap-*/
├── gradle/
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── ANDROID_TV_MIGRATION_PLAN.md
```

---

## 5. Execution Phasing

1. **Phase 1**: Preserve foundation & contracts in Kotlin data models, network client, and repositories.
2. **Phase 2**: Implement native Android project files, Gradle build configuration, and Android TV manifest.
3. **Phase 3**: Implement Erasmus Design System for TV (Theme, Colors, TV focusable components, glow borders).
4. **Phase 4**: Build TV Navigation, Screens (Profiles, Home, Movies, TV, Details, Search, Watchlist).
5. **Phase 5**: Build Native ExoPlayer with D-pad remote controls, resume support, subtitles, and server switching.
6. **Phase 6**: Remove all legacy web application files (Next.js, Tailwind, React, Vercel).
7. **Phase 7**: Validation, compile check, and verification.
