package com.ghdinteractivestudio.codestation.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveIconValidationTest {

    private val resDir = File("src/main/res")

    @Test
    fun testLauncherBackgroundIsPureBlack() {
        val bgFile = File(resDir, "drawable/ic_launcher_background.xml")
        assertTrue("ic_launcher_background.xml must exist", bgFile.exists())
        val content = bgFile.readText()
        assertTrue("Background must be pure black #FF000000", content.contains("#FF000000"))
    }

    @Test
    fun testLauncherForegroundVectorHas73Dots() {
        val fgFile = File(resDir, "drawable/ic_launcher_foreground.xml")
        assertTrue("ic_launcher_foreground.xml must exist", fgFile.exists())
        val content = fgFile.readText()
        // Count circle subpaths: each starts with 'M '
        val dotCount = content.split("M ").size - 1
        assertEquals("Exact number of dots in the CodeStation matrix must be 73", 73, dotCount)
        assertTrue("Dots must be pure white #FFFFFFFF", content.contains("#FFFFFFFF"))
    }

    @Test
    fun testAdaptiveIconDefinitionsUseCorrectDrawables() {
        val iconXml = File(resDir, "mipmap-anydpi-v26/ic_launcher.xml")
        val roundIconXml = File(resDir, "mipmap-anydpi-v26/ic_launcher_round.xml")

        assertTrue(iconXml.exists())
        assertTrue(roundIconXml.exists())

        val iconContent = iconXml.readText()
        assertTrue(iconContent.contains("@drawable/ic_launcher_background"))
        assertTrue(iconContent.contains("@drawable/ic_launcher_foreground"))

        val roundContent = roundIconXml.readText()
        assertTrue(roundContent.contains("@drawable/ic_launcher_background"))
        assertTrue(roundContent.contains("@drawable/ic_launcher_foreground"))
    }

    @Test
    fun testAllDensityMipmapFilesExist() {
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        for (d in densities) {
            val dir = File(resDir, "mipmap-$d")
            assertTrue("Directory mipmap-$d must exist", dir.exists())
            assertTrue("ic_launcher.png must exist in $d", File(dir, "ic_launcher.png").exists())
            assertTrue("ic_launcher_round.png must exist in $d", File(dir, "ic_launcher_round.png").exists())
            assertTrue("ic_launcher_foreground.png must exist in $d", File(dir, "ic_launcher_foreground.png").exists())
        }
    }
}
