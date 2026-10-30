package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.ExecutionStep
import com.ghdinteractivestudio.codestation.domain.model.StepType
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CodeBox
import com.ghdinteractivestudio.codestation.ui.components.Disclosure
import com.ghdinteractivestudio.codestation.ui.components.IconButton
import com.ghdinteractivestudio.codestation.ui.components.ModalHeader
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.ThinkingOrb
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TraceScreen(
    steps: List<ExecutionStep>,
    onNavigateBack: () -> Unit,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        ModalHeader(
            title = "Traces d'exécution",
            onClose = onNavigateBack,
            trailing = {
                if (steps.isNotEmpty() && onClear != null) {
                    IconButton(
                        title = "Effacer",
                        systemImage = "trash",
                        action = onClear
                    )
                }
            }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (steps.isEmpty()) {
                item {
                    CardSection {
                        row {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Symbol("doc.text", size = 32.dp, tint = Palette.tertiary)
                                Text(
                                    "Aucune trace enregistrée",
                                    style = Type.body.semibold,
                                    color = Palette.text
                                )
                                Text(
                                    "Les étapes d'exécution, journaux système et raisonnements de l'agent s'afficheront ici en temps réel lors de vos échanges.",
                                    style = Type.footnote,
                                    color = Palette.tertiary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Étapes d'exécution & Raisonnement (${steps.size})",
                            Modifier.padding(start = 4.dp),
                            style = Type.subheadline.semibold,
                            color = Palette.text
                        )

                        CardSection {
                            for (step in steps) {
                                row {
                                    TraceStepRow(step)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceStepRow(step: ExecutionStep) {
    var expanded by remember { mutableStateOf(false) }
    val timeStr = remember(step.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(step.timestamp))
    }

    when (step.type) {
        StepType.THINKING, StepType.THOUGHT -> {
            Disclosure(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            ThinkingOrb(size = 16.dp, color = Palette.secondary, animated = false)
                            Text(
                                step.title.ifBlank { "Raisonnement de l'agent" },
                                Modifier.padding(start = 8.dp),
                                style = Type.subheadline.semibold,
                                color = Palette.text
                            )
                        }
                        Text(
                            timeStr,
                            style = Type.caption2,
                            color = Palette.tertiary
                        )
                    }
                }
            ) {
                Text(
                    step.content,
                    style = Type.footnote,
                    color = Palette.secondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        StepType.TOOL_CALL, StepType.TOOL_RESULT -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Symbol(
                            if (step.isError) "exclamationmark.triangle" else "terminal",
                            size = 16.dp,
                            tint = if (step.isError) Palette.danger else Palette.emeraldAccent
                        )
                        Text(
                            step.title,
                            style = Type.subheadline.semibold,
                            color = if (step.isError) Palette.danger else Palette.text
                        )
                    }
                    Text(
                        timeStr,
                        style = Type.caption2,
                        color = Palette.tertiary
                    )
                }
                if (step.content.isNotBlank()) {
                    CodeBox(step.content)
                }
            }
        }
        StepType.PLANNING -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Symbol("checklist", size = 16.dp, tint = Palette.emeraldAccent)
                        Text(
                            step.title.ifBlank { "Planification" },
                            style = Type.subheadline.bold,
                            color = Palette.text
                        )
                    }
                    Text(
                        timeStr,
                        style = Type.caption2,
                        color = Palette.tertiary
                    )
                }
                Text(step.content, style = Type.footnote, color = Palette.secondary)
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconName = if (step.isError) {
                        "exclamationmark.circle"
                    } else when {
                        step.title.contains("Terminé", ignoreCase = true) || step.title.contains("validé", ignoreCase = true) -> "checkmark.circle"
                        step.title.contains("Processus", ignoreCase = true) || step.title.contains("CLI", ignoreCase = true) -> "gearshape"
                        step.title.contains("Agent", ignoreCase = true) -> "person"
                        step.title.contains("Envoi", ignoreCase = true) || step.title.contains("Requête", ignoreCase = true) -> "paperplane"
                        else -> "doc.text"
                    }
                    val iconColor = if (step.isError) Palette.danger else Palette.secondary

                    Symbol(iconName, size = 16.dp, tint = iconColor)

                    Column {
                        Text(
                            step.title,
                            style = Type.subheadline.semibold,
                            color = if (step.isError) Palette.danger else Palette.text
                        )
                        if (step.content.isNotBlank() && step.content != "Étape validée avec succès") {
                            Text(
                                step.content,
                                style = Type.caption,
                                color = Palette.secondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    timeStr,
                    style = Type.caption2,
                    color = Palette.tertiary
                )
            }
        }
    }
}
