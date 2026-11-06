package com.digimovie.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DigiGold,
    secondary = DigiGoldLight,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceHighlight,
    onPrimary = BgDark,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun DigiMovieTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
