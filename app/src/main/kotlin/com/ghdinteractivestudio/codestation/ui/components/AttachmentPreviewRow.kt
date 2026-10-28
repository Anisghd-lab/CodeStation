package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.PendingAttachment
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun AttachmentPreviewRow(
    attachments: List<PendingAttachment>,
    onRemoveAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        attachments.forEach { item ->
            AttachmentChip(
                item = item,
                onRemove = { onRemoveAttachment(item.id) }
            )
        }
    }
}

@Composable
fun AttachmentChip(
    item: PendingAttachment,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 220.dp)
            .clip(RoundedRectangle(12.dp))
            .background(Palette.glassSurface)
            .border(1.dp, Palette.glassBorder, RoundedRectangle(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val iconName = when {
            item.isImage -> "photo"
            item.isVideo -> "video"
            else -> "doc"
        }
        val iconTint = when {
            item.isVideo -> Palette.warning
            item.isImage -> Palette.accent
            else -> Palette.emeraldAccent
        }

        Symbol(iconName, size = 18.dp, tint = iconTint)

        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = item.name,
                style = Type.caption.semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.formattedSize,
                    style = Type.system(9f),
                    color = Palette.tertiary
                )
                if (item.isVideo) {
                    Text(
                        text = "• Modèle texte seul",
                        style = Type.system(9f),
                        color = Palette.warning
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(Capsule())
                .background(Palette.surface)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Symbol("xmark", size = 10.dp, tint = Palette.secondary)
        }
    }
}
