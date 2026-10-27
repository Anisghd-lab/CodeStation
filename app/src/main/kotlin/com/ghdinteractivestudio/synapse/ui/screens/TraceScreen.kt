package com.ghdinteractivestudio.synapse.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.ExecutionStep
import com.ghdinteractivestudio.synapse.domain.model.StepType
import com.ghdinteractivestudio.synapse.ui.components.CardSection
import com.ghdinteractivestudio.synapse.ui.components.CodeBox
import com.ghdinteractivestudio.synapse.ui.components.Disclosure
import com.ghdinteractivestudio.synapse.ui.components.ModalHeader
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.ThinkingOrb
import com.ghdinteractivestudio.synapse.ui.components.tr
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold
import com.ghdinteractivestudio.synapse.ui.theme.semibold

@Composable
fun TraceScreen(
    steps: List<ExecutionStep>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        ModalHeader(
            title = "Full conversation",
            onClose = onNavigateBack
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (steps.isEmpty()) {
                item {
                    Text(
                        tr("Nothing yet."),
                        style = Type.body,
                        color = Palette.tertiary,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Turn execution steps (${steps.size})",
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

    when (step.type) {
        StepType.THINKING -> {
            Disclosure(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                label = {
                    ThinkingOrb(size = 16.dp, color = Palette.secondary, animated = false)
                    Text(tr("Thinking"), Modifier.padding(start = 8.dp), style = Type.subheadline, color = Palette.secondary)
                }
            ) {
                Text(
                    step.description,
                    style = Type.footnote,
                    color = Palette.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        StepType.TOOL_CALL -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("terminal", size = 16.dp, tint = Palette.text)
                    Text(step.title, style = Type.subheadline.semibold, color = Palette.text)
                }
                CodeBox(step.description)
            }
        }
        StepType.PLANNING -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Symbol("checklist", size = 16.dp, tint = Palette.text)
                    Text(tr("Plan"), style = Type.subheadline.bold, color = Palette.text)
                }
                Text(step.description, style = Type.footnote, color = Palette.text)
            }
        }
        else -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Symbol("doc.text", size = 16.dp, tint = Palette.secondary)
                Text(step.description, style = Type.subheadline, color = Palette.text)
            }
        }
    }
}
