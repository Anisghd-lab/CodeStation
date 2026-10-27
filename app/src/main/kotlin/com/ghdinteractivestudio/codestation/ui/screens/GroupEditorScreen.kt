package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.ui.components.AvatarFace
import com.ghdinteractivestudio.codestation.ui.components.CardForm
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.GroupAvatar
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.tr
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun GroupEditorScreen(
    availableBots: List<Bot>,
    onCreateGroup: (String, String, List<String>) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedBotIds = remember { mutableStateListOf<String>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        PageHeader(
            title = "Nouveau Groupe",
            pop = onNavigateBack
        )

        CardForm(modifier = Modifier.weight(1f)) {
            // Group Avatar Preview
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val selectedBots = availableBots.filter { selectedBotIds.contains(it.id) }
                val faces = selectedBots.map {
                    AvatarFace(it.avatarGlyph, it.avatarColorHex, it.status.name.lowercase())
                }
                GroupAvatar(faces = faces, size = 68.dp)
                Text(
                    "${selectedBotIds.size} agents participants",
                    style = Type.caption.medium,
                    color = Palette.secondary
                )
            }

            // Group Info Section
            CardSection(title = "Groupe") {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nom du groupe", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = Type.body.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (name.isEmpty()) Text("ex: Équipe Refactoring", style = Type.body, color = Palette.tertiary)
                                    inner()
                                }
                            }
                        )
                    }
                }

                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Description", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = description,
                            onValueChange = { description = it },
                            textStyle = Type.body.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (description.isEmpty()) Text("Objectif commun des agents…", style = Type.body, color = Palette.tertiary)
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            // Member Selection Section
            CardSection(
                title = "Sélection des Agents",
                footer = "Dans une conversation de groupe, mentionnez un agent avec @ pour lui poser une question ciblée."
            ) {
                for (bot in availableBots) {
                    val isSelected = selectedBotIds.contains(bot.id)
                    row {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedBotIds.remove(bot.id)
                                    else selectedBotIds.add(bot.id)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Symbol(
                                if (isSelected) "checkmark.circle.fill" else "circle",
                                size = 20.dp,
                                tint = if (isSelected) Palette.accent else Palette.tertiary
                            )
                            Column(Modifier.weight(1f)) {
                                Text(bot.name, style = Type.body.semibold, color = Palette.text)
                                Text(bot.description.ifEmpty { bot.provider.displayName }, style = Type.caption, color = Palette.secondary)
                            }
                        }
                    }
                }
            }

            // Save Buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    title = "Créer le Groupe",
                    enabled = name.isNotBlank() && selectedBotIds.isNotEmpty(),
                    action = {
                        onCreateGroup(name, description, selectedBotIds.toList())
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    title = "Annuler",
                    action = onNavigateBack
                )
            }
        }
    }
}
