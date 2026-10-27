package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.ApprovalStatus
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import com.ghdinteractivestudio.synapse.domain.model.ChatMessage
import com.ghdinteractivestudio.synapse.domain.model.QuotedReply
import com.ghdinteractivestudio.synapse.ui.components.ApprovalCardItem
import com.ghdinteractivestudio.synapse.ui.components.BotAvatar
import com.ghdinteractivestudio.synapse.ui.components.ChatMessageItem
import com.ghdinteractivestudio.synapse.ui.components.GlassStage
import com.ghdinteractivestudio.synapse.ui.components.IconButton
import com.ghdinteractivestudio.synapse.ui.components.MessageComposer
import com.ghdinteractivestudio.synapse.ui.components.ScrollEdge
import com.ghdinteractivestudio.synapse.ui.components.Edge
import com.ghdinteractivestudio.synapse.ui.components.Spinner
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.ThinkingOrb
import com.ghdinteractivestudio.synapse.ui.components.glass
import com.ghdinteractivestudio.synapse.ui.components.rememberPress
import com.ghdinteractivestudio.synapse.ui.components.pressScale
import com.ghdinteractivestudio.synapse.ui.components.tr
import com.ghdinteractivestudio.synapse.ui.theme.Capsule
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.medium
import com.ghdinteractivestudio.synapse.ui.theme.semibold
import com.ghdinteractivestudio.synapse.viewmodel.ChatViewModel

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
    var isMenuOpen by remember { mutableStateOf(false) }
    var activeQuotedReply by remember { mutableStateOf<QuotedReply?>(null) }

    val bot = state.bot
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Auto-scroll when messages arrive
    LaunchedEffect(state.messages.size, state.isBotWorking) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    GlassStage(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background),
        chrome = {
            ScrollEdge(Edge.Top, topInset + 64.dp)

            // Thread Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp)
                    .padding(horizontal = 16.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (Glass capsule)
                val (backSource, backPressed) = rememberPress()
                Box(
                    Modifier
                        .size(44.dp)
                        .glass(Capsule(), pressed = backPressed)
                        .clickable(backSource, null, role = Role.Button, onClick = onNavigateBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("chevron.left", size = 28.dp, tint = Palette.text)
                }

                // Centered Bot Identity & Host Subtitle
                Box(
                    Modifier
                        .weight(1f)
                        .clickable { bot?.id?.let { onEditBot(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (bot != null) {
                                BotAvatar(bot = bot, size = 22.dp)
                                Text(
                                    bot.name,
                                    style = Type.system(14f).semibold,
                                    color = Palette.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Symbol("wifi", size = 10.dp, tint = Palette.secondary)
                            Text(
                                "Termux 127.0.0.1",
                                style = Type.system(10f).medium,
                                color = Palette.secondary
                            )
                        }
                    }
                }

                // Right: Computer / Trace / Actions
                Box {
                    IconButton("More", "ellipsis.circle", action = { isMenuOpen = true })

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Traces d'exécution", style = Type.body) },
                            leadingIcon = { Symbol("terminal", size = 18.dp, tint = Palette.text) },
                            onClick = {
                                isMenuOpen = false
                                onViewTrace()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres du Bot", style = Type.body) },
                            leadingIcon = { Symbol("gearshape", size = 18.dp, tint = Palette.text) },
                            onClick = {
                                isMenuOpen = false
                                bot?.id?.let { onEditBot(it) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Effacer l'historique", style = Type.body, color = Palette.danger) },
                            leadingIcon = { Symbol("trash", size = 18.dp, tint = Palette.danger) },
                            onClick = {
                                isMenuOpen = false
                                viewModel.clearHistory()
                            }
                        )
                    }
                }
            }

            // Bottom Message Composer
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                MessageComposer(
                    botName = bot?.name ?: "Agent",
                    isWorking = state.isBotWorking,
                    replyTo = activeQuotedReply,
                    onCancelReply = { activeQuotedReply = null },
                    onSend = { text ->
                        viewModel.onInputChanged(text)
                        viewModel.sendMessage()
                        activeQuotedReply = null
                    },
                    onStop = { viewModel.stopTurn() },
                    onAttach = { /* Sélecteur de fichiers */ }
                )
            }
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topInset + 64.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Approval cards
            items(state.pendingApprovals, key = { it.id }) { approval ->
                ApprovalCardItem(
                    request = approval,
                    onApprove = { always ->
                        viewModel.respondApproval(approval.id, ApprovalStatus.APPROVED, always)
                    },
                    onReject = {
                        viewModel.respondApproval(approval.id, ApprovalStatus.REJECTED, false)
                    }
                )
            }

            // Chat Messages
            items(state.messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onQuoteReply = {
                        activeQuotedReply = QuotedReply(
                            messageId = it.id,
                            authorName = it.author.displayName,
                            textExcerpt = it.text.take(60)
                        )
                    }
                )
            }

            // Live reasoning thinking orb indicator
            if (state.isBotWorking) {
                item("thinking") {
                    Row(
                        Modifier.padding(start = 16.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThinkingOrb(size = 18.dp, color = Palette.secondary)
                        Text(
                            "Réflexion en cours…",
                            style = Type.caption,
                            color = Palette.secondary
                        )
                    }
                }
            }
        }
    }
}
