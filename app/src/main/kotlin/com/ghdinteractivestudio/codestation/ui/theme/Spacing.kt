package com.ghdinteractivestudio.codestation.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class CodeStationSpacing(
    val default: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val normal: Dp = 16.dp,
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp
)

val LocalCodeStationSpacing = androidx.compose.runtime.staticCompositionLocalOf { CodeStationSpacing() }
