package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.FileAttachment
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    serverUrl: String = "http://127.0.0.1:19222",
    serverToken: String = "",
    onQuoteReply: (ChatMessage) -> Unit = {},
    onLongClick: (ChatMessage) -> Unit = {},
    onImageClick: (url: String, title: String) -> Unit = { _, _ -> },
    onArtifactClick: (artifactId: String, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val isUser = message.author.type == MessageAuthorType.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        val maxBubbleWidth = if (isUser) maxWidth * 0.82f else maxWidth * 0.92f

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            // Agent Identity Header (au-dessus de la bulle pour l'assistant)
            if (!isUser && message.author.type == MessageAuthorType.BOT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start)
                        .widthIn(max = maxBubbleWidth)
                        .padding(start = 4.dp, bottom = 4.dp)
                ) {
                    CharacterAvatar(
                        shape = message.author.avatarGlyph?.takeIf { it.isNotEmpty() } ?: "diamond",
                        color = "blue",
                        size = 20.dp
                    )
                    Text(
                        message.author.displayName,
                        style = Type.caption2.semibold,
                        color = Palette.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (message.timestamp > 0) {
                        Text(
                            text = formatMessageTime(message.timestamp),
                            style = Type.caption2,
                            color = Palette.tertiary,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentWidth(align = alignment)
                    .widthIn(min = 36.dp, max = maxBubbleWidth)
                    .frostedLiquidGlass(shape = bubbleShape, isUser = isUser)
                    .pointerInput(message.id) {
                        detectTapGestures(
                            onTap = { onQuoteReply(message) },
                            onLongPress = { onLongClick(message) }
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.wrapContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quoted Reply Preview
                    if (message.replyTo != null) {
                        Row(
                            Modifier
                                .wrapContentWidth()
                                .background(Palette.text.copy(alpha = 0.08f), RoundedRectangle(6.dp))
                                .clip(RoundedRectangle(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .clip(RoundedRectangle(1.dp))
                                    .background(Palette.electricBlue)
                            )
                            Column(Modifier.wrapContentWidth()) {
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

                    // Rich Message Text (filtré si c'est seulement le nom brut du fichier généré ou "Artifact: Title")
                    val isRedundantFileString = isRedundantFileMessage(message.text, message.attachments)
                    val isRedundantArtifactString = message.text.startsWith("Artifact: ") && message.attachments.any { it.isArtifact }

                    if (message.text.isNotBlank() && !isRedundantFileString && !isRedundantArtifactString) {
                        MarkdownMessageView(
                            text = message.text,
                            isUser = isUser,
                            textColor = Palette.text
                        )
                    }

                    // Attachments Preview (Images réelles, Artefacts, Documents)
                    if (message.attachments.isNotEmpty()) {
                        message.attachments.forEach { att ->
                            when {
                                att.isArtifact || att.mimeType == "text/html" -> {
                                    ArtifactMessageCard(
                                        att = att,
                                        botId = message.botId,
                                        serverUrl = serverUrl,
                                        serverToken = serverToken,
                                        onClick = { artId, artTitle ->
                                            onArtifactClick(artId, artTitle)
                                        }
                                    )
                                }
                                isImageAttachment(att) -> {
                                    ImageMessagePreview(
                                        att = att,
                                        botId = message.botId,
                                        isUser = isUser,
                                        serverUrl = serverUrl,
                                        serverToken = serverToken,
                                        onClick = { url, name ->
                                            onImageClick(url, name)
                                        }
                                    )
                                }
                                else -> {
                                    // Fichier générique
                                    GenericFileCard(att = att)
                                }
                            }
                        }
                    }
                }
            }

            if (isUser && message.timestamp > 0) {
                Text(
                    text = formatMessageTime(message.timestamp),
                    style = Type.caption2,
                    color = Palette.tertiary,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 6.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageMessagePreview(
    att: FileAttachment,
    botId: String,
    isUser: Boolean,
    serverUrl: String,
    serverToken: String,
    onClick: (url: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val mediaUrl = remember(att, botId, isUser, serverUrl, serverToken) {
        resolveMediaUrl(att, botId, isUser, serverUrl, serverToken)
    }
    val ext = att.name.substringAfterLast('.', "").uppercase().ifBlank { "IMG" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Palette.text.copy(alpha = 0.05f))
            .border(1.dp, Palette.text.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { onClick(mediaUrl, att.name) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 240.dp),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = att.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Palette.electricBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                error = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol("photo", size = 24.dp, tint = Palette.electricBlue)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = att.name,
                                style = Type.body.semibold,
                                color = Palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Aperçu indisponible · Cliquer pour agrandir",
                                style = Type.caption2,
                                color = Palette.tertiary
                            )
                        }
                    }
                }
            )

            // Badge d'extension en haut à droite
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = ext,
                    style = Type.system(9f).bold,
                    color = Color.White
                )
            }
        }

        // Barre inférieure avec nom et taille
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.text.copy(alpha = 0.04f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = att.name,
                style = Type.caption2.semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (att.sizeBytes > 0) {
                Text(
                    text = formatFileSize(att.sizeBytes),
                    style = Type.system(9f),
                    color = Palette.tertiary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtifactMessageCard(
    att: FileAttachment,
    botId: String,
    serverUrl: String,
    serverToken: String,
    onClick: (artifactId: String, title: String) -> Unit
) {
    val artifactTitle = att.artifactTitle ?: att.name.removeSuffix(".html")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Palette.electricBlue.copy(alpha = 0.16f),
                        Palette.turquoise.copy(alpha = 0.08f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .border(1.dp, Palette.electricBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(att.id, artifactTitle) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Palette.electricBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Symbol("doc.richtext", size = 20.dp, tint = Palette.electricBlue)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artifactTitle,
                style = Type.body.semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Document HTML Interactif · Cliquer pour ouvrir",
                style = Type.caption2,
                color = Palette.secondary
            )
        }
        Box(
            modifier = Modifier
                .background(Palette.electricBlue, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Ouvrir",
                style = Type.caption2.bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun GenericFileCard(att: FileAttachment) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.text.copy(alpha = 0.08f), RoundedRectangle(8.dp))
            .clip(RoundedRectangle(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val icon = when {
            att.mimeType.startsWith("video/") -> "video"
            att.mimeType.startsWith("audio/") -> "waveform"
            else -> "doc"
        }
        Symbol(icon, size = 18.dp, tint = Palette.electricBlue)
        Column(Modifier.weight(1f)) {
            Text(
                att.name,
                style = Type.caption2.semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatFileSize(att.sizeBytes),
                style = Type.system(9f),
                color = Palette.tertiary
            )
        }
    }
}

private fun resolveMediaUrl(
    att: FileAttachment,
    botId: String,
    isUser: Boolean,
    serverUrl: String,
    serverToken: String
): String {
    if (!att.localUri.isNullOrBlank()) {
        return att.localUri
    }
    val base = serverUrl.trimEnd('/')
    val tokenParam = if (serverToken.isNotBlank()) "?token=$serverToken" else ""
    return when {
        att.isArtifact -> "$base/media/artifact/$botId/${att.id}$tokenParam"
        isUser -> "$base/media/upload/$botId/${att.id}$tokenParam"
        else -> "$base/media/file/$botId/${att.id}$tokenParam"
    }
}

private fun isImageAttachment(att: FileAttachment): Boolean {
    if (att.isArtifact) return false
    if (att.mimeType.startsWith("image/")) return true
    val ext = att.name.substringAfterLast('.', "").lowercase()
    return ext in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "svg")
}

private fun isRedundantFileMessage(text: String, attachments: List<FileAttachment>): Boolean {
    val trimmed = text.trim()
    if (trimmed.matches(Regex("""^.+\s*\(\d+\s*bytes\)$"""))) return true
    return attachments.any { att -> trimmed == att.name || trimmed == "${att.name} (${att.sizeBytes} bytes)" }
}

private fun formatFileSize(sizeBytes: Long): String {
    return when {
        sizeBytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f Mo", sizeBytes / (1024.0 * 1024.0))
        sizeBytes >= 1024 -> String.format(java.util.Locale.US, "%.0f Ko", sizeBytes / 1024.0)
        sizeBytes > 0 -> "$sizeBytes o"
        else -> ""
    }
}

private fun formatMessageTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}
