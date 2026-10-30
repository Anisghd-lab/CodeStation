package com.ghdinteractivestudio.codestation.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.monospacedDigit
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import kotlin.math.roundToInt

data class QuotaGaugeItem(
    val group: String,
    val limitType: String,
    val remainingPercent: Double,
    val resetText: String? = null
)

internal sealed class MarkdownBlock {
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock()
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val level: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val prefix: String, val text: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class QuotaCard(val title: String, val items: List<QuotaGaugeItem>) : MarkdownBlock()
}

/**
 * High-quality Markdown renderer for bot and user messages:
 * - Electric Blue (#438DFF) styling for standalone slash commands (/quota, /help, /status...)
 * - Preserves code blocks with monospace, copy button and horizontal scrolling
 * - Renders headers, lists, and links without overflowing
 */
@Composable
fun MarkdownMessageView(
    text: String,
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
    textColor: Color = Palette.text
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(
        modifier = modifier.wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        language = block.language,
                        code = block.code
                    )
                }
                is MarkdownBlock.Header -> {
                    val style = when (block.level) {
                        1 -> Type.title2.bold
                        2 -> Type.title3.semibold
                        else -> Type.subheadline.bold
                    }
                    val annotated = buildRichText(block.text, isUser, textColor)
                    Text(
                        text = annotated,
                        style = style,
                        color = textColor
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = (block.level * 12).dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = Type.body.bold,
                            color = Palette.electricBlue
                        )
                        RichTextRenderer(
                            text = block.text,
                            isUser = isUser,
                            textColor = textColor
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.prefix,
                            style = Type.body.semibold,
                            color = Palette.secondary
                        )
                        RichTextRenderer(
                            text = block.text,
                            isUser = isUser,
                            textColor = textColor
                        )
                    }
                }
                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .clip(RoundedRectangle(1.5.dp))
                                .background(Palette.electricBlue)
                        )
                        RichTextRenderer(
                            text = block.text,
                            isUser = isUser,
                            textColor = Palette.secondary
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    RichTextRenderer(
                        text = block.text,
                        isUser = isUser,
                        textColor = textColor
                    )
                }
                is MarkdownBlock.QuotaCard -> {
                    QuotaCardView(
                        title = block.title,
                        items = block.items
                    )
                }
            }
        }
    }
}

@Composable
private fun RichTextRenderer(
    text: String,
    isUser: Boolean,
    textColor: Color
) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(text, isUser, textColor) {
        buildRichText(text, isUser, textColor)
    }

    ClickableText(
        text = annotated,
        style = Type.body.copy(color = textColor),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    runCatching { uriHandler.openUri(annotation.item) }
                }
        }
    )
}

@Composable
private fun CodeBlockView(
    language: String?,
    code: String
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val blockShape = RoundedRectangle(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.codeBackground, blockShape)
            .border(0.5.dp, Palette.glassBorder, blockShape)
            .clip(blockShape)
    ) {
        // Code Block Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.glassDivider)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language?.takeIf { it.isNotBlank() }?.uppercase() ?: "CODE",
                style = Type.system(10f).semibold,
                color = Palette.secondary
            )
            IconButton(
                title = if (isCopied) "Copié" else "Copier",
                systemImage = if (isCopied) "checkmark" else "doc.on.doc",
                tint = if (isCopied) Palette.added else Palette.secondary,
                action = {
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Code", code))
                    scope.launch {
                        isCopied = true
                        delay(1500)
                        isCopied = false
                    }
                }
            )
        }

        // Code Content with Horizontal Scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = code,
                style = Type.caption.monospaced,
                color = Palette.text,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Authentic segmented usage tick gauge (fine, continuous ticks).
 */
@Composable
fun QuotaUsageTicks(
    percent: Double,
    tint: Color,
    height: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val value = if (percent.isFinite()) percent.coerceIn(0.0, 100.0) else 0.0
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val gap = 1.5.dp.toPx()
        val count = maxOf(1, (size.width / 4.dp.toPx()).toInt())
        val filled = (count * value / 100.0).roundToInt()
        val width = (size.width - gap * (count - 1)) / count
        for (i in 0 until count) {
            drawRoundRect(
                color = if (i < filled) tint else tint.copy(alpha = 0.16f),
                topLeft = Offset(i * (width + gap), 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}

/**
 * Clean human-readable French label for standard quota limits.
 */
internal fun formatLimitLabel(raw: String): String {
    return when {
        raw.contains("Weekly", ignoreCase = true) -> "Limite Hebdomadaire (7j)"
        raw.contains("Five Hour", ignoreCase = true) || raw.contains("5-hour", ignoreCase = true) -> "Session 5 Heures"
        raw.contains("Daily", ignoreCase = true) -> "Limite Quotidienne"
        else -> raw.removeSuffix(" Remaining").removeSuffix(" remaining").trim()
    }
}

/**
 * Rich Quota & Rate Limit card with interactive visual gauges,
 * rendered whenever a quota report (/quota) is received in chat.
 */
@Composable
fun QuotaCardView(
    title: String,
    items: List<QuotaGaugeItem>,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedRectangle(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Palette.surface.copy(alpha = 0.90f))
            .border(0.5.dp, Palette.glassBorder, cardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // En-tête : Icône jauge / graphique + Titre + Badge d'état
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedRectangle(10.dp))
                    .background(Palette.electricBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Symbol("chart.bar", size = 18.dp, tint = Palette.electricBlue)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Quotas & Consommation" },
                    style = Type.system(13f).bold,
                    color = Palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Google Antigravity · Limites d'API",
                    style = Type.caption2,
                    color = Palette.secondary
                )
            }
            Row(
                modifier = Modifier
                    .background(Palette.added.copy(alpha = 0.12f), Capsule())
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(Capsule())
                        .background(Palette.added)
                )
                Text(
                    text = "En direct",
                    style = Type.system(9f).semibold,
                    color = Palette.added
                )
            }
        }

        HorizontalDivider(color = Palette.glassDivider, thickness = 0.5.dp)

        items.forEach { item ->
            val remaining = item.remainingPercent.coerceIn(0.0, 100.0)
            val used = (100.0 - remaining).coerceIn(0.0, 100.0)

            val gaugeColor = when {
                remaining <= 10.0 -> Palette.danger
                remaining <= 30.0 -> Palette.warning
                item.group.contains("Gemini", ignoreCase = true) -> Palette.electricBlue
                item.group.contains("Claude", ignoreCase = true) -> Color(0xFFD97757) // Orange Claude authentique
                else -> Palette.added
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedRectangle(10.dp))
                    .background(Palette.text.copy(alpha = 0.04f))
                    .border(0.5.dp, Palette.glassBorder, RoundedRectangle(10.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.group,
                            style = Type.system(12.5f).semibold,
                            color = Palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "·",
                            style = Type.system(12f),
                            color = Palette.tertiary
                        )
                        Text(
                            text = formatLimitLabel(item.limitType),
                            style = Type.caption2,
                            color = Palette.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${remaining.roundToInt()}%",
                            style = Type.system(13.5f).bold.monospacedDigit,
                            color = gaugeColor
                        )
                        Text(
                            text = " restant",
                            style = Type.system(10f),
                            color = Palette.secondary,
                            modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
                        )
                    }
                }

                // Jauge visuelle avec segments UsageTicks
                QuotaUsageTicks(
                    percent = remaining,
                    tint = gaugeColor,
                    height = 10.dp
                )

                // Ligne de détails basse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Consommé : ${used.roundToInt()}%",
                        style = Type.system(9.5f),
                        color = Palette.tertiary
                    )
                    if (!item.resetText.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Symbol("arrow.clockwise", size = 9.dp, tint = Palette.tertiary)
                            Text(
                                text = "Réinitialisation : ${item.resetText}",
                                style = Type.system(9.5f).monospacedDigit,
                                color = Palette.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Builds rich text with:
 * - Standalone slash commands highlighted in Electric Blue (#438DFF)
 * - Clickable URLs
 * - Inline bold, italic, and code snippets
 */
fun buildRichText(
    text: String,
    isUser: Boolean = false,
    defaultColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        // Regex patterns
        val urlPattern = Pattern.compile("https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+")
        val slashCommandPattern = Pattern.compile("(?<=^|[\\s(\\[{\"'])/([a-zA-Z][a-zA-Z0-9_-]*)(?=$|[\\s)\\]}\"',.!?:;])")

        // First identify URL spans to avoid matching slash commands inside URLs
        val urlMatches = mutableListOf<Pair<Int, Int>>()
        val urlMatcher = urlPattern.matcher(text)
        while (urlMatcher.find()) {
            urlMatches.add(urlMatcher.start() to urlMatcher.end())
        }

        // Find slash commands outside URLs
        val commandMatches = mutableListOf<Pair<Int, Int>>()
        val cmdMatcher = slashCommandPattern.matcher(text)
        while (cmdMatcher.find()) {
            val start = cmdMatcher.start()
            val end = cmdMatcher.end()
            val isInsideUrl = urlMatches.any { it.first <= start && end <= it.second }
            if (!isInsideUrl) {
                commandMatches.add(start to end)
            }
        }

        // Parse inline formatting while honoring the tokens
        var cursor = 0
        while (cursor < text.length) {
            // Check if cursor starts an URL
            val activeUrl = urlMatches.firstOrNull { it.first == cursor }
            if (activeUrl != null) {
                val urlText = text.substring(activeUrl.first, activeUrl.second)
                pushStringAnnotation(tag = "URL", annotation = urlText)
                withStyle(
                    SpanStyle(
                        color = Palette.electricBlue,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(urlText)
                }
                pop()
                cursor = activeUrl.second
                continue
            }

            // Check if cursor starts a slash command
            val activeCmd = commandMatches.firstOrNull { it.first == cursor }
            if (activeCmd != null) {
                val cmdText = text.substring(activeCmd.first, activeCmd.second)
                withStyle(
                    SpanStyle(
                        color = Palette.electricBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(cmdText)
                }
                cursor = activeCmd.second
                continue
            }

            // Check inline code: `code`
            if (text[cursor] == '`' && cursor + 1 < text.length) {
                val endIdx = text.indexOf('`', cursor + 1)
                if (endIdx != -1) {
                    val codeContent = text.substring(cursor + 1, endIdx)
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = if (isUser) Color(0x33FFFFFF) else Color(0x1A000000),
                            color = defaultColor
                        )
                    ) {
                        append(" $codeContent ")
                    }
                    cursor = endIdx + 1
                    continue
                }
            }

            // Check inline bold: **bold**
            if (cursor + 1 < text.length && text[cursor] == '*' && text[cursor + 1] == '*') {
                val endIdx = text.indexOf("**", cursor + 2)
                if (endIdx != -1) {
                    val boldContent = text.substring(cursor + 2, endIdx)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                        append(boldContent)
                    }
                    cursor = endIdx + 2
                    continue
                }
            }

            // Check inline italic: *italic*
            if (text[cursor] == '*' && cursor + 1 < text.length && text[cursor + 1] != '*') {
                val endIdx = text.indexOf('*', cursor + 1)
                if (endIdx != -1) {
                    val italicContent = text.substring(cursor + 1, endIdx)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor)) {
                        append(italicContent)
                    }
                    cursor = endIdx + 1
                    continue
                }
            }

            // Plain character
            append(text[cursor])
            cursor++
        }
    }
}

private val quotaRowPattern = Pattern.compile(
    "^(.+?)(?:\\t+|\\s{2,})(.+?Limit(?: Remaining)?|.+?Window|.+?Session|.+?Quota)(?:\\t+|\\s{2,})(\\d+(?:\\.\\d+)?%)\\s*(.*)$",
    Pattern.CASE_INSENSITIVE
)

/**
 * Splits markdown input text into high-level structured blocks:
 * code blocks, quota cards, headers, bullet/numbered lists, blockquotes, and paragraphs.
 */
internal fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // 1. Code Block Fence (```)
        if (line.trimStart().startsWith("```")) {
            val language = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // Skip closing ```
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 2. Quota Table Block (starts with "Quota:" or contains quota table lines)
        val isQuotaHeader = trimmed.equals("quota:", ignoreCase = true) || trimmed.equals("quota", ignoreCase = true)
        val directQuotaMatcher = quotaRowPattern.matcher(trimmed)
        if (isQuotaHeader || directQuotaMatcher.matches()) {
            var j = if (isQuotaHeader) i + 1 else i
            val items = mutableListOf<QuotaGaugeItem>()
            while (j < lines.size) {
                val nextTrimmed = lines[j].trim()
                if (nextTrimmed.isEmpty()) {
                    j++
                    continue
                }
                val m = quotaRowPattern.matcher(nextTrimmed)
                if (m.matches()) {
                    val group = m.group(1)?.trim() ?: ""
                    val limitType = m.group(2)?.trim() ?: ""
                    val pctStr = m.group(3)?.replace("%", "")?.trim() ?: "0"
                    val pct = pctStr.toDoubleOrNull() ?: 0.0
                    val reset = m.group(4)?.trim()?.takeIf { it.isNotBlank() }
                    items.add(QuotaGaugeItem(group, limitType, pct, reset))
                    j++
                } else {
                    break
                }
            }
            if (items.isNotEmpty()) {
                blocks.add(MarkdownBlock.QuotaCard("Quotas Antigravity & Modèles", items))
                i = j
                continue
            }
        }

        // 3. Headers (#, ##, ###)
        if (trimmed.startsWith("#")) {
            var level = 0
            while (level < trimmed.length && trimmed[level] == '#') {
                level++
            }
            if (level in 1..6 && level < trimmed.length && trimmed[level] == ' ') {
                val headerText = trimmed.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Header(level, headerText))
                i++
                continue
            }
        }

        // 4. Bullet list (- or *)
        if ((trimmed.startsWith("- ") || trimmed.startsWith("* ")) && !trimmed.startsWith("---")) {
            val indent = line.takeWhile { it.isWhitespace() }.length / 2
            val itemText = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(indent, itemText))
            i++
            continue
        }

        // 5. Numbered list (1. 2.)
        val numMatch = Regex("^(\\d+\\.)\\s+(.*)").find(trimmed)
        if (numMatch != null) {
            val prefix = numMatch.groupValues[1]
            val content = numMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(prefix, content))
            i++
            continue
        }

        // 6. Blockquote (> )
        if (trimmed.startsWith("> ")) {
            val quoteText = trimmed.removePrefix("> ").trim()
            blocks.add(MarkdownBlock.Blockquote(quoteText))
            i++
            continue
        }

        // 7. Regular paragraph
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val current = lines[i]
            val curTrimmed = current.trim()
            if (curTrimmed.isEmpty() ||
                curTrimmed.startsWith("```") ||
                curTrimmed.startsWith("#") ||
                curTrimmed.startsWith("- ") ||
                curTrimmed.startsWith("* ") ||
                curTrimmed.startsWith("> ") ||
                curTrimmed.equals("quota:", ignoreCase = true) ||
                curTrimmed.equals("quota", ignoreCase = true) ||
                quotaRowPattern.matcher(curTrimmed).matches() ||
                Regex("^(\\d+\\.)\\s+").containsMatchIn(curTrimmed)
            ) {
                break
            }
            paragraphLines.add(current)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
        }
    }

    return blocks
}
