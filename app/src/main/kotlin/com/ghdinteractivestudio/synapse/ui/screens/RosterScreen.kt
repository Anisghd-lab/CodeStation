package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.ui.components.BotCard
import com.ghdinteractivestudio.synapse.ui.components.ConfirmationDialog
import com.ghdinteractivestudio.synapse.ui.components.ConnectionStatusChip
import com.ghdinteractivestudio.synapse.ui.components.EmptyStateView
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkPrimary
import com.ghdinteractivestudio.synapse.viewmodel.BotFilter
import com.ghdinteractivestudio.synapse.viewmodel.RosterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    viewModel: RosterViewModel,
    onBotClick: (String) -> Unit,
    onCreateBotClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onConfigureServerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var selectedBotForAction by remember { mutableStateOf<Bot?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Synapse", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        ConnectionStatusChip(
                            serverConfig = state.serverConfig,
                            onClick = onConfigureServerClick
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { isFabMenuExpanded = true },
                    containerColor = SynapseDarkPrimary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouveau")
                }

                DropdownMenu(
                    expanded = isFabMenuExpanded,
                    onDismissRequest = { isFabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nouvel Agent") },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            onCreateBotClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nouveau Salon Multi-Bots") },
                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                        onClick = {
                            isFabMenuExpanded = false
                            onCreateGroupClick()
                        }
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barre de Recherche
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text("Rechercher un agent, mot-clé…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Liste principale avec sections hiérarchisées
            if (state.totalCount == 0) {
                EmptyStateView(
                    title = "Aucun agent trouvé",
                    description = if (state.searchQuery.isNotBlank()) "Aucun bot ne correspond à votre recherche." else "Configurez votre premier agent pour débuter.",
                    actionLabel = "Créer un agent",
                    onActionClick = onCreateBotClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Section Lead Bot (Étoile dorée ★ en tête permanente)
                    state.leadBot?.let { lead ->
                        item {
                            SectionHeader(title = "AGENT PRINCIPAL (LEAD)")
                            Spacer(modifier = Modifier.height(4.dp))
                            BotCard(
                                bot = lead,
                                onClick = { onBotClick(lead.id) },
                                onLongClick = { selectedBotForAction = lead }
                            )
                        }
                    }

                    // 2. Section Pinned Bots
                    if (state.pinnedBots.isNotEmpty()) {
                        item {
                            SectionHeader(title = "AGENTS ÉPINGLÉS (${state.pinnedBots.size})")
                        }
                        items(state.pinnedBots, key = { it.id }) { bot ->
                            BotCard(
                                bot = bot,
                                onClick = { onBotClick(bot.id) },
                                onLongClick = { selectedBotForAction = bot }
                            )
                        }
                    }

                    // 3. Section Autres Bots
                    if (state.otherBots.isNotEmpty()) {
                        item {
                            SectionHeader(title = "TOUS LES AGENTS (${state.otherBots.size})")
                        }
                        items(state.otherBots, key = { it.id }) { bot ->
                            BotCard(
                                bot = bot,
                                onClick = { onBotClick(bot.id) },
                                onLongClick = { selectedBotForAction = bot }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Dialogue d'actions contextuelles lors d'un appui long
    selectedBotForAction?.let { bot ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { selectedBotForAction = null }
        ) {
            if (!bot.isLead) {
                DropdownMenuItem(
                    text = { Text(if (bot.isPinned) "Désépingler" else "Épingler") },
                    onClick = {
                        viewModel.togglePin(bot.id)
                        selectedBotForAction = null
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Interrompre le tour") },
                onClick = {
                    viewModel.stopBot(bot.id)
                    selectedBotForAction = null
                }
            )
            if (!bot.isLead) {
                DropdownMenuItem(
                    text = { Text("Supprimer l'agent", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }

    // Confirmation de suppression
    if (showDeleteConfirmDialog && selectedBotForAction != null) {
        val bot = selectedBotForAction!!
        ConfirmationDialog(
            title = "Supprimer ${bot.name} ?",
            message = "Cette action supprimera définitivement cet agent et l'historique de ses conversations.",
            confirmLabel = "Supprimer",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteBot(bot.id)
                selectedBotForAction = null
                showDeleteConfirmDialog = false
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                selectedBotForAction = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}
