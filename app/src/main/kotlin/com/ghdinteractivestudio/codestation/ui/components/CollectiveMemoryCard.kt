package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.CollectiveMemoryStatus
import com.ghdinteractivestudio.codestation.domain.model.MemorySyncState
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold

/**
 * Native Collective Memory Card appearing at the top of the Roster.
 * Faithfully matches the visual reference:
 * - 48.dp Neural Avatar (green/turquoise & slate grey particle lattice).
 * - Title: "Mémoire collective".
 * - Subtitle: "Expériences partagées · Tous les agents".
 * - Dynamic Status: "• Synchronisée avec GitHub" (or real status, strictly no fake sync).
 */
@Composable
fun CollectiveMemoryCard(
    status: CollectiveMemoryStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (source, pressed) = rememberPress()
    val cardShape = RoundedRectangle(16.dp)

    val syncState = status.syncStatus
    val dotColor = Color(android.graphics.Color.parseColor(syncState.dotColorHex))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressed)
            .glass(shape = cardShape, style = GlassStyle.Card, pressed = pressed)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Native Animated Neural Avatar (Matching agent bots dot matrix)
        NeuralAvatar(
            size = 44.dp,
            onClick = onClick
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mémoire",
                    style = Type.body.semibold,
                    color = Palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Optional subtle icon or indicator
                Symbol("chevron.right", size = 12.dp, tint = Palette.tertiary)
            }

            // Subtitle row
            Text(
                text = "Expériences partagées · Tous les agents",
                style = Type.subheadline,
                color = Palette.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic Status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Colored status dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Text(
                    text = syncState.displayLabel,
                    style = Type.caption.copy(fontSize = 12.sp),
                    color = if (syncState == MemorySyncState.SYNCED) Palette.added else Palette.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
