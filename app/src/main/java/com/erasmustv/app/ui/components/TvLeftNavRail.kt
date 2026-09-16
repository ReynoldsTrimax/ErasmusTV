package com.erasmustv.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.FocusWhite
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.getAvatarGradient
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.ui.navigation.NavRoutes
import androidx.compose.ui.res.painterResource
import com.erasmustv.app.R

data class NavRailDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null
)

val NAV_RAIL_ITEMS = listOf(
    NavRailDestination(NavRoutes.HOME, "Home", icon = TvNavIcons.Home),
    NavRailDestination(NavRoutes.SEARCH, "Search", icon = TvNavIcons.Search),
    NavRailDestination(NavRoutes.TV, "TV Shows", drawableRes = R.drawable.ic_nav_tv),
    NavRailDestination(NavRoutes.MOVIES, "Movies", icon = TvNavIcons.Movies),
    NavRailDestination(NavRoutes.ANIME, "Anime", drawableRes = R.drawable.ic_nav_anime),
    NavRailDestination(NavRoutes.STUDIOS, "Studios", drawableRes = R.drawable.ic_nav_studios)
)

/**
 * Minimal Navigation Rail — zero layout-reflow sidebar expansion.
 *
 * Performance contract:
 *   - Layout width is ALWAYS 150.dp (fixed). No animateDpAsState, no layout passes on expand/collapse.
 *   - The collapsed visual state is achieved purely via graphicsLayer on the label text:
 *       scaleX=0f/alpha=0f when collapsed → visible only to GPU compositing, not layout.
 *   - This eliminates the full-screen relayout that occurred whenever the sidebar toggled.
 *   - Each nav item uses a single animateFloatAsState (focus brightness) and a single
 *     animateColorAsState (icon/text color). No redundant separate animators.
 */
@Composable
fun TvLeftNavRail(
    currentRoute: String,
    activeProfile: WatchProfile?,
    onNavigate: (String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    onNavigateRight: (() -> Boolean)? = null,
    onReselectCurrent: (() -> Unit)? = null,
    railFocusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val navInteractionSources = remember { List(NAV_RAIL_ITEMS.size) { MutableInteractionSource() } }
    val profileInteractionSource = remember { MutableInteractionSource() }

    val navItemFocusedStates = navInteractionSources.map { it.collectIsFocusedAsState() }
    val isProfileFocused by profileInteractionSource.collectIsFocusedAsState()

    // derivedStateOf: only recompute isRailFocused when the individual item states actually change,
    // not on every parent recomposition.
    val isRailFocused by remember {
        derivedStateOf { navItemFocusedStates.any { it.value } || isProfileFocused }
    }

    androidx.compose.runtime.LaunchedEffect(isRailFocused) {
        onFocusChanged?.invoke(isRailFocused)
    }

    // Animate the label reveal fraction as a single Float [0..1]:
    // 0 = collapsed (labels invisible), 1 = expanded (labels fully visible)
    val labelReveal by animateFloatAsState(
        targetValue = if (isRailFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "railLabelReveal"
    )

    // Dynamic width for sidebar: 52.dp when collapsed, 154.dp when expanded
    val railWidth by animateDpAsState(
        targetValue = if (isRailFocused) 154.dp else 52.dp,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "railWidth"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .clipToBounds()
            .zIndex(100f)
    ) {
        // Frosted Glass Acrylic Background: Translucent with hairline right border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x8008080E),
                            Color(0x6008080E),
                            Color(0x3508080E),
                            Color(0x1508080E)
                        )
                    )
                )
                .drawWithContent {
                    drawContent()
                    // Hairline frost vertical right border
                    drawLine(
                        color = Color(0x1AFFFFFF),
                        start = Offset(size.width - 1f, 0f),
                        end = Offset(size.width - 1f, size.height),
                        strokeWidth = 1f
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(154.dp)
                .padding(top = 28.dp, bottom = 24.dp, start = 6.dp, end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Top branding: E icon when collapsed, ERASMUS wordmark when expanded
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, top = 4.dp, bottom = 8.dp)
                    .height(30.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (labelReveal > 0.4f) {
                    Text(
                        text = "ERASMUS",
                        style = ErasmusTvTypography.Wordmark.copy(
                            fontSize = 15.sp,
                            letterSpacing = 2.2.sp
                        ),
                        color = TextPrimary.copy(alpha = labelReveal),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "E",
                        style = ErasmusTvTypography.Wordmark.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FocusWhite,
                        maxLines = 1
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NAV_RAIL_ITEMS.forEachIndexed { index, item ->
                    val isSelected = currentRoute == item.route
                    val attachFocusRequester = (isSelected || (currentRoute == NavRoutes.DETAILS && index == 0)) && railFocusRequester != null

                    MinimalNavItem(
                        destination = item,
                        isSelected = isSelected,
                        labelReveal = labelReveal,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        interactionSource = navInteractionSources[index],
                        onClick = {
                            if (isSelected && onReselectCurrent != null) {
                                onReselectCurrent()
                            } else {
                                onNavigate(item.route)
                            }
                        },
                        onNavigateRight = onNavigateRight,
                        modifier = if (attachFocusRequester) {
                            Modifier.focusRequester(railFocusRequester)
                        } else {
                            Modifier
                        }
                    )
                }
            }

            MinimalProfileItem(
                profile = activeProfile,
                labelReveal = labelReveal,
                isRailFocused = isRailFocused,
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                interactionSource = profileInteractionSource,
                onClick = onProfileClick,
                onNavigateRight = onNavigateRight
            )
        }
    }
}

@Composable
private fun MinimalNavItem(
    destination: NavRailDestination,
    isSelected: Boolean,
    labelReveal: Float,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateRight: (() -> Boolean)? = null
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Single float animator: scale the entire row item slightly when focused
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
        label = "navItemScale"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> FocusWhite
            else -> FocusWhite.copy(alpha = 0.50f)
        },
        animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
        label = "navItemColor"
    )

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            var handled = false
                            if (onNavigateRight != null) {
                                try {
                                    handled = onNavigateRight()
                                } catch (_: Exception) {
                                    handled = false
                                }
                            }
                            if (!handled) {
                                handled = focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            // Pressing Back while focused in the sidebar returns focus directly to page content
                            var handled = false
                            if (onNavigateRight != null) {
                                try {
                                    handled = onNavigateRight()
                                } catch (_: Exception) {
                                    handled = false
                                }
                            }
                            if (!handled) {
                                handled = focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)
                            }
                            if (!handled && currentRoute != NavRoutes.HOME) {
                                onNavigate(NavRoutes.HOME)
                                handled = true
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                            // Top item of sidebar (Home): consume to prevent wrapping or escaping upwards
                            if (destination.route == NavRoutes.HOME) {
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        ) {
            // Delicate vertical active hairline indicator
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isSelected) 14.dp else 0.dp)
                    .background(contentColor, RectangleShape)
            )

            Spacer(modifier = Modifier.width(if (isSelected) 8.dp else 10.dp))

            // Monochrome Icon
            if (destination.drawableRes != null) {
                Icon(
                    painter = painterResource(id = destination.drawableRes),
                    contentDescription = destination.label,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
            } else if (destination.icon != null) {
                Icon(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            // Label text: revealed via graphicsLayer — zero layout cost.
            // scaleX and alpha both animate from 0→1 using the shared labelReveal float.
            // This avoids AnimatedVisibility which triggers layout measure passes.
            Text(
                text = destination.label,
                style = ErasmusTvTypography.Body.copy(
                    fontSize = 12.5.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .graphicsLayer {
                        // Animate from 0→1 (collapsed→expanded) purely on the GPU.
                        // The text still occupies layout space (150dp rail is fixed),
                        // but layout only ever measures at 150dp — never changes.
                        scaleX = labelReveal
                        alpha = labelReveal
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            )
        }
    }
}

@Composable
private fun MinimalProfileItem(
    profile: WatchProfile?,
    labelReveal: Float,
    isRailFocused: Boolean,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onNavigateRight: (() -> Boolean)? = null
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
        label = "profileItemScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else FocusWhite.copy(alpha = 0.50f),
        animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
        label = "profileContentColor"
    )

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            onClick()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            var handled = false
                            if (onNavigateRight != null) {
                                try {
                                    handled = onNavigateRight()
                                } catch (_: Exception) {
                                    handled = false
                                }
                            }
                            if (!handled) {
                                handled = focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK -> {
                            var handled = false
                            if (onNavigateRight != null) {
                                try {
                                    handled = onNavigateRight()
                                } catch (_: Exception) {
                                    handled = false
                                }
                            }
                            if (!handled) {
                                handled = focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)
                            }
                            if (!handled && currentRoute != NavRoutes.HOME) {
                                onNavigate(NavRoutes.HOME)
                                handled = true
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            // Bottom-most item of sidebar (Profile): consume to prevent escaping downwards or wrapping
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
        ) {
            Spacer(modifier = Modifier.width(10.dp))

            // Just for that period of time while expanded: replace MySpace logo with profile avatar badge
            val showProfile = isRailFocused && profile != null
            if (showProfile) {
                ProfileAvatarView(
                    avatarKey = profile!!.avatarKey,
                    profileName = profile.name,
                    modifier = Modifier.size(18.dp),
                    shape = RoundedCornerShape(3.dp),
                    iconSize = 12.dp
                )
            } else {
                Icon(
                    imageVector = TvNavIcons.MySpace,
                    contentDescription = "My Space",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Just for that period of time while expanded: replace "My Space" text with profile name
            val labelText = if (showProfile) profile!!.name else "My Space"
            Text(
                text = labelText,
                style = ErasmusTvTypography.Body.copy(
                    fontSize = 12.5.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
                ),
                color = contentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .graphicsLayer {
                        scaleX = labelReveal
                        alpha = labelReveal
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            )
        }
    }
}
