package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.ServerConfig
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationDarkError
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationDarkPrimary
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationDarkSuccess

@Composable
fun ConnectionStatusChip(
    serverConfig: ServerConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (dotColor, label) = when (serverConfig.status) {
        ServerConnectionStatus.CONNECTED -> {
            val latency = serverConfig.latencyMs?.let { " (${it}ms)" } ?: ""
            CodeStationDarkSuccess to "Connecté$latency"
        }
        ServerConnectionStatus.CONNECTING -> {
            CodeStationDarkPrimary to "Connexion…"
        }
        ServerConnectionStatus.ERROR -> {
            CodeStationDarkError to "Erreur réseau"
        }
        ServerConnectionStatus.DISCONNECTED -> {
            MaterialTheme.colorScheme.onSurfaceVariant to "Hors-ligne"
        }
    }

    val chipShape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), chipShape)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), chipShape)
            .clip(chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (serverConfig.status == ServerConnectionStatus.CONNECTING) {
            ThinkingOrb(size = 8.dp, color = dotColor)
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
