package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.ApprovalActionType
import com.ghdinteractivestudio.codestation.domain.model.ApprovalRequest
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.semibold

/**
 * Approval card in Grok Bot's choice-card style: what the agent wants, where it runs, and choices.
 */
@Composable
fun ApprovalCardItem(
    request: ApprovalRequest,
    hostName: String = "localhost (Termux)",
    isAnswering: Boolean = false,
    onApprove: (Boolean) -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = request.status == ApprovalStatus.PENDING
    var expanded by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<String?>(null) }

    val headline = when (request.actionType) {
        ApprovalActionType.SHELL_COMMAND -> "Wants to run a command"
        ApprovalActionType.FILE_WRITE -> "Wants to change files"
        ApprovalActionType.NETWORK_ACCESS -> "Wants to access the network"
    }

    val cardShape = RoundedRectangle(22.dp)
    Column(
        modifier
            .padding(end = 40.dp)
            .widthIn(max = 420.dp)
            .background(Palette.bubbleAgent, cardShape)
            .border(0.5.dp, Palette.glassBorder, cardShape)
            .clip(cardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(tr(headline), style = Type.headline, color = Palette.text)
            if (pending) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(Capsule())
                        .background(Palette.warning)
                )
            }
        }

        Text(
            request.commandOrPath,
            style = Type.subheadline.monospaced,
            color = Palette.secondary,
            maxLines = if (expanded) Int.MAX_VALUE else 3
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Symbol("desktopcomputer", size = 14.dp, tint = Palette.tertiary)
            Text(
                "Runs on $hostName · ${request.botName}",
                style = Type.caption,
                color = Palette.tertiary
            )
        }

        val hasDetail = !request.diffPreview.isNullOrEmpty() || request.commandOrPath.length > 60
        if (hasDetail) {
            Disclosure(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                label = {
                    Text(tr("Details"), style = Type.caption.medium, color = Palette.secondary)
                }
            ) {
                Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CodeBox(request.commandOrPath)
                    if (!request.diffPreview.isNullOrEmpty()) {
                        CodeBox(request.diffPreview)
                    }
                }
            }
        }

        if (pending) {
            val options = listOf(
                "allow_once" to "Allow once",
                "allow_always" to "Always allow",
                "reject_once" to "Deny"
            )

            Column(
                Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedRectangle(14.dp))
                    .background(Palette.background)
            ) {
                for ((index, option) in options.withIndex()) {
                    val (id, label) = option
                    if (index > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Palette.border)
                        )
                    }
                    val isWaiting = isAnswering && chosen != null
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clickable(enabled = !isAnswering, role = Role.Button) {
                                chosen = id
                                when (id) {
                                    "allow_once" -> onApprove(false)
                                    "allow_always" -> onApprove(true)
                                    "reject_once" -> onReject()
                                }
                            }
                            .graphicsLayer { alpha = if (!isWaiting || chosen == id) 1f else 0.4f }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tr(label),
                            Modifier.weight(1f),
                            style = if (id == "allow_once") Type.body.semibold else Type.body,
                            color = if (id.startsWith("allow")) Palette.text else Palette.danger
                        )
                        if (isAnswering && chosen == id) {
                            Spinner(14.dp)
                        }
                    }
                }
            }
        } else {
            val statusLabel = when (request.status) {
                ApprovalStatus.APPROVED -> if (request.isAlwaysApproved) "Always approved" else "Approved"
                ApprovalStatus.REJECTED -> "Denied"
                else -> "Pending"
            }
            Text(
                tr(statusLabel),
                style = Type.caption.medium,
                color = if (request.status == ApprovalStatus.APPROVED) Palette.added else Palette.danger
            )
        }
    }
}
