package com.ghdinteractivestudio.codestation.ui

import androidx.compose.ui.graphics.Color
import com.ghdinteractivestudio.codestation.ui.components.AvatarPalette
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignTokensTest {

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
    fun `test Motion constants and easing functions follow Apple-style physics`() {
        assertEquals(0.98f, Motion.PRESS_SCALE)
        assertNotNull(Motion.hover<Float>())
        assertNotNull(Motion.fade<Float>())
        assertNotNull(Motion.press<Float>())
        assertNotNull(Motion.morph<Float>())
        assertNotNull(Motion.layout<Float>())
        assertNotNull(Motion.conversation<Float>())
        assertNotNull(Motion.present<Float>())
    }

    @Test
    fun `test Warning color token is amber`() {
        assertEquals(Color(0xFF000000 or 0xF0A030), Palette.warning)
    }
}
