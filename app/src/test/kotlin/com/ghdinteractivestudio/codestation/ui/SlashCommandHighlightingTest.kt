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

    @Test
    fun `test quota command output parsed into QuotaCard block with gauges data`() {
        val rawQuotaOutput = """
            Quota:
            Gemini Models          Weekly Limit Remaining     70%   2026-11-05 12:52 UTC
            Gemini Models          Five Hour Limit Remaining  83%   2026-10-30 03:52 UTC
            Claude and GPT models  Weekly Limit Remaining     100%  2026-11-05 23:51 UTC
            Claude and GPT models  Five Hour Limit Remaining  100%  2026-10-30 04:51 UTC
        """.trimIndent()

        val blocks = com.ghdinteractivestudio.codestation.ui.components.parseMarkdownBlocks(rawQuotaOutput)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is com.ghdinteractivestudio.codestation.ui.components.MarkdownBlock.QuotaCard)

        val card = blocks[0] as com.ghdinteractivestudio.codestation.ui.components.MarkdownBlock.QuotaCard
        assertEquals(4, card.items.size)

        // Item 1: Gemini Weekly
        assertEquals("Gemini Models", card.items[0].group)
        assertEquals("Weekly Limit Remaining", card.items[0].limitType)
        assertEquals(70.0, card.items[0].remainingPercent, 0.01)
        assertEquals("2026-11-05 12:52 UTC", card.items[0].resetText)

        // Item 2: Gemini 5-hour
        assertEquals("Gemini Models", card.items[1].group)
        assertEquals("Five Hour Limit Remaining", card.items[1].limitType)
        assertEquals(83.0, card.items[1].remainingPercent, 0.01)
        assertEquals("2026-10-30 03:52 UTC", card.items[1].resetText)

        // Item 3: Claude Weekly
        assertEquals("Claude and GPT models", card.items[2].group)
        assertEquals(100.0, card.items[2].remainingPercent, 0.01)

        // Item 4: Claude 5-hour
        assertEquals("Claude and GPT models", card.items[3].group)
        assertEquals(100.0, card.items[3].remainingPercent, 0.01)

        // Tab-delimited format (piped / non-interactive agy)
        val tabbedOutput = "Gemini Models\tWeekly Limit Remaining\t69%\t2026-11-05T12:52:22Z\nClaude and GPT models\tFive Hour Limit Remaining\t100%\t2026-10-30T05:04:23Z"
        val tabBlocks = com.ghdinteractivestudio.codestation.ui.components.parseMarkdownBlocks(tabbedOutput)
        assertEquals(1, tabBlocks.size)
        assertTrue(tabBlocks[0] is com.ghdinteractivestudio.codestation.ui.components.MarkdownBlock.QuotaCard)
        val tabCard = tabBlocks[0] as com.ghdinteractivestudio.codestation.ui.components.MarkdownBlock.QuotaCard
        assertEquals(2, tabCard.items.size)
        assertEquals(69.0, tabCard.items[0].remainingPercent, 0.01)
    }

    @Test
    fun `test formatLimitLabel transforms standard raw limits into clean French`() {
        val weekly = com.ghdinteractivestudio.codestation.ui.components.formatLimitLabel("Weekly Limit Remaining")
        assertEquals("Limite Hebdomadaire (7j)", weekly)

        val fiveHour = com.ghdinteractivestudio.codestation.ui.components.formatLimitLabel("Five Hour Limit Remaining")
        assertEquals("Session 5 Heures", fiveHour)
    }
}
