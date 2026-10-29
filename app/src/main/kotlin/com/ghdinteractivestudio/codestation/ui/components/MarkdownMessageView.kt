package com.ghdinteractivestudio.codestation.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern

private sealed class MarkdownBlock {
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock()
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val level: Int, val text: String) : MarkdownBlock()
    data class NumberedItem(val prefix: String, val text: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
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
        modifier = modifier.fillMaxWidth(),
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
                            .fillMaxWidth()
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(12.dp))
            .background(Palette.codeBackground)
            .border(0.5.dp, Palette.glassBorder, RoundedRectangle(12.dp))
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

/**
 * Splits markdown input text into high-level structured blocks:
 * code blocks, headers, bullet/numbered lists, blockquotes, and paragraphs.
 */
private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
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

        // 2. Headers (#, ##, ###)
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

        // 3. Bullet list (- or *)
        if ((trimmed.startsWith("- ") || trimmed.startsWith("* ")) && !trimmed.startsWith("---")) {
            val indent = line.takeWhile { it.isWhitespace() }.length / 2
            val itemText = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(indent, itemText))
            i++
            continue
        }

        // 4. Numbered list (1. 2.)
        val numMatch = Regex("^(\\d+\\.)\\s+(.*)").find(trimmed)
        if (numMatch != null) {
            val prefix = numMatch.groupValues[1]
            val content = numMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(prefix, content))
            i++
            continue
        }

        // 5. Blockquote (> )
        if (trimmed.startsWith("> ")) {
            val quoteText = trimmed.removePrefix("> ").trim()
            blocks.add(MarkdownBlock.Blockquote(quoteText))
            i++
            continue
        }

        // 6. Regular paragraph
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
