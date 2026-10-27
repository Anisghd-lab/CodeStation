package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import com.ghdinteractivestudio.synapse.ui.components.ApprovalCard
import com.ghdinteractivestudio.synapse.ui.components.BotAvatar
import com.ghdinteractivestudio.synapse.ui.components.ChatMessageItem
import com.ghdinteractivestudio.synapse.ui.components.ConfirmationDialog
import com.ghdinteractivestudio.synapse.ui.components.EmptyStateView
import com.ghdinteractivestudio.synapse.ui.components.MessageComposer
import com.ghdinteractivestudio.synapse.ui.components.ThinkingOrb
import com.ghdinteractivestudio.synapse.ui.components.ThinkingTraceView
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkPrimary
import com.ghdinteractivestudio.synapse.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onViewTrace: () -> Unit,
    onEditBot: (String) -> Unit,
    onVoiceCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // Défilement automatique vers le dernier message
    LaunchedEffect(state.messages.size, state.isBotWorking) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.bot?.id?.let { onEditBot(it) } }
                    ) {
                        state.bot?.let { bot ->
                            BotAvatar(
                                name = bot.name,
                                glyph = bot.avatarGlyph,
                                colorHex = bot.avatarColorHex,
                                status = bot.status,
                                isLead = bot.isLead,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = bot.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        state.isBotWorking -> "En cours d'exécution…"
                                        bot.status == BotStatus.WORKING -> "Réflexion en cours…"
                                        else -> "${bot.provider.displayName} • ${bot.model}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.isBotWorking) SynapseDarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onViewTrace) {
                        Icon(Icons.Default.Psychology, contentDescription = "Détails de réflexion", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Paramètres de l'agent") },
                            onClick = {
                                isMenuExpanded = false
                                state.bot?.id?.let { onEditBot(it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Effacer l'historique", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                isMenuExpanded = false
                                showClearHistoryDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            MessageComposer(
                text = state.inputDraft,
                onTextChanged = viewModel::onInputChanged,
                onSend = viewModel::sendMessage,
                onVoiceClick = onVoiceCall,
                onStopClick = viewModel::stopTurn,
                isBotWorking = state.isBotWorking,
                quotedReply = state.quotedReply,
                onDismissReply = viewModel::dismissQuotedReply
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Cartes d'approbation en attente épinglées en haut du chat si existantes
            state.pendingApprovals.forEach { approval ->
                ApprovalCard(
                    approval = approval,
                    onApproveOnce = { viewModel.respondApproval(approval.id, com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus.APPROVED, false) },
                    onApproveAlways = { viewModel.respondApproval(approval.id, com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus.APPROVED, true) },
                    onReject = { viewModel.respondApproval(approval.id, com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus.REJECTED, false) },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Fil des messages
            if (state.messages.isEmpty() && !state.isBotWorking) {
                EmptyStateView(
                    title = "Démarrez la conversation",
                    description = "Posez une question, confiez une tâche de code ou lancez une routine à ${state.bot?.name ?: "cet agent"}.",
                    icon = Icons.Default.Info,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            onReply = { viewModel.onReplyTo(message) }
                        )
                    }

                    // Pilule de réflexion animée pendant qu'un tour est actif
                    if (state.isBotWorking) {
                        item {
                            ThinkingTraceView(
                                steps = state.currentWorkingSteps,
                                isWorking = true,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(10.dp)) }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = "Effacer l'historique ?",
            message = "Tous les messages échangés avec cet agent seront supprimés.",
            confirmLabel = "Effacer",
            isDestructive = true,
            onConfirm = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }
}
