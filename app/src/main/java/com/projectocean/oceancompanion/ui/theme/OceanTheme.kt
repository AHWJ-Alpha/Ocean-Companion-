package com.projectocean.oceancompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightOceanColors = lightColorScheme(
    primary = Color(0xFF0E6FFF),
    onPrimary = Color.White,
    secondary = Color(0xFF00A6A6),
    tertiary = Color(0xFFFFB23F),
    background = Color(0xFFF7FBFF),
    surface = Color.White,
    surfaceVariant = Color(0xFFEAF2FA),
    onBackground = Color(0xFF122033),
    onSurface = Color(0xFF122033),
    onSurfaceVariant = Color(0xFF526071),
    outline = Color(0xFFB8C6D6)
)

private val DarkOceanColors = darkColorScheme(
    primary = Color(0xFF8AB4FF),
    onPrimary = Color(0xFF082A55),
    secondary = Color(0xFF75D6D2),
    tertiary = Color(0xFFFFC66D),
    background = Color(0xFF0B1018),
    surface = Color(0xFF111A24),
    surfaceVariant = Color(0xFF1B2733),
    onBackground = Color(0xFFE7EEF8),
    onSurface = Color(0xFFE7EEF8),
    onSurfaceVariant = Color(0xFFA9B7C8),
    outline = Color(0xFF40566C)
)

@Composable
fun OceanTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkOceanColors else LightOceanColors
    MaterialTheme(colorScheme = colors, content = content)
}
