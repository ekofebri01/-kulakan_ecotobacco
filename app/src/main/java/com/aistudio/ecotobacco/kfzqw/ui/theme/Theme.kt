package com.aistudio.ecotobacco.kfzqw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberOnPrimary,
    primaryContainer = AmberPrimaryContainer,
    onPrimaryContainer = AmberOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4D2700),
    primaryContainer = Color(0xFF6E3900),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE4BFA8),
    onSecondary = Color(0xFF422B1D),
    secondaryContainer = Color(0xFF5B4131),
    onSecondaryContainer = Color(0xFFFFDED1),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECE0DA),
    surface = Color(0xFF1A1816),
    onSurface = Color(0xFFECE0DA)
)

@Composable
fun EcoTobaccoTheme(
    themeMode: String = "LIGHT",
    customColor: Int = 0,
    uiConfig: UiConfig = UiConfig(),
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode.uppercase()) {
        "DARK" -> true
        "SYSTEM" -> isSystemInDarkTheme()
        else -> false
    }

    val baseScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (customColor != 0) {
        val accent = Color(customColor)
        baseScheme.copy(
            primary = accent,
            primaryContainer = accent.copy(alpha = if (useDarkTheme) 0.25f else 0.16f)
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalUiConfig provides uiConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
