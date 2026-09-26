package com.aistudio.ecotobacco.kfzqw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    customColor: Int = 0xFF10B981.toInt(), // Default Emerald
    uiConfig: UiConfig = UiConfig(),
    content: @Composable () -> Unit,
) {
    val primaryColor = Color(customColor)
    
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.Black,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFF334155),
            onSurface = if (uiConfig.customTextColor != 0) Color(uiConfig.customTextColor) else Color.White,
            onBackground = if (uiConfig.customTextColor != 0) Color(uiConfig.customTextColor) else Color.White,
            outline = Color(0xFF475569)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.1f),
            onPrimaryContainer = primaryColor,
            background = BackgroundModern,
            surface = Color.White,
            surfaceVariant = Color(0xFFF1F5F9),
            onSurface = if (uiConfig.customTextColor != 0) Color(uiConfig.customTextColor) else TextPrimary,
            onBackground = if (uiConfig.customTextColor != 0) Color(uiConfig.customTextColor) else TextPrimary,
            outline = Slate200
        )
    }

    CompositionLocalProvider(LocalUiConfig provides uiConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
