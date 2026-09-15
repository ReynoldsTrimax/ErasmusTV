package com.erasmustv.app.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val ErasmusDarkColorScheme = darkColorScheme(
    primary = FocusWhite,
    onPrimary = PitchBlack,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextPrimary,
    secondary = SurfaceElevated,
    onSecondary = TextPrimary,
    surface = PitchBlack,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    border = BorderHairline
)

@Composable
fun ErasmusTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ErasmusDarkColorScheme,
        content = content
    )
}
