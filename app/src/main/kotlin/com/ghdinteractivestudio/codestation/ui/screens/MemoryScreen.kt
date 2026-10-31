package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.domain.model.MemorySearchResultItem
import com.ghdinteractivestudio.codestation.ui.components.DynamicNeuralGraphView
import com.ghdinteractivestudio.codestation.ui.components.GraphNode3D
import com.ghdinteractivestudio.codestation.ui.components.GraphNodeType
import com.ghdinteractivestudio.codestation.ui.components.GlassStage
import com.ghdinteractivestudio.codestation.ui.components.GlassStyle
import com.ghdinteractivestudio.codestation.ui.components.NeuralAvatar
import com.ghdinteractivestudio.codestation.ui.components.Spinner
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.glass
import com.ghdinteractivestudio.codestation.ui.components.pressScale
import com.ghdinteractivestudio.codestation.ui.components.rememberPress
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.viewmodel.MemoryViewModel

/**
 * Dedicated CodeStation Collective Memory Screen.
 * Provides rich visualization, real metrics, search, filtering, detail views,
 * and GitHub synchronization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: MemoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var showDetailSheet by remember { mutableStateOf(false) }
    var selectedGraphNode by remember { mutableStateOf<GraphNode3D?>(null) }

    GlassStage(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background),
        chrome = {
            // Top Navigation Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp)
                    .padding(horizontal = 16.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Box(
                    Modifier
                        .size(40.dp)
                        .glass(Capsule())
                        .clickable(role = Role.Button, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("arrow.left", size = 18.dp, tint = Palette.text)
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "Mémoire",
                    style = Type.title3.semibold,
                    color = Palette.text,
                    modifier = Modifier.weight(1f)
                )

                // Sync Action Button
                Box(
                    Modifier
                        .height(40.dp)
                        .glass(Capsule(), style = GlassStyle.Regular)
                        .clickable(role = Role.Button, onClick = { viewModel.triggerSync() })
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.isSyncing) {
                            Spinner(size = 14.dp)
                        } else {
                            Symbol("arrow.triangle.2.circlepath", size = 16.dp, tint = Palette.electricBlue)
                        }
                        Text(
                            text = if (state.isSyncing) "Synchro…" else "Synchroniser",
                            style = Type.caption.semibold,
                            color = Palette.electricBlue
                        )
                    }
                }
            }
        }
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topInset + 46.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Dynamic Neural Network Graph Section (Compact 3D Smoked Glass Sphere)
            item("neural_graph") {
                DynamicNeuralGraphView(
                    experiences = state.allExperiences,
                    knowledge = state.allKnowledge,
                    onSelectExperience = { exp ->
                        viewModel.selectExperience(exp)
                        showDetailSheet = true
                    },
                    onSelectKnowledge = { knw ->
                        viewModel.selectKnowledge(knw)
                        showDetailSheet = true
                    },
                    onNodeSelected = { node ->
                        selectedGraphNode = node
                    },
                    height = 230.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                )
            }

            // 2. Real Metrics Grid
            item("metrics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Expériences",
                        value = state.status.totalExperiences.toString(),
                        accentColor = Palette.added,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Connaissances",
                        value = state.status.totalKnowledge.toString(),
                        accentColor = Palette.electricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Relations",
                        value = state.status.totalRelations.toString(),
                        accentColor = Color(0xFF9D4EDD),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Agents",
                        value = state.status.totalAgents.toString(),
                        accentColor = Palette.warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Search Bar & Filter Chips
            item("search_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glass(Capsule(), style = GlassStyle.Card)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol("magnifyingglass", size = 16.dp, tint = Palette.secondary)
                        Box(modifier = Modifier.weight(1f)) {
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "Rechercher une solution, un tag…",
                                    style = Type.body,
                                    color = Palette.tertiary
                                )
                            }
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                textStyle = Type.body.copy(color = Palette.text),
                                cursorBrush = SolidColor(Palette.electricBlue),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (state.searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.onSearchQueryChanged("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Symbol("xmark.circle.fill", size = 16.dp, tint = Palette.tertiary)
                            }
                        }
                    }

                    // Filter Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                label = "Tous",
                                isSelected = state.selectedFilter == "ALL",
                                onClick = { viewModel.onFilterSelected("ALL") }
                            )
                        }
                        item {
                            FilterChip(
                                label = "Solutions validées",
                                isSelected = state.selectedFilter == "VALIDATED",
                                onClick = { viewModel.onFilterSelected("VALIDATED") }
                            )
                        }
                        item {
                            FilterChip(
                                label = "Expériences",
                                isSelected = state.selectedFilter == "EXPERIENCES",
                                onClick = { viewModel.onFilterSelected("EXPERIENCES") }
                            )
                        }
                        item {
                            FilterChip(
                                label = "Connaissances",
                                isSelected = state.selectedFilter == "KNOWLEDGE",
                                onClick = { viewModel.onFilterSelected("KNOWLEDGE") }
                            )
                        }
                    }
                }
            }

            // 4. Items List Header
            item("list_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Éléments mémorisés (${state.items.size})",
                        style = Type.footnote.semibold,
                        color = Palette.secondary
                    )
                    if (state.isLoading) {
                        Spinner(size = 14.dp)
                    }
                }
            }

            // 5. Items List
            if (state.items.isEmpty() && !state.isLoading) {
                item("empty_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Symbol("doc.text.magnifyingglass", size = 36.dp, tint = Palette.tertiary)
                            Text(
                                text = if (state.searchQuery.isNotEmpty()) "Aucun résultat trouvé" else "Mémoire vide",
                                style = Type.body.semibold,
                                color = Palette.secondary
                            )
                        }
                    }
                }
            } else {
                items(state.items, key = { it.id }) { item ->
                    MemoryItemCard(
                        item = item,
                        onClick = {
                            viewModel.selectResultItem(item)
                            showDetailSheet = true
                        }
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (showDetailSheet && (state.selectedExperience != null || state.selectedKnowledge != null)) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = sheetState,
            containerColor = Palette.surface,
            scrimColor = Color.Black.copy(alpha = 0.55f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.selectedExperience != null) {
                    ExperienceDetailView(exp = state.selectedExperience!!)
                } else if (state.selectedKnowledge != null) {
                    KnowledgeDetailView(knw = state.selectedKnowledge!!)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Node Quick Detail Bottom Sheet (Appears cleanly below the graph on Android)
    if (selectedGraphNode != null) {
        val node = selectedGraphNode!!
        val exp = if (node.type == GraphNodeType.EXPERIENCE) {
            state.allExperiences.find { it.id == node.dataRefId }
        } else null
        val knw = if (node.type == GraphNodeType.KNOWLEDGE) {
            state.allKnowledge.find { it.id == node.dataRefId }
        } else null

        NodeQuickDetailSheet(
            node = node,
            experience = exp,
            knowledge = knw,
            onOpenFullDetail = {
                if (exp != null) {
                    viewModel.selectExperience(exp)
                    showDetailSheet = true
                } else if (knw != null) {
                    viewModel.selectKnowledge(knw)
                    showDetailSheet = true
                }
                selectedGraphNode = null
            },
            onDismiss = { selectedGraphNode = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeQuickDetailSheet(
    node: GraphNode3D,
    experience: MemoryExperience?,
    knowledge: MemoryKnowledge?,
    onOpenFullDetail: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryTitle = when (node.type) {
        GraphNodeType.AGENT -> "Agent IA"
        GraphNodeType.PROJECT -> "Cœur Neuronal"
        GraphNodeType.EXPERIENCE -> "Expérience"
        GraphNodeType.KNOWLEDGE -> "Connaissance Validée"
        GraphNodeType.INTERNEURON -> "Interneurone Synaptique"
    }

    val displayTitle = experience?.title ?: knowledge?.title ?: node.label
    val author = experience?.agent ?: (if (node.type == GraphNodeType.AGENT) node.label else "CodeStation Memory")
    val validationStatus = experience?.validationStatus ?: knowledge?.validationStatus ?: "Connecté"
    val isValidated = validationStatus in listOf("validated", "validated_solution", "active")

    val shortDescription = when {
        experience != null -> experience.solution.ifBlank { experience.problem }.take(220)
        knowledge != null -> knowledge.content.take(220)
        else -> node.subtitle
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.surface,
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Category Badge + Status Badge + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Indicator Chip
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(node.color.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(node.color)
                            )
                            Text(
                                text = categoryTitle,
                                style = Type.caption.semibold,
                                color = node.color
                            )
                        }
                    }

                    // Validation Status Badge
                    if (isValidated) {
                        Box(
                            modifier = Modifier
                                .clip(Capsule())
                                .background(Palette.added.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Validée",
                                style = Type.caption.semibold,
                                color = Palette.added
                            )
                        }
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("xmark.circle.fill", size = 22.dp, tint = Palette.tertiary)
                }
            }

            // Node Name / Title (Full horizontal, natural wrap, never vertical)
            Text(
                text = displayTitle,
                style = Type.title3.semibold,
                color = Palette.text,
                modifier = Modifier.fillMaxWidth()
            )

            // Author and ID Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("person.crop.circle", size = 14.dp, tint = Palette.secondary)
                Text(
                    text = "Auteur : $author",
                    style = Type.subheadline,
                    color = Palette.secondary
                )
                Text(
                    text = "• ${node.id}",
                    style = Type.caption,
                    color = Palette.tertiary
                )
            }

            // Short Description Box
            if (shortDescription.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedRectangle(12.dp), style = GlassStyle.Card)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Description & Contexte",
                        style = Type.caption.semibold,
                        color = Palette.electricBlue
                    )
                    Text(
                        text = shortDescription,
                        style = Type.body,
                        color = Palette.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (experience != null || knowledge != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .glass(Capsule(), style = GlassStyle.Regular)
                            .clickable(role = Role.Button, onClick = onOpenFullDetail)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Symbol("doc.text", size = 16.dp, tint = Palette.added)
                            Text(
                                text = "Détails complets",
                                style = Type.body.semibold,
                                color = Palette.added
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .then(if (experience != null || knowledge != null) Modifier.width(100.dp) else Modifier.fillMaxWidth())
                        .height(44.dp)
                        .glass(Capsule(), style = GlassStyle.Card)
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Fermer",
                        style = Type.body,
                        color = Palette.secondary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .glass(RoundedRectangle(14.dp), style = GlassStyle.Card)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = Type.title2.semibold,
            color = accentColor
        )
        Text(
            text = title,
            style = Type.caption.copy(fontSize = 11.sp),
            color = Palette.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (source, pressed) = rememberPress()
    Box(
        modifier = Modifier
            .pressScale(pressed)
            .glass(
                Capsule(),
                style = if (isSelected) GlassStyle.Regular else GlassStyle.Card,
                pressed = pressed
            )
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = Type.caption.semibold,
            color = if (isSelected) Palette.electricBlue else Palette.secondary
        )
    }
}

@Composable
private fun MemoryItemCard(
    item: MemorySearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (source, pressed) = rememberPress()
    val isExperience = item.itemType == "experience"
    val badgeColor = if (isExperience) Palette.added else Palette.electricBlue

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressed)
            .glass(RoundedRectangle(16.dp), style = GlassStyle.Card, pressed = pressed)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(badgeColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExperience) "Expérience" else "Connaissance",
                        style = Type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = badgeColor
                    )
                }

                if (item.validationStatus == "validated_solution") {
                    Box(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(Palette.added.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Validée",
                            style = Type.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                            color = Palette.added
                        )
                    }
                }
            }

            Symbol("chevron.right", size = 12.dp, tint = Palette.tertiary)
        }

        // Title
        Text(
            text = item.title,
            style = Type.body.semibold,
            color = Palette.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Summary
        Text(
            text = item.summary,
            style = Type.subheadline,
            color = Palette.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Tags Row
        if (item.tags.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                item.tags.take(3).forEach { tag ->
                    Text(
                        text = "#$tag",
                        style = Type.caption.copy(fontSize = 11.sp),
                        color = Palette.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperienceDetailView(exp: MemoryExperience) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = exp.title,
            style = Type.title2.semibold,
            color = Palette.text
        )

        DetailSection(title = "Problème identifié", content = exp.problem)
        if (exp.rootCause.isNotBlank()) {
            DetailSection(title = "Cause racine", content = exp.rootCause)
        }
        DetailSection(title = "Solution appliquée", content = exp.solution)
        if (exp.outcome.isNotBlank()) {
            DetailSection(title = "Résultat", content = exp.outcome)
        }
        if (exp.agent.isNotBlank() || exp.project.isNotBlank()) {
            DetailSection(
                title = "Contexte",
                content = "Agent : ${exp.agent} · Projet : ${exp.project} · Statut : ${exp.validationStatus}"
            )
        }
    }
}

@Composable
private fun KnowledgeDetailView(knw: MemoryKnowledge) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = knw.title,
            style = Type.title2.semibold,
            color = Palette.text
        )

        DetailSection(
            title = "Métadonnées",
            content = "Catégorie : ${knw.category} · Version : v${knw.version} · Statut : ${knw.validationStatus}"
        )

        DetailSection(title = "Contenu de la règle", content = knw.content)
    }
}

@Composable
private fun DetailSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedRectangle(12.dp), style = GlassStyle.Card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = Type.caption.semibold,
            color = Palette.electricBlue
        )
        Text(
            text = content,
            style = Type.body,
            color = Palette.text
        )
    }
}
