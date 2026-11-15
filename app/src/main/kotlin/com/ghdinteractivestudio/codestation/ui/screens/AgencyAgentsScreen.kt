package com.ghdinteractivestudio.codestation.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.AgencyAgent
import com.ghdinteractivestudio.codestation.domain.model.AgencyAgentCatalog
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.GlassStyle
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.glass
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun AgencyAgentsScreen(
    onNavigateBack: (() -> Unit)? = null,
    bottomInset: androidx.compose.ui.unit.Dp = 96.dp,
    modifier: Modifier = Modifier,
    includeStatusBarPadding: Boolean = onNavigateBack != null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDivision by rememberSaveable { mutableStateOf("Toutes") }
    var selectedAgentForDetail by remember { mutableStateOf<AgencyAgent?>(null) }

    val allAgents = remember { AgencyAgentCatalog.agents }
    val divisions = remember { AgencyAgentCatalog.divisions }

    val filteredAgents = remember(searchQuery, selectedDivision) {
        allAgents.filter { agent ->
            val matchesDivision = selectedDivision == "Toutes" || agent.division.equals(selectedDivision, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                agent.name.lowercase().contains(q) ||
                agent.slug.lowercase().contains(q) ||
                agent.division.lowercase().contains(q) ||
                agent.description.lowercase().contains(q) ||
                agent.role.lowercase().contains(q)
            matchesDivision && matchesSearch
        }
    }

    val baseModifier = if (includeStatusBarPadding) {
        modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    } else {
        modifier
            .fillMaxSize()
            .background(Palette.background)
    }

    Column(
        modifier = baseModifier
    ) {
        // En-tête
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onNavigateBack != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .glass(Capsule())
                        .clickable(role = Role.Button, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("chevron.left", size = 18.dp, tint = Palette.text)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "The Agency",
                        style = Type.title2.bold,
                        color = Palette.text
                    )
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(Palette.electricBlue.copy(alpha = 0.2f))
                            .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.5f), Capsule())
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "283 Experts",
                            style = Type.system(10f).bold,
                            color = Palette.electricBlue
                        )
                    }
                }
                Text(
                    "Catalogue des compétences et spécialistes autonomes",
                    style = Type.caption,
                    color = Palette.secondary
                )
            }
        }

        // Barre de Recherche Liquid Glass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .glass(RoundedRectangle(14.dp), style = GlassStyle.Card)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("magnifyingglass", size = 16.dp, tint = Palette.secondary)
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = Type.body.copy(color = Palette.text),
                    cursorBrush = SolidColor(Palette.electricBlue),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Rechercher parmi les 283 agents (nom, division, mot-clé)...",
                                style = Type.body,
                                color = Palette.secondary.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(Capsule())
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Symbol("xmark.circle.fill", size = 16.dp, tint = Palette.secondary)
                    }
                }
            }
        }

        // Filtres par Division (Scroll Horizontal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            divisions.forEach { div ->
                val isSelected = selectedDivision == div
                val count = if (div == "Toutes") allAgents.size else allAgents.count { it.division.equals(div, ignoreCase = true) }
                val chipShape = Capsule()

                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(
                            if (isSelected) Palette.electricBlue.copy(alpha = 0.25f)
                            else Palette.surface.copy(alpha = 0.6f)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) Palette.electricBlue else Palette.border,
                            shape = chipShape
                        )
                        .clickable { selectedDivision = div }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            div,
                            style = Type.system(11f).semibold,
                            color = if (isSelected) Palette.text else Palette.secondary
                        )
                        Box(
                            modifier = Modifier
                                .clip(Capsule())
                                .background(if (isSelected) Palette.electricBlue.copy(alpha = 0.5f) else Palette.border.copy(alpha = 0.5f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "$count",
                                style = Type.system(9f).bold,
                                color = if (isSelected) Palette.text else Palette.secondary
                            )
                        }
                    }
                }
            }
        }

        // Compteur de résultats
        Text(
            text = "${filteredAgents.size} agent${if (filteredAgents.size > 1) "s" else ""} trouvé${if (filteredAgents.size > 1) "s" else ""}",
            style = Type.caption.semibold,
            color = Palette.secondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Liste des cartes d'agents
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredAgents, key = { it.id }) { agent ->
                AgencyAgentCard(
                    agent = agent,
                    onClick = { selectedAgentForDetail = agent }
                )
            }
        }
    }

    // Dialogue Détails de l'Agent
    selectedAgentForDetail?.let { agent ->
        val promptText = "🎭 [AGENCY ACTIVATION : ${agent.name} (${agent.division})]\n🎯 Rôle : ${agent.role}\n📂 Skill : ${agent.skillPath}\n\nApplique l'excellence, les méthodes et les critères stricts de cet agent spécialisé."

        GlassAlertDialog(
            onDismissRequest = { selectedAgentForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CharacterAvatar(shape = "hexagon", color = "#00D2FF", size = 38.dp)
                    Column {
                        Text(agent.name, style = Type.title3.bold, color = Palette.text)
                        Text(agent.division, style = Type.caption, color = Palette.electricBlue)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        agent.description.ifBlank { agent.role },
                        style = Type.body,
                        color = Palette.text
                    )

                    // Emplacement du skill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(RoundedRectangle(8.dp), style = GlassStyle.Card)
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Fichier compétence (SKILL.md) :", style = Type.caption.semibold, color = Palette.secondary)
                            Text(
                                agent.skillPath,
                                style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = Palette.electricBlue
                            )
                        }
                    }

                    // Directives d'activation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(RoundedRectangle(8.dp), style = GlassStyle.Card)
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Directive d'activation auto :", style = Type.caption.semibold, color = Palette.secondary)
                            Text(
                                "Activé automatiquement par le superviseur CAMCS et dans tout chat CodeStation dès qu'une tâche correspondante est soumise.",
                                style = Type.caption,
                                color = Palette.secondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    title = "Copier la directive",
                    action = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Agency Activation", promptText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Directive de l'agent copiée !", Toast.LENGTH_SHORT).show()
                        selectedAgentForDetail = null
                    }
                )
            },
            dismissButton = {
                SecondaryButton(
                    title = "Fermer",
                    action = { selectedAgentForDetail = null }
                )
            }
        )
    }
}

@Composable
private fun AgencyAgentCard(
    agent: AgencyAgent,
    onClick: () -> Unit
) {
    val cardShape = RoundedRectangle(14.dp)
    val divisionColor = when (agent.division) {
        "Qualité & Tests" -> Palette.emeraldAccent
        "Cybersécurité" -> Palette.danger
        "Ingénierie & Dev" -> Palette.electricBlue
        "Architecture & Systèmes" -> Palette.turquoise
        "Design & UI/UX" -> Color(0xFFA855F7)
        "Data & AI" -> Color(0xFFEC4899)
        "GIS & Spatial" -> Color(0xFF10B981)
        "Game Development" -> Color(0xFFF59E0B)
        else -> Palette.secondary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(cardShape, style = GlassStyle.Card)
            .border(0.5.dp, Palette.glassBorder, cardShape)
            .clip(cardShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Ligne supérieure : Division + Tag Statut
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(divisionColor.copy(alpha = 0.15f))
                        .border(0.5.dp, divisionColor.copy(alpha = 0.4f), Capsule())
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        agent.division,
                        style = Type.system(10f).semibold,
                        color = divisionColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(Capsule())
                            .background(Palette.emeraldAccent)
                    )
                    Text(
                        "Prêt en CLI",
                        style = Type.system(9f).semibold,
                        color = Palette.emeraldAccent
                    )
                }
            }

            // Nom de l'Agent
            Text(
                text = agent.name,
                style = Type.title3.bold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description / Rôle
            Text(
                text = agent.description.ifBlank { agent.role },
                style = Type.caption,
                color = Palette.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Pied de carte : Slug monospace
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = agent.slug,
                    style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = Palette.electricBlue.copy(alpha = 0.8f)
                )

                Symbol("arrow.up.right", size = 12.dp, tint = Palette.secondary)
            }
        }
    }
}
