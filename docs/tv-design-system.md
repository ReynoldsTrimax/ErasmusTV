# ErasmusTV — Android TV Design System & Remote Interaction Guide

This document defines the visual design system, focus architecture, skeleton loading states, accessibility requirements, and verification workflows for ErasmusTV on Android TV.

---

## 1. Visual Design Tokens

All tokens are defined in [`Color.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/core/theme/Color.kt) and [`Type.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/core/theme/Type.kt).

### Surface & Background Tokens
- **Canvas (`PitchBlack`)**: `#000000` (Pure OLED black)
- **Base Surface (`SurfaceDark`)**: `#0D0D0D`
- **Elevated Surfaces (`SurfaceElevated`)**: `#141414`
- **Card Background (`SurfaceCard`)**: `#121212`
- **Card Borders (`SurfaceCardBorder` / `BorderHairline`)**: `#242424` / `#1F1F1F`

### Focus Tokens
- **Focus Indicator (`FocusWhite`)**: `#FFFFFF`
- **Default Focus Border Width**: `1.5.dp` (2.dp on primary hero actions)
- **Focused Scale**: `1.0f` (Strict 0% magnification across grid, row, and list items to preserve layout geometry on 10-foot screens)
- **Geometry**: `RectangleShape` (Sharp, architectural styling across all posters and cards)

### Accent & Metadata Tokens
- **Brand Accent (`BrandAccent`)**: `#FFFFFF`
- **Rating Gold (`RatingGold`)**: `#FBBF24`
- **Match Score (`MatchGreen`)**: `#46D369`
- **Top 10 / Error Accent**: `#E50914` / `#EF4444`

### 10-Foot Typography Scale
On living-room displays viewed from 7–10 feet, sub-11sp text becomes illegible. The typographic scale enforces a minimum of 11–12sp for all readable metadata:
- **PageTitle**: 34sp, line height 40sp, Bold weight, tracking `-0.6sp`
- **HeroTitleLarge**: 32sp, line height 38sp, Black weight, tracking `-0.6sp`
- **BillboardTitle**: 24sp, line height 30sp, Bold weight, tracking `-0.3sp`
- **SectionTitle**: 24sp, line height 30sp, SemiBold weight, tracking `-0.3sp`
- **CardTitle**: 14sp, line height 18sp, Medium weight, tracking `-0.1sp`
- **CardMeta**: 12.5sp, line height 16sp, Medium weight, tracking `+0.2sp`
- **Body / BodyLarge**: 12.5sp / 13.5sp, line height 18sp / 19sp, tracking `+0.2sp`
- **Badge / ButtonText**: 12sp / 13sp, Bold / SemiBold weight
- **BrandBadge**: 12.5sp, Bostone typeface, 1.5sp letter spacing
- **HeroMeta / MatchScore**: 12.5sp / 12sp, Medium / Bold weight
- **CastSummary**: 12.5sp, line height 17sp, Normal weight, tracking `+0.2sp`
- **HeroRankNumber**: 80sp, Black weight, tracking `-5sp` (graphic numeral)
- **Top10Badge**: 11sp, Black weight, 0.4sp letter spacing

#### Tracking ladder
Letter spacing is a function of size, never one value across the scale: large type reads too loose as it grows, small type too tight across a room.

| Size band | Tracking | Example |
|---|---|---|
| ≥ 30sp | `−0.018em` | PageTitle 34sp → `−0.6sp` |
| 24–29sp | `−0.012em` | SectionTitle 24sp → `−0.3sp` |
| 14–18sp | `−0.007em` | CardTitle 14sp → `−0.1sp` |
| ≤ 13.5sp | `+0.015em` | Body 12.5sp → `+0.2sp` |

Display numerals (`HeroRankNumber`) and the Bostone wordmark sit outside the ladder — both are graphic marks, tracked by eye.

---

## 1a. Motion — Spring Vocabulary

Defined in [`Motion.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/core/theme/Motion.kt). Anything that **moves, lifts, or resizes** uses a spring; only opacity and colour use a duration curve.

Why: on TV the remote outruns the animation. A viewer holding D-pad right retargets the focus animation every ~80ms, and a duration-based tween restarts its interpolation on each retarget, so fast traversal visibly stutters. A spring continues from the value and velocity already on screen.

Motion is specified with two numbers — **damping ratio** (overshoot) and **response** (seconds to reach target, not a duration).

| Profile | Damping | Response | Compose stiffness | Used for |
|---|---|---|---|---|
| `TvSpring.FocusFast` | `1.0` | `0.25s` | ≈ 632 | Button, chip, nav-item, player-control focus |
| `TvSpring.Focus` | `1.0` | `0.30s` | ≈ 439 | Card and tile focus lift + shadow |
| `TvSpring.Reposition` | `1.0` | `0.40s` | ≈ 247 | Nav pill geometry, timeline track/thumb |
| `TvSpring.Sheet` | `0.8` | `0.30s` | ≈ 439 | Panels arriving from an edge (episode switcher, submenu) |
| `TvSpring.Momentum` | `0.8` | `0.40s` | ≈ 247 | Motion the user threw |

Rules:
- **Damping `1.0` by default.** Overshoot is reserved for momentum-carrying motion (a panel thrown in from an edge). A menu that merely appeared must not bounce.
- **Compose conversion**: `stiffness = ω²` where `ω = 2π / response` at unit mass — `TvMotion.stiffnessFor(response)`. Pinned by `MotionSpringTest`.
- **Symmetric paths.** A panel entering from the right exits to the right, on the same spring in both directions.
- **Reduced motion snaps.** `TvSpring.*.floatSpec(isReducedMotion)` / `dpSpec` / `offsetSpec` return `snap()` when the system animator scale is 0 — the state change still lands, it just doesn't travel.
- **Durations remain** (`TvMotion.DURATION_*`) for opacity crossfades, colour shifts, and the skeleton pulse only.

---

## 2. Remote D-Pad Focus Conventions

The application relies entirely on D-pad remote navigation with zero touch or mouse pointer dependencies.

### Core Component: `TvFocusableCard`
Located at [`TvFocusableCard.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/ui/components/TvFocusableCard.kt):
- Standard wrapper for any focusable interactive tile or card.
- Manages `MutableInteractionSource`, focus scale (defaults to `1.0f`), border drawing (`1.5.dp` `FocusWhite`), and click events.
- Handles D-pad click via key down interception (`Key.DirectionCenter`, `Key.Enter`, `Key.NumPadEnter`).

### Focus Rules
1. **No Accidental Recomposition Theft**:
   - Never call `requestFocus()` in an un-gated `LaunchedEffect` or during ongoing recompositions.
   - Use `rememberSaveable { mutableStateOf(false) }` to track initial focus request per screen entry.
2. **Deterministic Rail Escapes**:
   - Left-most items in rows, grids, and hero carousels intercept `Key.DirectionLeft` to hop focus directly to `railFocusRequester`.
   - The navigation rail (`TvLeftNavRail.kt`) provides `onNavigateRight` to return focus directly to the primary content item.
3. **Boundary Protection**:
   - Top-most rows prevent upward focus leaks; bottom-most rows prevent downward traps.
   - Empty states must always offer a visible, remote-operable call to action (e.g. "Explore Movies & Shows" in [`WatchlistScreen.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/ui/screens/watchlist/WatchlistScreen.kt)).
4. **Hero Auto-Advance Gating**:
   - Auto-advance carousel timers in [`HeroBillboard.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/ui/components/HeroBillboard.kt) must pause when any hero button is focused or when the hero billboard is scrolled out of view (`isAutoAdvanceEnabled = listState.firstVisibleItemIndex == 0`).

---

## 3. Resilient Loading & Skeleton States

Located at [`TvFeedSkeleton.kt`](file:///Users/paarthsharma/Developer/ErasmusTV/app/src/main/java/com/erasmustv/app/ui/components/TvFeedSkeleton.kt):
- Never display blank screens or indeterminate central spinning wheels during initial screen loads.
- Shimmer animations use lightweight infinite alpha transitions (`0.25f` to `0.65f` over 1100ms) with `FastOutSlowInEasing`.
- Skeleton variants mirror exact screen geometry:
  - **`TvFeedSkeleton`**: Hero billboard placeholder + two horizontal poster shelves (`HomeScreen`, `MoviesScreen`, `TvShowsScreen`, `AnimeScreen`).
  - **`TvGridSkeleton`**: Grid title placeholder + 5-column poster card grid (`WatchlistScreen`, `CategoriesScreen`, `StudiosScreen`).
  - **`TvDetailSkeleton`**: Full-screen backdrop placeholder + title, badge pills, overview lines, action buttons, and episode row (`MediaDetailScreen`).
  - **`TvSearchSkeleton`**: Dual-column layout with keyboard placeholders and search result grid (`SearchScreen`).

---

## 4. Accessibility & TalkBack Expectations

All TV components must provide structured, clear information to Android accessibility services and screen readers (TalkBack):

1. **Semantic Node Merging**:
   - Interactive composite containers use `.semantics(mergeDescendants = true)`.
   - Prevents TalkBack from reading individual internal elements (e.g., separate star icons, ratings, bullets, year texts) as fragmented, jarring audio stops.
2. **Explicit Semantic Roles**:
   - Cards and actionable controls declare `role = Role.Button`.
   - Navigation rail items, category filters, and season selectors declare `role = Role.Tab` with `selected = isSelected`.
3. **Structured Content Descriptions**:
   - **Poster Cards**: `"[Number {rank}, ]{title}, {Movie/TV Series}[, {year}][, rated {rating} stars]"`
   - **Continue Watching**: `"{title}[, Season {s} Episode {e}], {resumeLabel}"`
   - **Detail Screen Actions**: `"Play"`, `"Add to Watchlist"` / `"In List, click to remove from Watchlist"`, `"Back to previous screen"`
   - **Episode Cards**: `"Episode {number}, {name}[, {runtime} minutes]"`
   - **Search Keyboard**: `"Key {char}"`, `"Space key"`, `"Del key"`, `"Clear key"`
4. **Decorative Asset Nullification**:
   - Background artwork, poster images inside labeled cards, and decorative icons have `contentDescription = null` to prevent duplicate or misleading announcements.

---

## 5. Verification Commands

Run these standard commands from the repository root:

```bash
# 1. Compile Kotlin source code
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew compileDebugKotlin

# 2. Run unit tests
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew testDebugUnitTest

# 3. Assemble Debug APK
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug

# 4. Safe Static Configuration & Compiler Checks
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew checkKotlinGradlePluginConfigurationErrors compileDebugUnitTestKotlin compileDebugKotlin
```

---

## 6. Release Handoff & Acceptance Evidence

For the complete Phase 13 Release Candidate checklist, binary checksums, and the 35-scenario emulator regression matrix, see [`docs/release-handover.md`](file:///Users/paarthsharma/Developer/ErasmusTV/docs/release-handover.md).

