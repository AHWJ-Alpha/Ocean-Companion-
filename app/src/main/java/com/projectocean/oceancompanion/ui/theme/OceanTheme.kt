package com.projectocean.oceancompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.staticCompositionLocalOf
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

data class OceanAccentColors(
    val primary: Color = Color(0xFF0E6FFF),
    val secondary: Color = Color(0xFF00A6A6)
)

val LocalOceanAccent = staticCompositionLocalOf { OceanAccentColors() }

fun parseOceanColor(value: String, fallback: Color): Color = runCatching {
    val normalized = value.trim().removePrefix("#")
    val rgb = normalized.toLong(16)
    when (normalized.length) {
        6 -> Color(0xFF000000 or rgb)
        8 -> Color(rgb)
        else -> fallback
    }
}.getOrDefault(fallback)

private fun animeLightColors(primary: Color, secondary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    secondary = secondary,
    tertiary = Color(0xFFFFD166),
    background = Color(0xFFF4FEFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2F8FA),
    onBackground = Color(0xFF10202A),
    onSurface = Color(0xFF10202A),
    onSurfaceVariant = Color(0xFF49656C),
    outline = Color(0xFF9BCFD2),
    primaryContainer = Color(0xFFC8F7F3),
    onPrimaryContainer = Color(0xFF053936),
    secondaryContainer = Color(0xFFD2F1FF),
    onSecondaryContainer = Color(0xFF073044)
)

private fun animeDarkColors(primary: Color, secondary: Color) = darkColorScheme(
    primary = Color(
        red = (primary.red * 0.58f + 0.42f).coerceIn(0f, 1f),
        green = (primary.green * 0.58f + 0.42f).coerceIn(0f, 1f),
        blue = (primary.blue * 0.58f + 0.42f).coerceIn(0f, 1f),
        alpha = 1f
    ),
    onPrimary = Color(0xFF062D32),
    secondary = Color(
        red = (secondary.red * 0.60f + 0.38f).coerceIn(0f, 1f),
        green = (secondary.green * 0.60f + 0.38f).coerceIn(0f, 1f),
        blue = (secondary.blue * 0.60f + 0.38f).coerceIn(0f, 1f),
        alpha = 1f
    ),
    tertiary = Color(0xFFFFD98A),
    background = Color(0xFF071316),
    surface = Color(0xFF0D1C20),
    surfaceVariant = Color(0xFF153038),
    onBackground = Color(0xFFE8FAFC),
    onSurface = Color(0xFFE8FAFC),
    onSurfaceVariant = Color(0xFFA8C9D0),
    outline = Color(0xFF3B727A),
    primaryContainer = Color(0xFF0D4A50),
    onPrimaryContainer = Color(0xFFD8FFFB),
    secondaryContainer = Color(0xFF123B54),
    onSecondaryContainer = Color(0xFFD9F3FF)
)

@Composable
fun OceanTheme(
    themeMode: String = "system",
    animePrimaryColor: String = "#39C5BB",
    animeSecondaryColor: String = "#00AEEF",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val primary = parseOceanColor(animePrimaryColor, Color(0xFF39C5BB))
    val secondary = parseOceanColor(animeSecondaryColor, Color(0xFF00AEEF))
    val dark = when (themeMode) {
        "dark" -> true
        "light", "anime" -> false
        else -> systemDark
    }
    val colors = when (themeMode) {
        "anime" -> if (systemDark) animeDarkColors(primary, secondary) else animeLightColors(primary, secondary)
        else -> if (dark) DarkOceanColors else LightOceanColors
    }
    CompositionLocalProvider(LocalOceanAccent provides OceanAccentColors(primary, secondary)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
