package com.djtaylor.wordjourney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.djtaylor.wordjourney.domain.model.GameTheme
import com.djtaylor.wordjourney.domain.model.ThemeRegistry

/** Provides the current high‑contrast flag to all composables. */
val LocalHighContrast = staticCompositionLocalOf { false }

/**
 * Provides the current colorblind mode to all composables.
 * Values: "none", "protanopia", "deuteranopia", "tritanopia"
 */
val LocalColorblindMode = staticCompositionLocalOf { "none" }

/** Provides the text scale factor (0.8 – 1.5) to all composables. */
val LocalTextScale = staticCompositionLocalOf { 1.0f }

/** Provides the active GameTheme to all composables. */
val LocalGameTheme = staticCompositionLocalOf { ThemeRegistry.CLASSIC }

private val HighContrastDarkScheme = darkColorScheme(
    primary            = Color(0xFFFFD54F),
    onPrimary          = Color(0xFF000000),
    primaryContainer   = Color(0xFF3E2723),
    onPrimaryContainer = Color(0xFFFFFFFF),
    background         = Color(0xFF000000),
    onBackground       = Color(0xFFFFFFFF),
    surface            = Color(0xFF1A1A1A),
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF2A2A2A),
    onSurfaceVariant   = Color(0xFFFFFFFF),
    error              = Color(0xFFFF6B6B),
    onError            = Color(0xFF000000)
)

private val HighContrastLightScheme = lightColorScheme(
    primary            = Color(0xFF6D4C00),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFFFE082),
    onPrimaryContainer = Color(0xFF000000),
    background         = Color(0xFFFFFFFF),
    onBackground       = Color(0xFF000000),
    surface            = Color(0xFFF5F5F5),
    onSurface          = Color(0xFF000000),
    surfaceVariant     = Color(0xFFE0E0E0),
    onSurfaceVariant   = Color(0xFF000000),
    error              = Color(0xFFB71C1C),
    onError            = Color(0xFFFFFFFF)
)

/** Build a MaterialTheme color scheme from a [GameTheme]. */
private fun gameThemeToDarkScheme(t: GameTheme) = darkColorScheme(
    primary            = t.primaryAccent,
    onPrimary          = Color.White,
    primaryContainer   = t.surfaceDark,
    onPrimaryContainer = Color.White,
    background         = t.backgroundDark,
    onBackground       = Color(0xFFF0E8D4),
    surface            = t.surfaceDark,
    onSurface          = Color(0xFFE8DCC8),
    surfaceVariant     = t.surfaceDark.copy(alpha = 0.8f),
    onSurfaceVariant   = Color(0xFFE8DCC8),
    error              = Error,
    onError            = OnError
)

private fun gameThemeToLightScheme(t: GameTheme) = lightColorScheme(
    primary            = t.primaryAccent,
    onPrimary          = Color.White,
    primaryContainer   = t.surfaceLight,
    onPrimaryContainer = Color(0xFF1C150A),
    background         = t.backgroundLight,
    onBackground       = Color(0xFF1C150A),
    surface            = t.surfaceLight,
    onSurface          = Color(0xFF2A1F0E),
    surfaceVariant     = t.surfaceLight.copy(alpha = 0.8f),
    onSurfaceVariant   = Color(0xFF2A1F0E),
    error              = Error,
    onError            = OnError
)

@Composable
fun WordJourneysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    colorblindMode: String = "none",
    textScale: Float = 1.0f,
    gameTheme: GameTheme = ThemeRegistry.CLASSIC,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme  -> HighContrastDarkScheme
        highContrast && !darkTheme -> HighContrastLightScheme
        darkTheme                  -> gameThemeToDarkScheme(gameTheme)
        else                       -> gameThemeToLightScheme(gameTheme)
    }

    CompositionLocalProvider(
        LocalHighContrast provides highContrast,
        LocalColorblindMode provides colorblindMode,
        LocalTextScale provides textScale,
        LocalGameTheme provides gameTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = WordJourneysTypography,
            content     = content
        )
    }
}
