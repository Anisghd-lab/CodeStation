package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.repository.BotRepository
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.monospacedDigit
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.delay

@Composable
fun BoxScope.DynamicIslandBar(
    botRepository: BotRepository,
    onNavigateToBot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bots by botRepository.getBotsStream().collectAsState(initial = emptyList())
    val activeBot = bots.firstOrNull { it.status == BotStatus.WORKING }
        ?: bots.firstOrNull { it.status == BotStatus.APPROVAL_REQUESTED }

    var lastCompletedBotId by remember { mutableStateOf<String?>(null) }
    var showCompletedBanner by remember { mutableStateOf(false) }
    var previousWorkingBotId by remember { mutableStateOf<String?>(null) }
    var startedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activeBot?.id, activeBot?.status) {
        if (activeBot != null && activeBot.status == BotStatus.WORKING) {
            if (previousWorkingBotId != activeBot.id) {
                startedAt = System.currentTimeMillis()
                previousWorkingBotId = activeBot.id
            }
        } else if (activeBot == null && previousWorkingBotId != null) {
            // Un bot vient de terminer sa tâche
            lastCompletedBotId = previousWorkingBotId
            previousWorkingBotId = null
            showCompletedBanner = true
            delay(3500)
            showCompletedBanner = false
        }
    }

    val isVisible = activeBot != null || showCompletedBanner
    val displayBot = activeBot ?: bots.firstOrNull { it.id == lastCompletedBotId }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(Motion.fade()) + slideInVertically { -it },
        exit = fadeOut(Motion.fade()) + slideOutVertically { -it },
        modifier = modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 4.dp)
    ) {
        val isWorking = activeBot?.status == BotStatus.WORKING
        val isNeedsApproval = activeBot?.status == BotStatus.APPROVAL_REQUESTED
        val isCompleted = showCompletedBanner && !isWorking && !isNeedsApproval

        val capsuleBackground = when {
            isNeedsApproval -> Palette.accent
            isCompleted -> Palette.added
            else -> Color(0xFF1E1E1E)
        }

        Row(
            modifier = Modifier
                .clip(Capsule())
                .background(capsuleBackground)
                .clickable(
                    role = Role.Button,
                    onClick = {
                        displayBot?.id?.let { onNavigateToBot(it) }
                    }
                )
                .padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
                .animateContentSize(Motion.layoutSize),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val iconName = when {
                isNeedsApproval -> "hand.raised.fill"
                isCompleted -> "checkmark"
                else -> "sparkles"
            }
            Symbol(iconName, size = 13.dp, tint = Color.White)

            val statusText = when {
                isNeedsApproval -> "Needs you"
                isCompleted -> "Done"
                isWorking -> formatIslandElapsed(startedAt)
                else -> "Actif"
            }

            Text(
                text = statusText,
                style = Type.system(12f).semibold.monospacedDigit,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (displayBot != null) {
                Text(
                    text = "·",
                    style = Type.system(12f),
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = displayBot.name,
                    style = Type.system(12f).semibold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun formatIslandElapsed(started: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(started) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = if (started > 0) (now - started) / 1000 else 0
    return "%d:%02d".format(elapsed / 60, elapsed % 60)
}
