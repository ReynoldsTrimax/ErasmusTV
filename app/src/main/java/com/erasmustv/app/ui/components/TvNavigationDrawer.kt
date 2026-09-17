package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.ElectricBlue
import com.erasmustv.app.core.theme.ErasmusTvTypography
import com.erasmustv.app.core.theme.PitchBlack
import com.erasmustv.app.core.theme.TextMuted
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.getAvatarGradient
import com.erasmustv.app.data.model.WatchProfile
import com.erasmustv.app.ui.navigation.NavRoutes

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val NAV_ITEMS = listOf(
    NavItem(NavRoutes.SEARCH, "Search", Icons.Default.Search),
    NavItem(NavRoutes.HOME, "Home", Icons.Default.Home),
    NavItem(NavRoutes.MOVIES, "Movies", Icons.Default.Movie),
    NavItem(NavRoutes.TV, "TV Shows", Icons.Default.Tv),
    NavItem(NavRoutes.WATCHLIST, "Watchlist", Icons.Default.Bookmark)
)

@Composable
fun TvTopBar(
    currentRoute: String,
    activeProfile: WatchProfile?,
    onNavigate: (String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PitchBlack)
            .padding(horizontal = 48.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ErasmusTV Signature Wordmark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = "E R A S M U S",
                    style = ErasmusTvTypography.Wordmark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(2.dp)
                        .background(ElectricBlue)
                )
            }
        }

        // Navigation Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NAV_ITEMS.forEach { item ->
                val isSelected = currentRoute == item.route
                TvFocusableCard(
                    onClick = { onNavigate(item.route) },
                    focusedScale = 1.0f,
                    shape = RectangleShape
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .background(
                                color = when {
                                    isSelected -> ElectricBlue.copy(alpha = 0.2f)
                                    isFocused -> ElectricBlue.copy(alpha = 0.12f)
                                    else -> PitchBlack
                                },
                                shape = RectangleShape
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = when {
                                isSelected -> ElectricBlue
                                isFocused -> TextPrimary
                                else -> TextMuted
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = item.label,
                            style = ErasmusTvTypography.Badge,
                            color = when {
                                isSelected -> TextPrimary
                                isFocused -> TextPrimary
                                else -> TextMuted
                            }
                        )
                    }
                }
            }

            // Profile Switcher Button
            TvFocusableCard(
                onClick = onProfileClick,
                focusedScale = 1.0f,
                shape = RectangleShape
            ) { isFocused ->
                Row(
                    modifier = Modifier
                        .background(
                            if (isFocused) ElectricBlue.copy(alpha = 0.2f) else PitchBlack,
                            RectangleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileAvatarView(
                        avatarKey = activeProfile?.avatarKey ?: "slate",
                        profileName = activeProfile?.name ?: "Profile",
                        modifier = Modifier.size(24.dp),
                        shape = RectangleShape,
                        iconSize = 14.dp
                    )
                    Text(
                        text = activeProfile?.name ?: "Profile",
                        style = ErasmusTvTypography.Badge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
