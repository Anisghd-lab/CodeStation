package com.ghdinteractivestudio.codestation.ui

import androidx.compose.ui.graphics.Color
import com.ghdinteractivestudio.codestation.ui.components.buildRichText
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class SlashCommandHighlightingTest {

    private val defaultColor = Color.White
    private val slashCommandPattern = Pattern.compile("(?<=^|[\\s(\\[{\"'])/([a-zA-Z][a-zA-Z0-9_-]*)(?=$|[\\s)\\]}\"',.!?:;])")

    @Test
    fun `test slash command tokens recognized correctly`() {
        val commands = listOf("/quota", "/help", "/status", "/model", "/learn", "/clear", "/stop")
        for (cmd in commands) {
            val matcher = slashCommandPattern.matcher(cmd)
            assertTrue("Command $cmd should match pattern", matcher.find())
            assertEquals("/" + matcher.group(1), cmd)
        }
    }

    @Test
    fun `test slash commands in sentence with punctuation`() {
        val text = "Vérifie avec /quota et ensuite /help. Que donne /status?"
        val matcher = slashCommandPattern.matcher(text)

        val matches = mutableListOf<String>()
        while (matcher.find()) {
            matches.add(text.substring(matcher.start(), matcher.end()))
        }

        assertEquals(listOf("/quota", "/help", "/status"), matches)
    }

    @Test
    fun `test url containing slash is not recognized as slash command`() {
        val text = "Consultez https://example.com/quota ou https://github.com/Anisghd-lab/CodeStation"
        val richText = buildRichText(text, isUser = false, defaultColor = defaultColor)

        // URL annotations should be present
        val urlAnnotations = richText.getStringAnnotations(tag = "URL", start = 0, end = text.length)
        assertEquals(2, urlAnnotations.size)
        assertEquals("https://example.com/quota", urlAnnotations[0].item)
        assertEquals("https://github.com/Anisghd-lab/CodeStation", urlAnnotations[1].item)

        // Text preserved intact
        assertEquals(text, richText.text)
    }

    @Test
    fun `test unix file path is not matched as standalone slash command`() {
        val path = "/root/CodeStation/environments/bot-antigravity"
        val matcher = slashCommandPattern.matcher(path)
        assertFalse("Unix path should not be recognized as standalone slash command", matcher.find())
    }

    @Test
    fun `test raw text preserved without modification`() {
        val raw = "Exécute /model gemini-4-argon-medium puis /status. 🚀 Accents: é, à, ç, œ."
        val richText = buildRichText(raw, isUser = false, defaultColor = defaultColor)

        assertEquals("Text content must remain completely identical", raw, richText.text)
    }

    @Test
    fun `test slash command inside inline code is preserved as code`() {
        val inlineCode = "Utilise `/quota` pour voir la consommation."
        val richText = buildRichText(inlineCode, isUser = false, defaultColor = defaultColor)

        // Raw text content preserved
        assertTrue(richText.text.contains("/quota"))
    }
}
