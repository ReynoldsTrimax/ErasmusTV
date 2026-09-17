package com.erasmustv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.erasmustv.app.core.theme.BorderHairline
import com.erasmustv.app.core.theme.TextPrimary
import com.erasmustv.app.core.theme.getAvatarGradient

@Composable
fun ProfileAvatarView(
    avatarKey: String,
    profileName: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
    iconSize: Dp = 48.dp
) {
    val isCustom = avatarKey.startsWith("avatar_") || avatarKey.endsWith(".png")
    if (isCustom) {
        AsyncImage(
            model = "file:///android_asset/avatars/$avatarKey",
            contentDescription = profileName,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    } else {
        val gradient = getAvatarGradient(avatarKey)
        Box(
            modifier = modifier
                .clip(shape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = profileName,
                tint = TextPrimary,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
