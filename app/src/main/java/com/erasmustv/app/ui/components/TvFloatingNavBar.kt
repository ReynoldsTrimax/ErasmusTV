package com.erasmustv.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.erasmustv.app.R
import com.erasmustv.app.core.theme.ErasmusDimens
import com.erasmustv.app.core.theme.ErasmusElevation
import com.erasmustv.app.core.theme.ErasmusRadius
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.LocalAccentColor
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.TvMotion
import com.erasmustv.app.core.theme.TvSpring
import com.erasmustv.app.core.theme.dpSpec
import com.erasmustv.app.core.theme.floatSpec
import com.erasmustv.app.core.theme.rememberReducedMotion
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.ui.navigation.NavRoutes

/**
 * A destination rendered inside the floating nav pill.
 *
 * [showLabel] distinguishes primary destinations (rendered as text labels)
 * from secondary actions (rendered icon-only) so the pill doesn't overcrowd.
 */
data class FloatingNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null,
    val showLabel: Boolean = true
)

/** Primary destinations — rendered as text labels in the pill. */
val FLOATING_NAV_PRIMARY = listOf(
    FloatingNavDestination(NavRoutes.HOME, "Home"),
    FloatingNavDestination(NavRoutes.TV, "TV Shows"),
    FloatingNavDestination(NavRoutes.MOVIES, "Movies"),
    FloatingNavDestination(NavRoutes.ANIME, "Anime")
)

/** Secondary actions — rendered icon-only to keep the pill uncluttered. */
val FLOATING_NAV_SECONDARY = listOf(
    FloatingNavDestination(
        NavRoutes.SEARCH, "Search",
        icon = TvNavIcons.Search, showLabel = false
    ),
    FloatingNavDestination(
        NavRoutes.WATCHLIST, "Watch List",
        icon = Icons.Rounded.BookmarkBorder, showLabel = false
    )
)

/**
 * Where the selection indicator was last parked, carried across a screen swap.
 *
 * Every screen mounts its own [TvFloatingNavBar], so a route change destroys the
 * bar and rebuilds it with the indicator already sitting under the new tab —
 * there is no local state left to animate *from*. This one field remembers the
 * previously-selected route so the freshly-built bar can start the indicator at
 * the old tab's slot and travel to the new one, which is the whole point of the
 * motion: it says *which* tab you came from, not merely which one you're on.
 *
 * Purely cosmetic and intentionally not persisted — if the process dies the
 * indicator simply appears in place, which is the correct fallback.
 */
private object NavIndicatorMemory {
    var route: String? = null
}

/**
 * ERASMUS FLOATING NAVIGATION
 *
 * A centered, pill-shaped navigation bar that floats near the top of the
 * screen above page content (including the hero billboard) instead of
 * reserving a permanent vertical strip of the layout like the previous left
 * sidebar did.
 *
 * Visual character: a mostly-opaque near-black pill with a very subtle
 * border and a soft cinematic shadow. Deliberately *not* glassmorphism —
 * there is no true backdrop blur (not viable at minSdk 26, and a performance
 * trap on low-end TV chipsets); depth comes from layered translucency and
 * shadow instead.
 *
 * D-pad contract (mirrors the old rail's contract so screens wire in the
 * same way):
 *  - LEFT / RIGHT move between nav items; edges are consumed (no wrap).
 *  - DOWN leaves the nav and enters page content via [onNavigateIntoContent].
 *  - UP is consumed (nothing lives above the nav).
 *  - CENTER / ENTER activates; re-selecting the active route calls
 *    [onReselectCurrent] instead of re-navigating.
 *  - BACK is not handled here at all; it falls through to the hosting
 *    screen's BackHandler, which is the only code that knows whether this
 *    screen should exit the app or pop to Home.
 *
 * @param isCompact when true the pill shrinks slightly (used while the page
 *   is scrolled). It never hides — navigation stays reachable at all times.
 * @param navFocusRequester attached to the currently-active item so screens
 *   can move focus into the nav.
 * @param onNavigateIntoContent invoked on DOWN/BACK; return true if the
 *   screen successfully moved focus into its content.
 */
@Composable
fun TvFloatingNavBar(
    currentRoute: String,
    activeProfile: WatchProfile?,
    onNavigate: (String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onNavigateIntoContent: (() -> Boolean)? = null,
    onReselectCurrent: (() -> Unit)? = null,
    navFocusRequester: FocusRequester? = null
) {
    val isReducedMotion = rememberReducedMotion()
    val accent = LocalAccentColor.current

    val allItems = remember { FLOATING_NAV_PRIMARY + FLOATING_NAV_SECONDARY }
    // +1 trailing slot for the profile action.
    val interactionSources = remember(allItems.size) {
        List(allItems.size + 1) { MutableInteractionSource() }
    }
    val focusStates = interactionSources.map { it.collectIsFocusedAsState() }
    val isNavFocused by remember { derivedStateOf { focusStates.any { it.value } } }

    LaunchedEffect(isNavFocused) {
        onFocusChanged?.invoke(isNavFocused)
    }

    val motionDuration = if (isReducedMotion) 0 else TvMotion.DURATION_MEDIUM

    // ------------------------------------------------------------------
    // The pill is a FIXED size. It does not shrink while scrolling any more.
    //
    // The scroll-driven compact toggle animated the pill's height (and re-sized
    // every child, the profile avatar included) the instant the feed returned to
    // the top — read as the whole bar "stretching" vertically, and it distorted
    // the profile logo mid-transition. A navigation bar that changes dimensions
    // under the user is more distracting than the tiny space it reclaims, so the
    // geometry is now constant regardless of scroll position.
    // ------------------------------------------------------------------
    val pillHeight = ErasmusDimens.NavPillHeight
    val topMargin = ErasmusDimens.NavPillTopMargin

    // The pill gains a little presence only when it holds focus — a change in
    // translucency, never in size.
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (isNavFocused) 0.97f else 0.90f,
        animationSpec = tween(motionDuration, easing = TvMotion.EasingSilk),
        label = "navPillSurfaceAlpha"
    )

    val pillShape = RoundedCornerShape(ErasmusRadius.NavPillLarge)
    val focusManager = LocalFocusManager.current

    // ------------------------------------------------------------------
    // Selection indicator: ONE capsule, shared by every tab, that travels to
    // whichever route is active.
    //
    // It replaces a per-item background that cross-faded out under the old tab
    // and in under the new one — a state change with no continuity, so the eye
    // had nothing to follow. A single object that moves reads as the selection
    // itself relocating.
    //
    // Deliberately no bounce. The house rule in TvMotion is that overshoot is
    // reserved for motion the user gave momentum to; a nav bar that wobbles
    // every time a page is picked is a tic, and at ten feet on a large panel an
    // oscillating chrome element is far louder than it looks on a laptop.
    //
    // Geometry is measured, not assumed: tab widths differ ("Home" vs "TV
    // Shows"), and the icon-only items are a different size again, so the
    // indicator resizes as it moves. All four edges are animated in the draw
    // phase only — no layout or recomposition per frame.
    // ------------------------------------------------------------------
    var rowCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val slots = remember { mutableStateMapOf<String, Rect>() }
    // Captured once: reading this during composition (rather than tracking it)
    // is what lets the bar animate away from the route it replaced.
    val entryRoute = remember { NavIndicatorMemory.route }

    val targetSlot = slots[currentRoute]
    val entrySlot = entryRoute
        ?.takeIf { it != currentRoute }
        ?.let { slots[it] }

    // Held at the entry slot for exactly one frame so the spring has somewhere
    // to travel from; after that the target is the live route.
    var hasDeparted by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute, targetSlot != null) {
        if (targetSlot == null) return@LaunchedEffect
        withFrameNanos { }
        hasDeparted = true
        NavIndicatorMemory.route = currentRoute
    }

    val indicatorSlot = when {
        targetSlot == null -> null
        !hasDeparted && entrySlot != null -> entrySlot
        else -> targetSlot
    }
    val slotRect = indicatorSlot ?: Rect.Zero
    val indicatorSpec = TvSpring.Reposition.floatSpec(isReducedMotion)
    val indicatorLeft by animateFloatAsState(
        targetValue = slotRect.left,
        animationSpec = indicatorSpec,
        label = "navIndicatorLeft"
    )
    val indicatorTop by animateFloatAsState(
        targetValue = slotRect.top,
        animationSpec = indicatorSpec,
        label = "navIndicatorTop"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = slotRect.width,
        animationSpec = indicatorSpec,
        label = "navIndicatorWidth"
    )
    val indicatorHeight by animateFloatAsState(
        targetValue = slotRect.height,
        animationSpec = indicatorSpec,
        label = "navIndicatorHeight"
    )
    // Fades rather than pops when the indicator has no business being visible —
    // on Details, for instance, where no tab is the active route.
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (targetSlot != null) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 0 else TvMotion.DURATION_FAST,
            easing = TvMotion.EasingSilk
        ),
        label = "navIndicatorAlpha"
    )

    /** Shared DOWN/BACK escape used by every item in the pill. */
    val escapeIntoContent: () -> Boolean = {
        var handled = false
        if (onNavigateIntoContent != null) {
            handled = try {
                onNavigateIntoContent()
            } catch (_: Exception) {
                false
            }
        }
        if (!handled) {
            handled = try {
                focusManager.moveFocus(FocusDirection.Down)
            } catch (_: Exception) {
                false
            }
        }
        handled
    }

    Box(
        modifier = modifier
            .padding(top = topMargin)
            .zIndex(100f)
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = ErasmusDimens.NavPillMaxWidth)
                .height(pillHeight)
                .shadow(
                    elevation = ErasmusElevation.Overlay,
                    shape = pillShape,
                    ambientColor = ErasmusElevation.ShadowAmbientColor,
                    spotColor = ErasmusElevation.ShadowSpotColor
                )
                // Mostly-opaque near-black fill with a barely-there vertical
                // sheen so the pill reads as a solid object, not frosted glass.
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF121214).copy(alpha = surfaceAlpha),
                            Color(0xFF0A0A0B).copy(alpha = surfaceAlpha)
                        )
                    ),
                    shape = pillShape
                )
                .border(
                    width = 1.dp,
                    color = if (isNavFocused) {
                        accent.copy(alpha = 0.22f)
                    } else {
                        FocusWhite.copy(alpha = 0.08f)
                    },
                    shape = pillShape
                )
                .padding(horizontal = ErasmusDimens.NavPillHorizontalPadding)
                // These two sit adjacent and after the padding on purpose: with
                // no layout modifier between them they share one coordinator, so
                // the slots reported here and the rect drawn below are guaranteed
                // to be in the same coordinate space.
                .onGloballyPositioned { rowCoords = it }
                .drawBehind {
                    if (indicatorAlpha <= 0.01f || indicatorWidth <= 1f) return@drawBehind
                    drawRoundRect(
                        color = FocusWhite.copy(alpha = 0.10f * indicatorAlpha),
                        topLeft = Offset(indicatorLeft, indicatorTop),
                        size = Size(indicatorWidth, indicatorHeight),
                        // Clamped to a true stadium so the corner radius can't
                        // read differently from the focus capsule drawn by the item.
                        cornerRadius = CornerRadius(
                            minOf(ErasmusRadius.NavPill.toPx(), indicatorHeight / 2f)
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ErasmusDimens.NavPillItemSpacing)
        ) {
            // --- Wordmark -------------------------------------------------
            Text(
                text = "ERASMUS",
                style = ErasmusTvTypography.Wordmark.copy(
                    fontSize = if (isCompact) 10.sp else 11.5.sp,
                    letterSpacing = 1.8.sp
                ),
                color = TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(10.dp))

            // --- Destinations ---------------------------------------------
            allItems.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route
                val isFirst = index == 0
                // When the current route isn't represented in the nav (e.g.
                // Details), anchor the screen's focus requester to the first
                // item so moving focus into the nav lands somewhere sensible.
                val isFocusAnchor = isSelected ||
                    (isFirst && allItems.none { it.route == currentRoute })
                FloatingNavItem(
                    destination = item,
                    isSelected = isSelected,
                    // Fixed size: the bar never renders a compact variant.
                    isCompact = false,
                    accent = accent,
                    interactionSource = interactionSources[index],
                    onClick = {
                        if (isSelected && onReselectCurrent != null) {
                            onReselectCurrent()
                        } else {
                            onNavigate(item.route)
                        }
                    },
                    onEscapeIntoContent = escapeIntoContent,
                    consumeLeft = isFirst,
                    consumeRight = false,
                    // Reported from outside the item's focus-scale layer, so a
                    // focused tab can't drag the indicator's geometry with it.
                    onSlotMeasured = { coords ->
                        val row = rowCoords
                        if (row != null && row.isAttached && coords.isAttached) {
                            val measured = Rect(
                                offset = row.localPositionOf(coords, Offset.Zero),
                                size = coords.size.toSize()
                            )
                            // Equality-guarded: layout runs far more often than
                            // the slots actually change, and an unconditional
                            // write here would invalidate composition every pass.
                            if (slots[item.route] != measured) {
                                slots[item.route] = measured
                            }
                        }
                    },
                    modifier = if (isFocusAnchor && navFocusRequester != null) {
                        Modifier.focusRequester(navFocusRequester)
                    } else {
                        Modifier
                    }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // --- Profile (trailing) ---------------------------------------
            FloatingNavProfileItem(
                profile = activeProfile,
                isCompact = false,
                accent = accent,
                interactionSource = interactionSources.last(),
                onClick = onProfileClick,
                onEscapeIntoContent = escapeIntoContent,
                // Right-most element: consume RIGHT so focus can't fall out.
                consumeRight = true
            )
        }
    }
}

@Composable
private fun FloatingNavItem(
    destination: FloatingNavDestination,
    isSelected: Boolean,
    isCompact: Boolean,
    accent: Color,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onEscapeIntoContent: () -> Boolean,
    modifier: Modifier = Modifier,
    consumeLeft: Boolean = false,
    consumeRight: Boolean = false,
    onSlotMeasured: ((LayoutCoordinates) -> Unit)? = null
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()
    val duration = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) TvMotion.FocusScaleSubtle else 1.0f,
        animationSpec = TvSpring.FocusFast.floatSpec(isReducedMotion),
        label = "navItemScale"
    )

    // Inactive items are muted; focus brightens them. Neither state is neon.
    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> FocusWhite
            isSelected -> FocusWhite.copy(alpha = 0.92f)
            else -> FocusWhite.copy(alpha = 0.46f)
        },
        animationSpec = tween(duration, easing = TvMotion.EasingSilk),
        label = "navItemColor"
    )

    // Focus adds a faint accent-tinted wash. Selection is NOT drawn here any
    // more — the shared travelling indicator in the parent owns that state, and
    // a second capsule under the active tab would double it.
    val capsuleColor by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(duration, easing = TvMotion.EasingSilk),
        label = "navItemCapsule"
    )

    val capsuleShape = RoundedCornerShape(ErasmusRadius.NavPill)
    val horizontalPadding = if (destination.showLabel) {
        if (isCompact) 9.dp else 11.dp
    } else {
        if (isCompact) 7.dp else 8.dp
    }

    Box(
        modifier = modifier
            .then(
                if (onSlotMeasured != null) {
                    // First in the chain, so it sits outside the graphicsLayer
                    // below and reports the untransformed layout box.
                    Modifier.onGloballyPositioned(onSlotMeasured)
                } else Modifier
            )
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription =
                    "${destination.label}, navigation tab${if (isSelected) ", selected" else ""}"
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color = capsuleColor, shape = capsuleShape)
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 1.dp,
                        color = accent.copy(alpha = 0.45f),
                        shape = capsuleShape
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                    return@onKeyEvent false
                }
                when (keyEvent.nativeKeyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    android.view.KeyEvent.KEYCODE_ENTER,
                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        onClick()
                        true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        onEscapeIntoContent()
                        true
                    }
                    // Nothing lives above the nav — block wrap-around.
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> consumeLeft
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> consumeRight
                    // BACK is deliberately NOT handled here.
                    //
                    // It used to call onEscapeIntoContent(), which made BACK an
                    // unbreakable loop: the screen's own BackHandler sends focus
                    // from content up to the nav, and the nav sent it straight
                    // back down into content, so the user could never leave the
                    // page with BACK at all. Letting the event fall through means
                    // the hosting screen's BackHandler decides — which is the only
                    // place that knows whether this screen is a root that should
                    // exit the app or a secondary page that should pop to Home.
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = horizontalPadding, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (destination.showLabel) {
            Text(
                text = destination.label,
                style = ErasmusTvTypography.Body.copy(
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = when {
                        isFocused -> FontWeight.SemiBold
                        isSelected -> FontWeight.Medium
                        else -> FontWeight.Normal
                    }
                ),
                color = contentColor,
                maxLines = 1
            )
        } else {
            val iconSize = if (isCompact) 15.dp else ErasmusDimens.NavPillIconSize
            when {
                destination.drawableRes != null -> Icon(
                    painter = painterResource(id = destination.drawableRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
                destination.icon != null -> Icon(
                    imageVector = destination.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

@Composable
private fun FloatingNavProfileItem(
    profile: WatchProfile?,
    isCompact: Boolean,
    accent: Color,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onEscapeIntoContent: () -> Boolean,
    modifier: Modifier = Modifier,
    consumeRight: Boolean = false
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isReducedMotion = rememberReducedMotion()
    val duration = if (isReducedMotion) 0 else TvMotion.DURATION_FOCUS_FAST

    val scale by animateFloatAsState(
        targetValue = if (isFocused && !isReducedMotion) TvMotion.FocusScaleSubtle else 1.0f,
        animationSpec = tween(duration, easing = TvMotion.EasingSilk),
        label = "navProfileScale"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isFocused) accent.copy(alpha = 0.55f) else FocusWhite.copy(alpha = 0.16f),
        animationSpec = tween(duration, easing = TvMotion.EasingSilk),
        label = "navProfileRing"
    )

    val avatarSize = if (isCompact) 18.dp else 21.dp
    val shape = RoundedCornerShape(ErasmusRadius.NavPill)

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = if (profile != null) {
                    "Switch profile, currently ${profile.name}"
                } else {
                    "Profiles"
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                    return@onKeyEvent false
                }
                when (keyEvent.nativeKeyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    android.view.KeyEvent.KEYCODE_ENTER,
                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                        onClick()
                        true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        onEscapeIntoContent()
                        true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> true
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> consumeRight
                    // BACK falls through to the hosting screen — see the note on
                    // FloatingNavItem.
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                // requiredSize, not size: the avatar keeps its square footprint
                // even if the Row is momentarily over-constrained, so the logo
                // can never be squeezed into an oval.
                .requiredSize(avatarSize)
                .border(width = 1.5.dp, color = ringColor, shape = shape),
            contentAlignment = Alignment.Center
        ) {
            if (profile != null) {
                ProfileAvatarView(
                    avatarKey = profile.avatarKey,
                    profileName = profile.name,
                    modifier = Modifier.requiredSize(avatarSize),
                    shape = shape,
                    iconSize = avatarSize * 0.6f
                )
            } else {
                Icon(
                    imageVector = TvNavIcons.MySpace,
                    contentDescription = null,
                    tint = FocusWhite.copy(alpha = if (isFocused) 1f else 0.5f),
                    modifier = Modifier.size(avatarSize * 0.7f)
                )
            }
        }
    }
}
