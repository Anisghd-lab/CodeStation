package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.ui.theme.Palette

@Composable
fun BotAvatar(
    bot: Bot,
    bots: List<Bot> = emptyList(),
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    if (bot.isGroup) {
        val members = bots.filter { bot.memberBotIds.contains(it.id) }
        val faces = members.map {
            AvatarFace(it.avatarGlyph, it.avatarColorHex, it.status.name.lowercase())
        }
        GroupAvatar(faces, size)
    } else {
        CharacterAvatar(
            shape = bot.avatarGlyph,
            color = bot.avatarColorHex,
            size = size,
            status = bot.status.name.lowercase(),
            modifier = modifier
        )
    }
}

/**
 * The roster's avatar with its status dot: amber exclamation mark needs you,
 * accent dot is unread, and star badge is lead agent.
 */
@Composable
fun AvatarWithStatus(
    bot: Bot,
    bots: List<Bot> = emptyList(),
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(size)) {
        BotAvatar(bot, bots, size)
        val isNeedsInput = bot.status == BotStatus.BUSY || bot.pendingApprovalsCount > 0
        if (isNeedsInput) {
            Box(
                Modifier
                    .size(size * 0.36f)
                    .align(if (bot.isLead) Alignment.TopEnd else Alignment.BottomEnd)
                    .avatarRing()
                    .background(Palette.warning, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Symbol("exclamationmark", size = size * 0.24f, tint = Color.White)
            }
        } else if (bot.unreadCount > 0) {
            Box(
                Modifier
                    .size(size * 0.28f)
                    .align(if (bot.isLead) Alignment.TopEnd else Alignment.BottomEnd)
                    .avatarRing()
                    .background(Palette.accentFill, CircleShape)
            )
        }
        if (bot.isLead) {
            Box(
                Modifier
                    .size(size * 0.34f)
                    .align(Alignment.BottomEnd)
                    .avatarRing()
                    .background(Palette.accentFill, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Symbol("star.fill", size = size * 0.20f, tint = Palette.onAccent)
            }
        }
    }
}

private fun Modifier.avatarRing(): Modifier {
    return drawWithContent {
        drawContent()
        drawCircle(Color.Transparent, size.minDimension / 2, style = Stroke(2.dp.toPx()))
    }
}
