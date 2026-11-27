package com.aistudio.ecotobacco.kfzqw.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = TobaccoAmber,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = TobaccoAmberContainer,
    onPrimaryContainer = TobaccoOnAmberContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    background = KraftBackground,
    onBackground = KraftText,
    surface = KraftSurface,
    onSurface = KraftText,
    surfaceVariant = Color(0xFFF0EFEA),
    onSurfaceVariant = KraftSubtext,
    outline = KraftBorder,
    outlineVariant = KraftBorder,
    tertiary = EarthSuccess,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = EarthSuccessContainer,
    onTertiaryContainer = Color(0xFF022C22)
)

private val DarkColorScheme = darkColorScheme(
    primary = WarmAmber,
    onPrimary = Color(0xFF451A03),
    primaryContainer = WarmAmberContainer,
    onPrimaryContainer = WarmOnAmberContainer,
    secondary = Color(0xFFD6D3D1),
    onSecondary = Color(0xFF292524),
    secondaryContainer = Color(0xFF44403C),
    onSecondaryContainer = Color(0xFFF5F5F4),
    background = LeafCharcoalBg,
    onBackground = LeafText,
    surface = LeafCharcoalCard,
    onSurface = LeafText,
    surfaceVariant = Color(0xFF252B28),
    onSurfaceVariant = LeafSubtext,
    outline = LeafCharcoalBorder,
    outlineVariant = LeafCharcoalBorder,
    tertiary = LeafSuccess,
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = LeafSuccessContainer,
    onTertiaryContainer = Color(0xFFD1FAE5)
)

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF002026),
    primaryContainer = Color(0xFF0E3A47),
    onPrimaryContainer = CyberTextBright,
    secondary = CyberEmerald,
    onSecondary = Color(0xFF002116),
    secondaryContainer = Color(0xFF0B3B2B),
    onSecondaryContainer = CyberTextBright,
    tertiary = CyberViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2D1E5F),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = CyberBg,
    onBackground = CyberTextBright,
    surface = CyberCard,
    onSurface = CyberTextBright,
    surfaceVariant = CyberCardElevated,
    onSurfaceVariant = CyberTextDim,
    outlineVariant = CyberBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF002114),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF271900),
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0A0D12),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF161B22),
    onSurfaceVariant = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF27303F)
)

@Composable
fun EcoTobaccoTheme(
    themeMode: String = "LIGHT",
    customColor: Int = 0,
    uiConfig: UiConfig = UiConfig(),
    content: @Composable () -> Unit
) {
    val normalizedMode = themeMode.uppercase()
    val systemDark = isSystemInDarkTheme()
    val isDarkMode = when (normalizedMode) {
        "DARK", "CYBER", "AMOLED" -> true
        "SYSTEM" -> systemDark
        else -> false
    }

    var scheme = when (normalizedMode) {
        "CYBER" -> CyberColorScheme
        "AMOLED" -> AmoledColorScheme
        else -> if (isDarkMode) DarkColorScheme else LightColorScheme
    }

    val effectiveDark = if (uiConfig.customBackgroundColor != 0) {
        Color(uiConfig.customBackgroundColor).luminance() < 0.45f
    } else {
        isDarkMode
    }

    if (uiConfig.customBackgroundColor != 0) {
        val bg = Color(uiConfig.customBackgroundColor)
        val surfaceColor = lerp(bg, if (effectiveDark) Color.White else Color.Black, if (effectiveDark) 0.06f else 0.03f)
        val surfaceVariantColor = lerp(bg, if (effectiveDark) Color.White else Color.Black, if (effectiveDark) 0.13f else 0.07f)
        val autoText = if (effectiveDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)
        val autoMuted = if (effectiveDark) Color(0xFF94A3B8) else Color(0xFF475569)
        val autoOutline = if (effectiveDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.14f)
        scheme = scheme.copy(
            background = bg,
            onBackground = autoText,
            surface = surfaceColor,
            onSurface = autoText,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = autoMuted,
            outlineVariant = autoOutline
        )
    }

    if (customColor != 0) {
        val accent = Color(customColor)
        val onAccent = if (accent.luminance() > 0.55f) Color(0xFF0F172A) else Color.White
        val container = accent.copy(alpha = if (effectiveDark) 0.24f else 0.14f)
        val onContainer = if (effectiveDark) {
            Color(0xFFF8FAFC)
        } else {
            lerp(accent, Color.Black, 0.52f)
        }
        scheme = scheme.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = container,
            onPrimaryContainer = onContainer,
            secondary = lerp(accent, if (effectiveDark) Color.White else Color.Black, 0.22f)
        )
    }

    if (uiConfig.customTextColor != 0) {
        val textCol = Color(uiConfig.customTextColor)
        scheme = scheme.copy(
            onBackground = textCol,
            onSurface = textCol,
            onSurfaceVariant = textCol.copy(alpha = 0.72f)
        )
    }

    val radius = uiConfig.cornerRadius.coerceIn(0, 32)
    val dynamicShapes = Shapes(
        small = RoundedCornerShape((radius * 0.55f).dp),
        medium = RoundedCornerShape(radius.dp),
        large = RoundedCornerShape((radius * 1.25f).coerceAtMost(32f).dp),
        extraLarge = RoundedCornerShape((radius * 1.5f).coerceAtMost(36f).dp)
    )

    CompositionLocalProvider(LocalUiConfig provides uiConfig) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = dynamicShapes,
            content = content
        )
    }
}
