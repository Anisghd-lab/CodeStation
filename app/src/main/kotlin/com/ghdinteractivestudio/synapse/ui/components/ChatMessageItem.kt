package com.ghdinteractivestudio.synapse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.MessageAuthorType
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold
import com.ghdinteractivestudio.synapse.ui.theme.semibold

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onQuoteReply: (ChatMessage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUser = message.author.type == MessageAuthorType.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Palette.bubbleUser else Palette.bubbleAgent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        if (!isUser && message.author.type == MessageAuthorType.BOT) {
            Text(
                message.author.displayName,
                style = Type.caption2.semibold,
                color = Palette.secondary,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Box(
            Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedRectangle(18.dp))
                .background(bubbleColor)
                .clickable { onQuoteReply(message) }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                .background(Palette.accentFill)
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

                Text(
                    text = message.text,
                    style = Type.body,
                    color = Palette.text
                )
            }
        }
    }
}
