package com.aistudio.ecotobacco.kfzqw.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UiConfig(
    val fontSizeScale: Float = 1.0f,
    val paddingScale: Float = 1.0f,
    val buttonHeight: Int = 56,
    val cornerRadius: Int = 16,
    val customTextColor: Int = 0
)

val LocalUiConfig = staticCompositionLocalOf { UiConfig() }

// Helper extensions for easy access
val Dp.scaled: Dp
    @Composable
    @ReadOnlyComposable
    get() = this * LocalUiConfig.current.paddingScale

val TextUnit.scaled: TextUnit
    @Composable
    @ReadOnlyComposable
    get() = (this.value * LocalUiConfig.current.fontSizeScale).sp
