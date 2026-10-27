package com.ghdinteractivestudio.synapse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.synapse.domain.model.BotStatus
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkAccentGold
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkError
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkPrimary
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkSuccess

@Composable
fun BotAvatar(
    name: String,
    glyph: String,
    colorHex: String,
    status: BotStatus = BotStatus.IDLE,
    isLead: Boolean = false,
    unreadCount: Int = 0,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val baseColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        SynapseDarkPrimary
    }

    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "S"

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main avatar container
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(baseColor.copy(alpha = 0.2f))
                .border(1.5.dp, baseColor.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = baseColor,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Working / status pulsing ring if WORKING
        if (status == BotStatus.WORKING) {
            ThinkingOrb(
                size = size * 0.35f,
                color = SynapseDarkPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            )
        } else if (status == BotStatus.IDLE) {
            Box(
                modifier = Modifier
                    .size(size * 0.26f)
                    .clip(CircleShape)
                    .background(SynapseDarkSuccess)
                    .border(1.dp, Color.Black, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        // Lead Bot Gold Star badge (★) on bottom-left or bottom-end if not busy
        if (isLead) {
            Box(
                modifier = Modifier
                    .size(size * 0.38f)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1705))
                    .border(1.dp, SynapseDarkAccentGold, CircleShape)
                    .align(Alignment.BottomStart)
                    .offset(x = (-2).dp, y = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Lead Bot",
                    tint = SynapseDarkAccentGold,
                    modifier = Modifier.size(size * 0.25f)
                )
            }
        }

        // Unread Badge (Top-Right)
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SynapseDarkError)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }
    }
}
