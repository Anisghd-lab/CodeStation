package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.data.local.ApprovalDao
import com.ghdinteractivestudio.codestation.data.local.ApprovalEntity
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.monospacedDigit
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class IslandPhase {
    READY,
    WORKING,
    STREAMING,
    COMPLETED,
    NEEDS_APPROVAL,
    ERROR,
    PAUSED,
    IDLE
}

@Composable
fun IslandPhase.tint(): Color = when (this) {
    IslandPhase.NEEDS_APPROVAL -> Palette.warning
    IslandPhase.ERROR -> Palette.danger
    IslandPhase.COMPLETED -> Palette.added
    IslandPhase.STREAMING -> Palette.electricBlue
    IslandPhase.WORKING -> Palette.emeraldAccent
    IslandPhase.PAUSED -> Palette.secondary
    IslandPhase.IDLE -> Palette.secondary
    IslandPhase.READY -> Palette.text
}

@Composable
fun IslandPhase.backgroundColor(): Color = when (this) {
    IslandPhase.NEEDS_APPROVAL -> Palette.warning
    IslandPhase.ERROR -> Palette.danger
    IslandPhase.COMPLETED -> Palette.added
    IslandPhase.STREAMING -> Color(0xFF1E2A3A)
    IslandPhase.WORKING -> Color(0xFF2C2C2C)
    IslandPhase.PAUSED -> Color(0xFF1E1E1E)
    IslandPhase.IDLE -> Color(0xFF1E1E1E)
    IslandPhase.READY -> Color(0xFF1E1E1E)
}

/**
 * Interactive Apple-inspired Dynamic Island for CodeStation.
 * - Collapsed: Minimal animated pill anchored at top center notch with live timer/status.
 * - Expanded: Smooth spring glassmorphism card with agent identity, task activity, and actions.
 */
@Composable
fun BoxScope.DynamicIslandBar(
    botRepository: BotRepository,
    approvalDao: ApprovalDao,
    onNavigateToBot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bots by botRepository.getBotsStream().collectAsState(initial = emptyList())
    val pendingApprovals by approvalDao.getAllPendingApprovalsStream().collectAsState(initial = emptyList())

    val activeApproval: ApprovalEntity? = pendingApprovals.firstOrNull()
    val activeBot: Bot? = bots.firstOrNull { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }

    var lastCompletedBotId by remember { mutableStateOf<String?>(null) }
    var lastCompletedBotName by remember { mutableStateOf<String?>(null) }
    var showCompletedBanner by remember { mutableStateOf(false) }
    var previousWorkingBotId by remember { mutableStateOf<String?>(null) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeBot?.id, activeBot?.status) {
        if (activeBot != null && (activeBot.status == BotStatus.WORKING || activeBot.status == BotStatus.BUSY)) {
            if (previousWorkingBotId != activeBot.id) {
                startedAt = System.currentTimeMillis()
                previousWorkingBotId = activeBot.id
            }
        } else if (activeBot == null && previousWorkingBotId != null) {
            val candidate = bots.firstOrNull { it.id == previousWorkingBotId }
            lastCompletedBotId = previousWorkingBotId
            lastCompletedBotName = candidate?.name ?: previousWorkingBotId
            previousWorkingBotId = null
            showCompletedBanner = true
            delay(4000)
            showCompletedBanner = false
            isExpanded = false
        }
    }

    val isVisible = activeApproval != null || activeBot != null || showCompletedBanner
    if (!isVisible) return

    val currentPhase = when {
        activeApproval != null -> IslandPhase.NEEDS_APPROVAL
        activeBot != null -> IslandPhase.WORKING
        showCompletedBanner -> IslandPhase.COMPLETED
        else -> IslandPhase.WORKING
    }

    val displayBotId = activeApproval?.botId ?: activeBot?.id ?: lastCompletedBotId ?: ""
    val displayBotName = activeApproval?.botName
        ?: activeBot?.name
        ?: lastCompletedBotName
        ?: "Agent CodeStation"

    val displayBot = bots.firstOrNull { it.id == displayBotId }

    val density = LocalDensity.current
    val insets = WindowInsets.statusBars
    val topInsetDp = with(density) { insets.getTop(density).toDp() }
    val pillHeight = 26.dp
    val pillTop = ((topInsetDp - pillHeight) / 2).coerceAtLeast(0.dp)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(Motion.fade()) + slideInVertically { -it },
        exit = fadeOut(Motion.fade()) + slideOutVertically { -it },
        modifier = modifier
            .align(Alignment.TopCenter)
            .padding(top = pillTop)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade()) },
            label = "IslandState"
        ) { expanded ->
            if (expanded) {
                // EXPANDED DYNAMIC ISLAND
                DynamicIslandExpandedCard(
                    phase = currentPhase,
                    botId = displayBotId,
                    botName = displayBotName,
                    bot = displayBot,
                    approval = activeApproval,
                    startedAt = startedAt,
                    onCollapse = { isExpanded = false },
                    onOpen = {
                        isExpanded = false
                        if (displayBotId.isNotEmpty()) onNavigateToBot(displayBotId)
                    },
                    onApprove = { approvalId ->
                        scope.launch(Dispatchers.IO) {
                            approvalDao.updateStatus(approvalId, "APPROVED", false)
                        }
                    },
                    onReject = { approvalId ->
                        scope.launch(Dispatchers.IO) {
                            approvalDao.updateStatus(approvalId, "REJECTED", false)
                        }
                    }
                )
            } else {
                // COLLAPSED DYNAMIC ISLAND (Pill)
                DynamicIslandCollapsedPill(
                    phase = currentPhase,
                    botName = displayBotName,
                    startedAt = startedAt,
                    approval = activeApproval,
                    onClick = { isExpanded = true }
                )
            }
        }
    }
}

@Composable
private fun DynamicIslandCollapsedPill(
    phase: IslandPhase,
    botName: String,
    startedAt: Long,
    approval: ApprovalEntity?,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "PulseScale"
    )

    val pillShape = Capsule()
    val pillBg = phase.backgroundColor()
    val accentColor = phase.tint()

    Row(
        modifier = Modifier
            .height(15.dp)
            .clip(pillShape)
            .background(pillBg)
            .border(
                width = 0.5.dp,
                color = if (phase == IslandPhase.NEEDS_APPROVAL || phase == IslandPhase.ERROR || phase == IslandPhase.COMPLETED) {
                    Color.Transparent
                } else {
                    accentColor.copy(alpha = 0.35f)
                },
                shape = pillShape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 5.dp, end = 7.dp, top = 1.dp, bottom = 1.dp)
            .animateContentSize(Motion.layoutSize),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (phase) {
            IslandPhase.WORKING -> {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .scale(pulseScale)
                        .clip(Capsule())
                        .background(Palette.emeraldAccent)
                )
            }
            IslandPhase.STREAMING -> {
                Box(
                    modifier = Modifier
                        .size(4.5.dp)
                        .scale(pulseScale)
                        .clip(Capsule())
                        .background(Palette.electricBlue)
                )
            }
            IslandPhase.COMPLETED -> {
                Symbol("checkmark", size = 9.dp, tint = Color.White)
            }
            IslandPhase.NEEDS_APPROVAL -> {
                Symbol("exclamationmark.triangle.fill", size = 9.dp, tint = Color.White)
            }
            IslandPhase.ERROR -> {
                Symbol("exclamationmark.triangle.fill", size = 9.dp, tint = Color.White)
            }
            IslandPhase.PAUSED -> {
                Symbol("pause.fill", size = 8.dp, tint = Color.White.copy(alpha = 0.8f))
            }
            IslandPhase.IDLE, IslandPhase.READY -> {
                Symbol("sparkles", size = 9.dp, tint = Color.White)
            }
        }

        Text(
            text = when (phase) {
                IslandPhase.WORKING, IslandPhase.STREAMING -> formatIslandElapsed(startedAt)
                IslandPhase.NEEDS_APPROVAL -> "Needs you"
                IslandPhase.COMPLETED -> "Done"
                IslandPhase.ERROR -> "Failed"
                IslandPhase.PAUSED -> "Paused"
                IslandPhase.IDLE -> "Idle"
                IslandPhase.READY -> "Ready"
            },
            style = Type.system(9f).semibold.monospacedDigit,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DynamicIslandExpandedCard(
    phase: IslandPhase,
    botId: String,
    botName: String,
    bot: Bot?,
    approval: ApprovalEntity?,
    startedAt: Long,
    onCollapse: () -> Unit,
    onOpen: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val cardShape = RoundedRectangle(22.dp)
    val bgColor = Color(0xF50E1117)
    val borderColor = when (phase) {
        IslandPhase.NEEDS_APPROVAL -> Palette.warning.copy(alpha = 0.6f)
        IslandPhase.COMPLETED -> Palette.added.copy(alpha = 0.6f)
        IslandPhase.ERROR -> Palette.danger.copy(alpha = 0.6f)
        IslandPhase.STREAMING -> Palette.electricBlue.copy(alpha = 0.6f)
        IslandPhase.WORKING -> Palette.electricBlue.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.2f)
    }

    Column(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .background(bgColor, cardShape)
            .border(0.5.dp, borderColor, cardShape)
            .clip(cardShape)
            .padding(14.dp)
            .animateContentSize(Motion.layoutSize),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // En-tête : Avatar + Titre + Backend + Bouton réduire
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                CharacterAvatar(
                    shape = bot?.avatarGlyph ?: "diamond",
                    color = if (phase == IslandPhase.NEEDS_APPROVAL) "amber" else "blue",
                    size = 28.dp
                )
                Column {
                    Text(
                        text = botName,
                        style = Type.body.bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bot?.provider?.displayName ?: "Hôte CodeStation",
                        style = Type.system(10f),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Bouton de réduction compact
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color.White.copy(alpha = 0.1f), Capsule())
                    .clip(Capsule())
                    .clickable(role = Role.Button, onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Symbol("chevron.up", size = 12.dp, tint = Color.White)
            }
        }

        // Corps d'état
        when (phase) {
            IslandPhase.WORKING, IslandPhase.STREAMING -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedRectangle(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("sparkles", size = 16.dp, tint = if (phase == IslandPhase.STREAMING) Palette.electricBlue else Palette.emeraldAccent)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (phase == IslandPhase.STREAMING) "Réception des tokens en cours..." else "Exécution en cours",
                            style = Type.caption.bold,
                            color = Color.White
                        )
                        Text(
                            text = bot?.lastMessage ?: bot?.description?.takeIf { it.isNotBlank() } ?: "Traitement des requêtes...",
                            style = Type.system(11f),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = formatIslandElapsed(startedAt),
                        style = Type.caption.bold.monospacedDigit,
                        color = if (phase == IslandPhase.STREAMING) Palette.electricBlue else Palette.emeraldAccent
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Palette.electricBlue, Capsule())
                            .clip(Capsule())
                            .clickable(role = Role.Button, onClick = onOpen)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Accéder au Chat", style = Type.caption.bold, color = Color.White)
                    }
                }
            }

            IslandPhase.NEEDS_APPROVAL -> {
                if (approval != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.warning.copy(alpha = 0.15f), RoundedRectangle(12.dp))
                            .border(0.5.dp, Palette.warning.copy(alpha = 0.4f), RoundedRectangle(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Symbol("exclamationmark.triangle.fill", size = 14.dp, tint = Palette.warning)
                            Text("Autorisation d'exécution requise", style = Type.caption.bold, color = Palette.warning)
                        }
                        Text(
                            text = approval.commandOrPath,
                            style = Type.caption2.monospaced,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Boutons Autoriser / Refuser
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Palette.danger.copy(alpha = 0.2f), Capsule())
                                .border(0.5.dp, Palette.danger.copy(alpha = 0.5f), Capsule())
                                .clip(Capsule())
                                .clickable(role = Role.Button) { onReject(approval.id) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Refuser", style = Type.caption.semibold, color = Palette.danger)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Palette.emeraldAccent, Capsule())
                                .clip(Capsule())
                                .clickable(role = Role.Button) { onApprove(approval.id) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Autoriser", style = Type.caption.bold, color = Color.White)
                        }
                    }
                }
            }

            IslandPhase.COMPLETED -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.added.copy(alpha = 0.15f), RoundedRectangle(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("checkmark.circle.fill", size = 18.dp, tint = Palette.added)
                    Column(Modifier.weight(1f)) {
                        Text("Tâche terminée", style = Type.caption.bold, color = Palette.added)
                        Text("La réponse est prête à être consultée.", style = Type.system(11f), color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.12f), Capsule())
                        .clip(Capsule())
                        .clickable(role = Role.Button, onClick = onOpen)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ouvrir", style = Type.caption.bold, color = Color.White)
                }
            }

            IslandPhase.ERROR -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.danger.copy(alpha = 0.15f), RoundedRectangle(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("xmark.circle.fill", size = 18.dp, tint = Palette.danger)
                    Text("L'exécution a échoué.", style = Type.caption.bold, color = Palette.danger)
                }
            }

            IslandPhase.PAUSED -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedRectangle(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("pause.fill", size = 16.dp, tint = Color.White.copy(alpha = 0.7f))
                    Text("Exécution suspendue", style = Type.caption.bold, color = Color.White.copy(alpha = 0.8f))
                }
            }

            IslandPhase.IDLE, IslandPhase.READY -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedRectangle(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("sparkles", size = 16.dp, tint = Color.White)
                    Text("Prêt pour de nouvelles instructions", style = Type.caption.bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun formatIslandElapsed(started: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(started) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = if (started > 0) ((now - started) / 1000).coerceAtLeast(0) else 0
    return "%d:%02d".format(elapsed / 60, elapsed % 60)
}
