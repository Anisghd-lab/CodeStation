package com.ghdinteractivestudio.synapse.ui

import androidx.compose.ui.graphics.Color
import com.ghdinteractivestudio.synapse.ui.components.AvatarPalette
import com.ghdinteractivestudio.synapse.ui.theme.Motion
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokensTest {

    @Test
    fun `test Dark Palette follows authentic Codync monochrome tokens`() {
        val dark = Palette.dark()

        // Background is pure deep dark #0A0A0A (not cyan or saturated purple)
        assertEquals(Color(0xFF0A0A0A), dark.background)
        assertEquals(Color(0xFF141414), dark.surface)
        assertEquals(Color(0xFFFFFFFF), dark.text)
        assertEquals(Color(0xFF1E1E1E), dark.bubbleAgent)
        assertEquals(Color(0xFF262626), dark.bubbleUser)

        // Accent in dark mode is crisp white
        assertEquals(Color(0xFFFFFFFF), dark.accent)
        assertEquals(Color(0xFF0A0A0A), dark.onAccent)

        // Semantic status colors
        assertEquals(Color(0xFFEF4444), dark.danger)
        assertEquals(Color(0xFFF59E0B), dark.warning)
        assertEquals(Color(0xFFF59E0B), dark.attention)
    }

    @Test
    fun `test Light Palette follows clean high-contrast tokens`() {
        val light = Palette.light()

        assertEquals(Color(0xFFFFFFFF), light.background)
        assertEquals(Color(0xFFF5F5F5), light.surface)
        assertEquals(Color(0xFF0A0A0A), light.text)
        assertEquals(Color(0xFF0A0A0A), light.accent)
        assertEquals(Color(0xFFFFFFFF), light.onAccent)
        assertEquals(Color(0xFFE5E5E5), light.bubbleAgent)
    }

    @Test
    fun `test AvatarPalette contains 8 authentic geometric shapes and 11 distinct colors`() {
        assertEquals(8, AvatarPalette.shapes.size)
        assertTrue(AvatarPalette.shapes.contains("blob"))
        assertTrue(AvatarPalette.shapes.contains("pebble"))
        assertTrue(AvatarPalette.shapes.contains("squircle"))
        assertTrue(AvatarPalette.shapes.contains("tablet"))
        assertTrue(AvatarPalette.shapes.contains("wedge"))
        assertTrue(AvatarPalette.shapes.contains("hex"))
        assertTrue(AvatarPalette.shapes.contains("cloud"))
        assertTrue(AvatarPalette.shapes.contains("teardrop"))

        assertEquals(11, AvatarPalette.colors.size)
        AvatarPalette.colors.forEach { color ->
            assertNotNull(color)
        }
    }

    @Test
    fun `test Motion constants follow Apple-style physics`() {
        assertEquals(0.98f, Motion.PRESS_SCALE)
        assertEquals(150, Motion.DURATION_HOVER)
        assertEquals(180, Motion.DURATION_FADE)
        assertEquals(100, Motion.DURATION_PRESS)
        assertEquals(260, Motion.DURATION_MORPH)
        assertEquals(300, Motion.DURATION_CONVERSATION)
    }
}
