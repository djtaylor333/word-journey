package com.djtaylor.wordjourney.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Provides the current high‑contrast flag to all composables. */
val LocalHighContrast = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    background         = BackgroundDark,
    onBackground       = OnBackgroundDark,
    surface            = SurfaceDark,
    onSurface          = OnSurfaceDark,
    surfaceVariant     = SurfaceVariantDark,
    onSurfaceVariant   = OnSurfaceDark,
    error              = Error,
    onError            = OnError
)

private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    background         = BackgroundLight,
    onBackground       = OnBackgroundLight,
    surface            = SurfaceLight,
    onSurface          = OnSurfaceLight,
    surfaceVariant     = SurfaceVariantLight,
    onSurfaceVariant   = OnSurfaceLight,
    error              = Error,
    onError            = OnError
)

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

@Composable
fun WordJourneysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme  -> HighContrastDarkScheme
        highContrast && !darkTheme -> HighContrastLightScheme
        darkTheme                  -> DarkColorScheme
        else                       -> LightColorScheme
    }

    CompositionLocalProvider(LocalHighContrast provides highContrast) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = WordJourneysTypography,
            content     = content
        )
    }
}
