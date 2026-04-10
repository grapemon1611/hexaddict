package com.almostbrilliantideas.hexaddict.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B84D4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D3A5C),
    onPrimaryContainer = Color(0xFFE8E6F0),

    secondary = Color(0xFF4DB893),
    onSecondary = Color.White,

    background = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8E6F0),

    surface = Color(0xFF252540),
    onSurface = Color(0xFFE8E6F0),
    surfaceVariant = Color(0xFF2D2D4A),
    onSurfaceVariant = Color(0xFFB8B4C4),

    outline = Color(0xFF4A4A6A)
)

@Composable
fun HexAddictTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
