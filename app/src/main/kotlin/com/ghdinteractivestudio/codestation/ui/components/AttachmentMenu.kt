package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun AttachmentMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onPickDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    val menuShape = RoundedRectangle(18.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = menuShape,
        containerColor = Palette.glassSurface,
        border = BorderStroke(0.5.dp, Palette.glassBorder),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .width(220.dp)
            .padding(vertical = 4.dp)
    ) {
        AttachmentMenuItem(
            title = "Photo",
            subtitle = "Galerie d'images",
            icon = "photo",
            onClick = {
                onDismissRequest()
                onPickPhoto()
            }
        )

        AttachmentMenuItem(
            title = "Vidéo",
            subtitle = "Vidéos de l'appareil",
            icon = "video",
            onClick = {
                onDismissRequest()
                onPickVideo()
            }
        )

        AttachmentMenuItem(
            title = "Fichier",
            subtitle = "Documents et code",
            icon = "doc",
            onClick = {
                onDismissRequest()
                onPickDocument()
            }
        )
    }
}

@Composable
private fun AttachmentMenuItem(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Symbol(icon, size = 20.dp, tint = Palette.accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Type.body.semibold, color = Palette.text)
            Text(subtitle, style = Type.system(10f), color = Palette.tertiary)
        }
    }
}
