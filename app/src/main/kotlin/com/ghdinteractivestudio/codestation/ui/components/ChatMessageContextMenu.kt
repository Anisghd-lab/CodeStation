package com.ghdinteractivestudio.codestation.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.semibold

/**
 * Copies plain text to Android ClipboardManager and shows a discreet confirmation Toast.
 */
fun copyToClipboard(
    context: Context,
    text: String,
    label: String = "CodeStation",
    successMessage: String = "Copié dans le presse-papiers"
) {
    try {
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Échec de la copie : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Contextual Glassmorphism menu displaying actions on long press for User and AI messages.
 */
@Composable
fun MessageContextMenu(
    message: ChatMessage,
    expanded: Boolean,
    isBotWorking: Boolean,
    onDismissRequest: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRegenerate: () -> Unit,
    onCopyCode: () -> Unit,
    onSelectText: () -> Unit,
    onDelete: () -> Unit,
    codeBlocksCount: Int = 0,
    width: Dp = 230.dp
) {
    val isUser = message.author.type == MessageAuthorType.USER

    GlassMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        width = width
    ) {
        // 1. Copier l'intégralité du message
        GlassMenuItem(
            title = if (isUser) "Copier le message" else "Copier la réponse",
            icon = "doc.on.doc",
            tint = Palette.text,
            onClick = {
                onDismissRequest()
                onCopy()
            }
        )

        // 2. Modifier (uniquement pour les messages utilisateur)
        if (isUser) {
            GlassMenuDivider()
            GlassMenuItem(
                title = "Modifier",
                subtitle = "Éditer et renvoyer à l'agent",
                icon = "pencil",
                tint = Palette.electricBlue,
                onClick = {
                    onDismissRequest()
                    onEdit()
                }
            )
        }

        // 3. Régénérer (uniquement pour les réponses IA)
        if (!isUser) {
            GlassMenuDivider()
            GlassMenuItem(
                title = "Régénérer",
                subtitle = if (isBotWorking) "Génération déjà en cours…" else "Nouvelle génération de l'agent",
                icon = "arrow.clockwise",
                tint = if (isBotWorking) Palette.secondary else Palette.electricBlue,
                onClick = {
                    onDismissRequest()
                    onRegenerate()
                }
            )
        }

        // 4. Copier le code (si des blocs de code Markdown existent)
        if (!isUser && codeBlocksCount > 0) {
            GlassMenuDivider()
            GlassMenuItem(
                title = "Copier le code",
                subtitle = if (codeBlocksCount > 1) "$codeBlocksCount blocs détectés" else "1 bloc détecté",
                icon = "terminal",
                tint = Palette.electricBlue,
                onClick = {
                    onDismissRequest()
                    onCopyCode()
                }
            )
        }

        // 5. Sélectionner du texte (évite les conflits d'appui long et de défilement)
        GlassMenuDivider()
        GlassMenuItem(
            title = "Sélectionner du texte",
            subtitle = "Sélection libre avec curseurs",
            icon = "text.cursor",
            tint = Palette.secondary,
            onClick = {
                onDismissRequest()
                onSelectText()
            }
        )

        // 6. Supprimer le message
        GlassMenuDivider()
        GlassMenuItem(
            title = "Supprimer",
            subtitle = "Effacer de la discussion locale",
            icon = "trash",
            destructive = true,
            onClick = {
                onDismissRequest()
                onDelete()
            }
        )
    }
}

/**
 * Glass Dialog allowing free and precise text selection without gesture conflicts.
 */
@Composable
fun GlassTextSelectionDialog(
    text: String,
    title: String = "Sélectionner le texte",
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    GlassAlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedRectangle(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("text.cursor", size = 20.dp, tint = Palette.electricBlue)
                Text(title, style = Type.title3.semibold, color = Palette.text)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Vous pouvez mettre en surbrillance n'importe quelle partie du message pour la copier :",
                    style = Type.caption2,
                    color = Palette.secondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 340.dp)
                        .clip(RoundedRectangle(12.dp))
                        .background(Palette.text.copy(alpha = 0.04f))
                        .border(0.5.dp, Palette.glassBorder, RoundedRectangle(12.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            text = text,
                            style = Type.body,
                            color = Palette.text
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(Capsule())
                        .background(Palette.text.copy(alpha = 0.08f))
                        .clickable(role = Role.Button) {
                            copyToClipboard(context, text, "Message complet", "Message complet copié !")
                            onDismissRequest()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Tout copier", style = Type.caption.semibold, color = Palette.text)
                }

                Box(
                    Modifier
                        .clip(Capsule())
                        .background(Palette.accent)
                        .clickable(role = Role.Button) { onDismissRequest() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Fermer", style = Type.caption.bold, color = Palette.onAccent)
                }
            }
        }
    )
}

/**
 * Glass Dialog presenting detected Markdown code blocks with independent copy actions.
 */
@Composable
fun GlassCodeBlocksDialog(
    codeBlocks: List<MarkdownCodeBlock>,
    onDismissRequest: () -> Unit,
    onCopyBlock: (MarkdownCodeBlock) -> Unit,
    onCopyAll: () -> Unit
) {
    GlassAlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedRectangle(22.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("terminal", size = 20.dp, tint = Palette.electricBlue)
                Text("Blocs de code (${codeBlocks.size})", style = Type.title3.semibold, color = Palette.text)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                codeBlocks.forEachIndexed { index, block ->
                    val lang = block.language ?: "code"
                    val previewLines = block.code.lines().take(3).joinToString("\n")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedRectangle(12.dp))
                            .background(Palette.text.copy(alpha = 0.04f))
                            .border(0.5.dp, Palette.glassBorder, RoundedRectangle(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(Capsule())
                                    .background(Palette.electricBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = lang.uppercase(),
                                    style = Type.system(10f).bold,
                                    color = Palette.electricBlue
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(Capsule())
                                    .background(Palette.accent)
                                    .clickable(role = Role.Button) {
                                        onCopyBlock(block)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Symbol("doc.on.doc", size = 12.dp, tint = Palette.onAccent)
                                    Text(
                                        text = "Copier",
                                        style = Type.system(11f).bold,
                                        color = Palette.onAccent
                                    )
                                }
                            }
                        }

                        Text(
                            text = previewLines + if (block.code.lines().size > 3) "\n..." else "",
                            style = Type.caption2.monospaced,
                            color = Palette.text,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (codeBlocks.size > 1) {
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.text.copy(alpha = 0.08f))
                            .clickable(role = Role.Button) {
                                onCopyAll()
                                onDismissRequest()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Tout copier", style = Type.caption.semibold, color = Palette.text)
                    }
                }

                Box(
                    Modifier
                        .clip(Capsule())
                        .background(Palette.accent)
                        .clickable(role = Role.Button) { onDismissRequest() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Fermer", style = Type.caption.bold, color = Palette.onAccent)
                }
            }
        }
    )
}
