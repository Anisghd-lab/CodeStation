package com.ghdinteractivestudio.codestation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Whether the dark half of each color pair applies; CodeStationTheme sets it from the system. */
val LocalDark = staticCompositionLocalOf { false }

private fun hex(value: Long) = Color(0xFF000000 or value)

@Composable
@ReadOnlyComposable
private fun pair(light: Long, dark: Long) = hex(if (LocalDark.current) dark else light)

/**
 * CodeStation Premium Glassmorphism design palette:
 * Smoked Obsidian background, translucent glass layers with fine borders,
 * soft electric blue accent (#438DFF), emerald status indicators, and high contrast typography.
 */
object Palette {
    val background: Color @Composable @ReadOnlyComposable get() = pair(0xF8F9FA, 0x0B0D12)
    val surface: Color @Composable @ReadOnlyComposable get() = pair(0xF0F2F5, 0x171B24)
    val bubbleAgent: Color @Composable @ReadOnlyComposable get() = pair(0xF3F4F6, 0x171B24)
    val bubbleUser: Color @Composable @ReadOnlyComposable get() = pair(0xE2E8F0, 0x242A38)
    val border: Color @Composable @ReadOnlyComposable get() = pair(0xE2E8F0, 0x262D3D)
    val text: Color @Composable @ReadOnlyComposable get() = pair(0x111827, 0xF9FAFB)
    val secondary: Color @Composable @ReadOnlyComposable get() = pair(0x4B5563, 0x9CA3AF)
    val tertiary: Color @Composable @ReadOnlyComposable get() = pair(0x6B7280, 0x6B7280)
    /** Ink used for fills (primary buttons, the send button, unread badges). */
    val accentFill: Color @Composable @ReadOnlyComposable get() = pair(0x111827, 0xFFFFFF)
    /** Ink readable as text and tint on the background. */
    val accent: Color @Composable @ReadOnlyComposable get() = pair(0x111827, 0xFFFFFF)
    val onAccent: Color @Composable @ReadOnlyComposable get() = pair(0xFFFFFF, 0x0B0D12)
    val accentDim: Color @Composable @ReadOnlyComposable get() = pair(0xD1D5DB, 0x374151)
    val danger: Color @Composable @ReadOnlyComposable get() = pair(0xDC2626, 0xF87171)
    val warning: Color get() = hex(0xF0A030)
    /** A bot that needs you, in widgets and task cards: readable amber in both modes. */
    val attention: Color @Composable @ReadOnlyComposable get() = pair(0xB45309, 0xFBBF24)
    val codeBackground: Color @Composable @ReadOnlyComposable get() = pair(0xEEF2F6, 0x0E1117)
    val added: Color @Composable @ReadOnlyComposable get() = pair(0x16A34A, 0x34D399)
    val removed: Color @Composable @ReadOnlyComposable get() = pair(0xDC2626, 0xF87171)

    // Glassmorphism design tokens (Dark: smoked glass on obsidian black; Light: frosted luminous clear glass)
    val glassSurface: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0xD9171B24) else Color(0xF2FFFFFF)
    val glassCard: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0xB3171B24) else Color(0xF0FFFFFF)
    val glassBorder: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x38FFFFFF) else Color(0x18000000)
    val glassDivider: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x1FFFFFFF) else Color(0x0F000000)

    // Electric soft blue (#438DFF) & Turquoise
    val electricBlue: Color get() = Color(0xFF438DFF)
    val electricBlueSoft: Color get() = Color(0x26438DFF)
    val turquoise: Color get() = Color(0xFF2DD4BF)
    val slashCommand: Color get() = Color(0xFF438DFF)

    // Emerald green accents (Dark: vivid emerald #10B981; Light: deep emerald #059669)
    val emeraldAccent: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0xFF10B981) else Color(0xFF059669)
    val emeraldGlow: Color @Composable @ReadOnlyComposable get() = if (LocalDark.current) Color(0x2610B981) else Color(0x1A059669)
    val onEmerald: Color @Composable @ReadOnlyComposable get() = Color.White
}
