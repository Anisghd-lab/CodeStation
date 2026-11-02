package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.ProviderType
import com.ghdinteractivestudio.codestation.ui.components.AvatarPalette
import com.ghdinteractivestudio.codestation.ui.components.CardForm
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.CodyncSwitch
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
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
    val antigravityCatalogState by (viewModel.antigravityCatalogState?.collectAsState() ?: remember { mutableStateOf(null) })
    var showDeleteConfirm by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(state.isSaved, state.isDeleted) {
        if (state.isSaved || state.isDeleted) {
            onNavigateBack()
        }
    }

    if (showDeleteConfirm) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer l'Agent", style = Type.title3.bold, color = Palette.danger) },
            text = { Text("Êtes-vous sûr de vouloir supprimer définitivement l'agent '${state.name}' ? Cette action est irréversible.", style = Type.body, color = Palette.text) },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(Palette.danger)
                        .clickable {
                            showDeleteConfirm = false
                            viewModel.deleteBot()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Supprimer", style = Type.caption.bold, color = Color.White)
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(Palette.text.copy(alpha = 0.1f))
                        .clickable { showDeleteConfirm = false }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Annuler", style = Type.caption.semibold, color = Palette.text)
                }
            }
        )
    }

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

        state.errorMessage?.let { err ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(Palette.danger.copy(alpha = 0.15f), RoundedRectangle(8.dp))
                    .border(1.dp, Palette.danger.copy(alpha = 0.5f), RoundedRectangle(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("exclamationmark.triangle.fill", size = 16.dp, tint = Palette.danger)
                    Text(err, style = Type.caption, color = Palette.danger, modifier = Modifier.weight(1f))
                }
            }
        }

        CardForm(
            modifier = Modifier.weight(1f),
            verticalSpacing = 14.dp
        ) {
            // Section 1: Identité & Avatar (Disposition compacte et équilibrée)
            CardSection(title = "Identité de l'Agent") {
                // Ligne supérieure : Avatar compact + Saisie du Nom
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CharacterAvatar(
                            shape = state.avatarGlyph,
                            color = state.avatarColorHex,
                            size = 54.dp,
                            status = "working"
                        )

                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Nom de l'Agent", style = Type.caption, color = Palette.secondary)
                            BasicTextField(
                                value = state.name,
                                onValueChange = { viewModel.onNameChanged(it) },
                                textStyle = Type.body.semibold.copy(color = Palette.text),
                                cursorBrush = SolidColor(Palette.accent),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    Box {
                                        if (state.name.isEmpty()) {
                                            Text("ex: OpenCode GLM", style = Type.body, color = Palette.tertiary)
                                        }
                                        inner()
                                    }
                                }
                            )
                        }
                    }
                }

                // Saisie de la description courte
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Rôle & description", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = state.description,
                            onValueChange = { viewModel.onDescriptionChanged(it) },
                            textStyle = Type.body.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (state.description.isEmpty()) {
                                        Text("ex: Spécialiste du code et des refactorisations", style = Type.body, color = Palette.tertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                // Sélecteur de forme d'avatar (défilement horizontal compact)
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Forme d'avatar", style = Type.caption, color = Palette.secondary)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (shape in AvatarPalette.shapes) {
                                val isSelected = state.avatarGlyph == shape
                                Box(
                                    Modifier
                                        .clip(Capsule())
                                        .background(if (isSelected) Palette.accentFill else Palette.surface)
                                        .clickable { viewModel.onAvatarGlyphChanged(shape) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        shape,
                                        style = Type.caption.semibold,
                                        color = if (isSelected) Palette.onAccent else Palette.text
                                    )
                                }
                            }
                        }
                    }
                }

                // Sélecteur de couleur (pastilles compactes)
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Couleur de badge", style = Type.caption, color = Palette.secondary)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for ((colorName, _) in AvatarPalette.colors) {
                                val isSelected = state.avatarColorHex == colorName
                                Box(
                                    Modifier
                                        .size(26.dp)
                                        .clip(Capsule())
                                        .background(AvatarPalette.tint(colorName))
                                        .clickable { viewModel.onAvatarColorHexChanged(colorName) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Symbol("checkmark", size = 12.dp, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Moteur & Modèle d'exécution (Cartes compactes, sans vide artificiel)
            CardSection(title = "Moteur & Modèle") {
                // Cartes de sélection du moteur
                row {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Registre Officiel des 6 CLI (CodeStation Execution Engine)",
                            style = Type.caption.semibold,
                            color = Palette.accent
                        )

                        val officialClis = listOf(
                            ProviderType.ANTIGRAVITY,
                            ProviderType.OPENCODE,
                            ProviderType.CODEBUDDY,
                            ProviderType.CLIXAD,
                            ProviderType.FREEBUFF,
                            ProviderType.OMNIRUSH
                        )
                        val otherProviders = ProviderType.entries.filter { it !in officialClis }

                        // Section 1: Registre Officiel des 6 CLI
                        officialClis.forEach { provider ->
                            val isSelected = state.provider == provider
                            val (badgeText, badgeColor) = when (provider) {
                                ProviderType.ANTIGRAVITY -> "Opérationnel (Gemini 4 Argon / Claude 5.5)" to Palette.emeraldAccent
                                ProviderType.OPENCODE -> "Opérationnel (GLM-5.3-Flash Free)" to Palette.emeraldAccent
                                ProviderType.CODEBUDDY -> "Opérationnel (Hy3 Free)" to Palette.emeraldAccent
                                ProviderType.CLIXAD -> "Opérationnel (GPT-5 Nano)" to Palette.emeraldAccent
                                ProviderType.FREEBUFF -> "Opérationnel (Free Auto - TUI)" to Palette.accent
                                ProviderType.OMNIRUSH -> "Configuré (GPT-6 Astra - Standby Cloud)" to Palette.warning
                                else -> "CLI Détectée" to Palette.emeraldAccent
                            }

                            val itemShape = RoundedRectangle(10.dp)
                            val radioShape = Capsule()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Palette.bubbleUser.copy(alpha = 0.5f) else Color.Transparent, itemShape)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) Palette.emeraldAccent.copy(alpha = 0.7f) else Palette.glassBorder,
                                        shape = itemShape
                                    )
                                    .clip(itemShape)
                                    .clickable { viewModel.onProviderChanged(provider) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Indicateur radio
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.5.dp,
                                            color = if (isSelected) Palette.emeraldAccent else Palette.tertiary,
                                            shape = radioShape
                                        )
                                        .clip(radioShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(Capsule())
                                                .background(Palette.emeraldAccent)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = provider.displayName,
                                            style = Type.body.semibold,
                                            color = if (isSelected) Palette.text else Palette.secondary
                                        )
                                        Text(
                                            text = badgeText,
                                            style = Type.system(9f).semibold,
                                            color = badgeColor
                                        )
                                    }
                                    Text(
                                        text = "Défaut : ${provider.defaultModel}",
                                        style = Type.caption,
                                        color = Palette.tertiary
                                    )
                                }

                                if (isSelected) {
                                    Symbol("checkmark", size = 15.dp, tint = Palette.emeraldAccent)
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Autres CLI détectées",
                            style = Type.caption,
                            color = Palette.secondary
                        )

                        // Section 2: Autres CLI
                        otherProviders.forEach { provider ->
                            val isSelected = state.provider == provider
                            val badgeText = when (provider) {
                                ProviderType.OPENAI_CODEX -> "Installée (Termux)"
                                else -> "À configurer"
                            }
                            val badgeColor = when (provider) {
                                ProviderType.OPENAI_CODEX -> Palette.accent
                                else -> Palette.tertiary
                            }

                            val itemShape = RoundedRectangle(10.dp)
                            val radioShape = Capsule()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Palette.bubbleUser.copy(alpha = 0.5f) else Color.Transparent, itemShape)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) Palette.emeraldAccent.copy(alpha = 0.7f) else Palette.glassBorder,
                                        shape = itemShape
                                    )
                                    .clip(itemShape)
                                    .clickable { viewModel.onProviderChanged(provider) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.5.dp,
                                            color = if (isSelected) Palette.emeraldAccent else Palette.tertiary,
                                            shape = radioShape
                                        )
                                        .clip(radioShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(Capsule())
                                                .background(Palette.emeraldAccent)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = provider.displayName,
                                            style = Type.body,
                                            color = if (isSelected) Palette.text else Palette.secondary
                                        )
                                        Text(
                                            text = badgeText,
                                            style = Type.system(9f),
                                            color = badgeColor
                                        )
                                    }
                                    Text(
                                        text = "Défaut : ${provider.defaultModel}",
                                        style = Type.caption,
                                        color = Palette.tertiary
                                    )
                                }

                                if (isSelected) {
                                    Symbol("checkmark", size = 15.dp, tint = Palette.emeraldAccent)
                                }
                            }
                        }
                    }
                }

                // Modèles compatibles avec le moteur choisi
                row {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Modèle pour ${state.provider.displayName}",
                                style = Type.caption,
                                color = Palette.secondary
                            )

                            if (state.provider == ProviderType.ANTIGRAVITY) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val lastUpdated = antigravityCatalogState?.lastUpdated
                                    if (lastUpdated != null && lastUpdated > 0L) {
                                        val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                                        Text(
                                            text = "Màj ${timeFormat.format(java.util.Date(lastUpdated))}",
                                            style = Type.system(10f),
                                            color = Palette.tertiary
                                        )
                                    }

                                    val isRefreshing = antigravityCatalogState?.isRefreshing == true
                                    if (isRefreshing) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = Palette.emeraldAccent
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedRectangle(4.dp))
                                                .clickable { viewModel.refreshAntigravityModels(force = true) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Symbol("arrow.clockwise", size = 11.dp, tint = Palette.emeraldAccent)
                                            Text(
                                                text = "Actualiser",
                                                style = Type.system(10f).semibold,
                                                color = Palette.emeraldAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Message d'erreur ou d'avertissement catalogue si applicable
                        if (state.provider == ProviderType.ANTIGRAVITY && antigravityCatalogState?.errorMessage != null) {
                            val alertShape = RoundedRectangle(6.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Palette.warning.copy(alpha = 0.12f), alertShape)
                                    .border(0.5.dp, Palette.warning.copy(alpha = 0.4f), alertShape)
                                    .clip(alertShape)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Symbol("exclamationmark.triangle", size = 12.dp, tint = Palette.warning)
                                Text(
                                    text = antigravityCatalogState?.errorMessage.orEmpty(),
                                    style = Type.system(10f),
                                    color = Palette.warning
                                )
                            }
                        }

                        // Alerte si le modèle courant n'est pas vérifié
                        if (state.provider == ProviderType.ANTIGRAVITY && !viewModel.isCurrentModelVerified()) {
                            val unverifiedShape = RoundedRectangle(6.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Palette.warning.copy(alpha = 0.12f), unverifiedShape)
                                    .border(0.5.dp, Palette.warning.copy(alpha = 0.5f), unverifiedShape)
                                    .clip(unverifiedShape)
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Symbol("exclamationmark.triangle.fill", size = 13.dp, tint = Palette.warning)
                                Text(
                                    text = "Modèle non vérifié par agy : '${state.model}'. Il sera transmis tel quel.",
                                    style = Type.system(10f).semibold,
                                    color = Palette.warning
                                )
                            }
                        }

                        val rawModels = if (state.provider == ProviderType.ANTIGRAVITY) {
                            antigravityCatalogState?.models?.takeIf { it.isNotEmpty() } ?: state.provider.getAvailableModels()
                        } else {
                            state.provider.getAvailableModels()
                        }
                        val availableModels = if (state.model.isNotBlank() && rawModels.none { it.id == state.model }) {
                            listOf(
                                com.ghdinteractivestudio.codestation.domain.model.ModelInfo(
                                    id = state.model,
                                    displayName = "${state.model} (Non vérifié)",
                                    isFree = false,
                                    isVerified = false
                                )
                            ) + rawModels
                        } else {
                            rawModels
                        }

                        if (availableModels.isNotEmpty()) {
                            availableModels.forEach { modelInfo ->
                                val isModelSelected = state.model == modelInfo.id
                                val modelShape = RoundedRectangle(8.dp)
                                val modelBg = if (isModelSelected) Palette.emeraldAccent.copy(alpha = 0.12f) else Palette.glassSurface
                                val modelBorderWidth = if (isModelSelected) 1.dp else 0.5.dp
                                val modelBorderColor = if (isModelSelected) Palette.emeraldAccent else Palette.glassBorder
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(modelBg, modelShape)
                                        .border(modelBorderWidth, modelBorderColor, modelShape)
                                        .clip(modelShape)
                                        .clickable { viewModel.onModelChanged(modelInfo.id) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = modelInfo.displayName,
                                                style = Type.caption.semibold,
                                                color = if (isModelSelected) Palette.text else Palette.secondary
                                            )
                                            if (modelInfo.isFree) {
                                                Text(
                                                    text = "Gratuit",
                                                    style = Type.system(9f).semibold,
                                                    color = Palette.emeraldAccent
                                                )
                                            } else if (!modelInfo.isVerified) {
                                                Text(
                                                    text = "Non vérifié",
                                                    style = Type.system(9f).semibold,
                                                    color = Palette.warning
                                                )
                                            }
                                        }
                                        Text(
                                            text = modelInfo.id,
                                            style = Type.system(10f).monospaced,
                                            color = Palette.tertiary
                                        )
                                    }
                                    if (isModelSelected) {
                                        Symbol("checkmark", size = 14.dp, tint = Palette.emeraldAccent)
                                    }
                                }
                            }
                        }

                        // Saisie libre d'identifiant de modèle si besoin
                        BasicTextField(
                            value = state.model,
                            onValueChange = { viewModel.onModelChanged(it) },
                            textStyle = Type.footnote.monospaced.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (state.model.isEmpty()) {
                                        Text(state.provider.defaultModel, style = Type.footnote.monospaced, color = Palette.tertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: Comportement & Options (Interrupteurs directement accolés)
            CardSection(title = "Options d'exécution") {
                // Fast Mode
                row {
                    CodyncSwitch(
                        checked = state.isFastMode,
                        onChange = { viewModel.onFastModeToggled(it) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Fast Mode (Prioritaire)", style = Type.body, color = Palette.text)
                            Text("Accélère les requêtes courtes et réduit la latence", style = Type.caption, color = Palette.secondary)
                        }
                    }
                }

                // Épingler en haut
                row {
                    CodyncSwitch(
                        checked = state.isPinned,
                        onChange = { viewModel.onPinnedToggled(it) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Épingler en haut du roster", style = Type.body, color = Palette.text)
                            Text("Affiche cet agent en tête de liste sur l'écran d'accueil", style = Type.caption, color = Palette.secondary)
                        }
                    }
                }
            }

            // Section 4: Instructions Système
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
                                .heightIn(min = 60.dp, max = 110.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (state.instructions.isEmpty()) {
                                        Text("Entrez les directives spécifiques de l'agent…", style = Type.body, color = Palette.tertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            if (!state.isNewBot && !state.isLead) {
                CardSection(title = "Gestion de l'Agent") {
                    row {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedRectangle(10.dp))
                                .border(1.dp, Palette.danger.copy(alpha = 0.5f), RoundedRectangle(10.dp))
                                .background(Palette.danger.copy(alpha = 0.08f))
                                .clickable { showDeleteConfirm = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Symbol("trash.fill", size = 18.dp, tint = Palette.danger)
                            Text(
                                "Supprimer définitivement cet agent",
                                style = Type.body.semibold,
                                color = Palette.danger
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        // Barre d'actions inférieure fixe (toujours accessible même avec clavier ouvert)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            color = Palette.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecondaryButton(
                    title = "Annuler",
                    action = onNavigateBack
                )
                PrimaryButton(
                    title = if (state.isNewBot) "Créer l'Agent" else "Enregistrer l'Agent",
                    enabled = state.name.isNotBlank() && !state.isSaving,
                    action = {
                        viewModel.saveBot()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
