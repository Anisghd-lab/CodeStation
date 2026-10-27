package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    symbolName: String = "info.circle",
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(Capsule())
                .background(Palette.surface),
            contentAlignment = Alignment.Center
        ) {
            Symbol(name = symbolName, size = 32.dp, tint = Palette.secondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = Type.title3.bold,
            color = Palette.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = Type.body,
            color = Palette.secondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(18.dp))
            PrimaryButton(
                title = actionLabel,
                action = onActionClick
            )
        }
    }
}

@Composable
fun ErrorStateView(
    title: String = "Une erreur est survenue",
    message: String,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(Capsule())
                .background(Palette.danger.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Symbol(name = "exclamationmark.triangle.fill", size = 32.dp, tint = Palette.danger)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = Type.title3.bold,
            color = Palette.text,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = Type.body,
            color = Palette.danger,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        PrimaryButton(
            title = "Réessayer",
            action = onRetry
        )
    }
}
