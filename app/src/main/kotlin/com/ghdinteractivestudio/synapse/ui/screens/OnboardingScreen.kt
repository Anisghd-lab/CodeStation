package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.ui.components.CharacterAvatar
import com.ghdinteractivestudio.synapse.ui.components.PrimaryButton
import com.ghdinteractivestudio.synapse.ui.components.SecondaryButton
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.theme.Capsule
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold
import com.ghdinteractivestudio.synapse.ui.theme.semibold

@Composable
fun OnboardingScreen(
    onConfigureServer: () -> Unit = {},
    onExploreDemo: () -> Unit = {},
    onContinue: () -> Unit = onConfigureServer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Center Animated Avatar
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            CharacterAvatar(
                shape = "blob",
                color = "blue",
                size = 90.dp,
                status = "working"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Synapse",
            style = Type.largeTitle.bold,
            color = Palette.text
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Vos agents IA locaux dans Termux, directement sur votre téléphone Android.",
            style = Type.body,
            color = Palette.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Key Value Propositions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            FeatureRow(
                symbol = "brain",
                title = "Agents Autonomes",
                description = "Pilotez Google Antigravity, OpenAI Codex et OpenCode exécutés dans PRoot."
            )

            FeatureRow(
                symbol = "wifi",
                title = "Boucle Locale 127.0.0.1",
                description = "Communication directe ultra-rapide sans latence cloud ni proxy intermédiaire."
            )

            FeatureRow(
                symbol = "lock.fill",
                title = "Confidentialité Absolue",
                description = "Vos données, conversations et artefacts restent confinés sur votre appareil."
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrimaryButton(
                title = "Configurer Termux",
                action = onConfigureServer,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                title = "Mode Hors-ligne",
                action = onExploreDemo
            )
        }
    }
}

@Composable
private fun FeatureRow(
    symbol: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(Capsule())
                .background(Palette.surface),
            contentAlignment = Alignment.Center
        ) {
            Symbol(name = symbol, size = 20.dp, tint = Palette.text)
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = Type.body.semibold,
                color = Palette.text
            )
            Text(
                text = description,
                style = Type.subheadline,
                color = Palette.secondary
            )
        }
    }
}
