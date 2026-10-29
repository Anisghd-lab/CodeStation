package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onQuoteReply: (ChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.author.type == MessageAuthorType.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Palette.bubbleUser else Palette.bubbleAgent

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        val maxBubbleWidth = if (isUser) maxWidth * 0.86f else maxWidth * 0.96f

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            // Agent Identity Header
            if (!isUser && message.author.type == MessageAuthorType.BOT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                ) {
                    CharacterAvatar(
                        shape = message.author.avatarGlyph?.takeIf { it.isNotEmpty() } ?: "diamond",
                        color = "blue",
                        size = 22.dp
                    )
                    Text(
                        message.author.displayName,
                        style = Type.caption2.semibold,
                        color = Palette.secondary
                    )
                }
            }

            Box(
                Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(bubbleShape)
                    .background(bubbleColor)
                    .border(0.5.dp, Palette.glassBorder, bubbleShape)
                    .clickable { onQuoteReply(message) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quoted Reply Preview
                    if (message.replyTo != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedRectangle(6.dp))
                                .background(Palette.text.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .clip(RoundedRectangle(1.dp))
                                    .background(Palette.electricBlue)
                            )
                            Column {
                                Text(
                                    message.replyTo.authorName,
                                    style = Type.caption2.bold,
                                    color = Palette.secondary
                                )
                                Text(
                                    message.replyTo.textExcerpt,
                                    style = Type.caption,
                                    color = Palette.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Attachments Preview
                    if (message.attachments.isNotEmpty()) {
                        message.attachments.forEach { att ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedRectangle(8.dp))
                                    .background(Palette.text.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val icon = when {
                                    att.mimeType.startsWith("image/") -> "photo"
                                    att.mimeType.startsWith("video/") -> "video"
                                    else -> "doc"
                                }
                                Symbol(icon, size = 16.dp, tint = Palette.electricBlue)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        att.name,
                                        style = Type.caption2.semibold,
                                        color = Palette.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val formatted = when {
                                        att.sizeBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f Mo", att.sizeBytes / (1024.0 * 1024.0))
                                        att.sizeBytes >= 1024 -> String.format(java.util.Locale.US, "%.0f Ko", att.sizeBytes / 1024.0)
                                        else -> "${att.sizeBytes} o"
                                    }
                                    Text(
                                        formatted,
                                        style = Type.system(9f),
                                        color = Palette.tertiary
                                    )
                                }
                            }
                        }
                    }

                    // Rich Message Text with blue slash commands & markdown formatting
                    MarkdownMessageView(
                        text = message.text,
                        isUser = isUser,
                        textColor = Palette.text
                    )
                }
            }
        }
    }
}
