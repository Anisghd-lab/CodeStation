package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.PermissionPolicy
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.domain.model.WorkspaceMode
import com.ghdinteractivestudio.codestation.ui.components.AvatarPalette
import com.ghdinteractivestudio.codestation.ui.components.CardForm
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.ChoicePicker
import com.ghdinteractivestudio.codestation.ui.components.CodyncSwitch
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.tr
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.viewmodel.BotEditorViewModel

@Composable
fun BotEditorScreen(
    viewModel: BotEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        PageHeader(
            title = if (state.isNewBot) "Nouvel Agent" else "Modifier l'Agent",
            pop = onNavigateBack
        )

        CardForm(
            modifier = Modifier.weight(1f)
        ) {
            // Avatar Preview & Customization
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CharacterAvatar(
                    shape = state.avatarGlyph,
                    color = state.avatarColorHex,
                    size = 72.dp,
                    status = "working"
                )

                // Avatar Shape Picker
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (shape in AvatarPalette.shapes) {
                        val isSelected = state.avatarGlyph == shape
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(if (isSelected) Palette.accentFill else Palette.surface)
                                .clickable { viewModel.onAvatarGlyphChanged(shape) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                shape,
                                style = Type.caption.semibold,
                                color = if (isSelected) Palette.onAccent else Palette.text
                            )
                        }
                    }
                }

                // Avatar Color Picker
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((colorName, _) in AvatarPalette.colors) {
                        val isSelected = state.avatarColorHex == colorName
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(Capsule())
                                .background(AvatarPalette.tint(colorName))
                                .clickable { viewModel.onAvatarColorHexChanged(colorName) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Symbol("checkmark", size = 14.dp, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // General Info Section
            CardSection(title = "Identité") {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nom de l'Agent", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = state.name,
                            onValueChange = { viewModel.onNameChanged(it) },
                            textStyle = Type.body.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (state.name.isEmpty()) Text("ex: Claude Engineer", style = Type.body, color = Palette.tertiary)
                                    inner()
                                }
                            }
                        )
                    }
                }

                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Description courte", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = state.description,
                            onValueChange = { viewModel.onDescriptionChanged(it) },
                            textStyle = Type.body.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (state.description.isEmpty()) Text("ex: Expert en refactorisation", style = Type.body, color = Palette.tertiary)
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            // AI Provider & Settings
            CardSection(title = "Moteur & Modèle") {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fournisseur IA", style = Type.body, color = Palette.text, modifier = Modifier.weight(1f))
                        ChoicePicker(
                            selection = state.provider,
                            options = ProviderType.entries.map { it to it.displayName },
                            select = { viewModel.onProviderChanged(it) }
                        )
                    }
                }

                row {
                    CodyncSwitch(
                        checked = state.isFastMode,
                        onChange = { viewModel.onFastModeToggled(it) }
                    ) {
                        Column {
                            Text("Fast Mode (Prioritaire)", style = Type.body, color = Palette.text)
                            Text("Accélère les requêtes courtes", style = Type.caption, color = Palette.secondary)
                        }
                    }
                }

                row {
                    CodyncSwitch(
                        checked = state.isPinned,
                        onChange = { viewModel.onPinnedToggled(it) }
                    ) {
                        Text("Épingler en haut du roster", style = Type.body, color = Palette.text)
                    }
                }
            }

            // Instructions & System Prompt
            CardSection(title = "Instructions Système") {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Consignes comportementales", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = state.instructions,
                            onValueChange = { viewModel.onInstructionsChanged(it) },
                            textStyle = Type.subheadline.monospaced.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (state.instructions.isEmpty()) Text("Entrez le prompt système…", style = Type.body, color = Palette.tertiary)
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            // Save / Cancel buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    title = "Enregistrer l'Agent",
                    enabled = state.name.isNotBlank() && !state.isSaving,
                    action = {
                        viewModel.saveBot()
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
