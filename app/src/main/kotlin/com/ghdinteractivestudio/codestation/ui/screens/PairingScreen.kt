package com.ghdinteractivestudio.codestation.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.BotEnvironment
import com.ghdinteractivestudio.codestation.domain.model.EnvironmentStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerMode
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CodeBox
import com.ghdinteractivestudio.codestation.ui.components.IconButton
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.SegmentedChoice
import com.ghdinteractivestudio.codestation.ui.components.Spinner
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.tr
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.caption2
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.viewmodel.PairingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    onNavigateBack: () -> Unit,
    onPairingSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isConnected = state.status == ServerConnectionStatus.CONNECTED
    val isConnecting = state.status == ServerConnectionStatus.CONNECTING
    val isError = state.status == ServerConnectionStatus.ERROR

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        PageHeader(
            title = "Connexion Termux",
            pop = onNavigateBack
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header explanation
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Connecter à Termux",
                    style = Type.title2.bold,
                    color = Palette.text
                )
                Text(
                    "Vos agents s'exécutent dans l'environnement Termux / Ubuntu PRoot sur ce même téléphone.",
                    style = Type.subheadline,
                    color = Palette.secondary
                )
            }

            // Connection Mode Selector
            SegmentedChoice(
                selection = state.serverMode,
                options = listOf(
                    ServerMode.LOCAL_LOOPBACK to "Local (127.0.0.1)",
                    ServerMode.LAN_HOST to "Réseau LAN",
                    ServerMode.REMOTE_GATEWAY to "Passerelle"
                ),
                select = { viewModel.setServerMode(it) }
            )

            // Step 1: Commands to run in Termux
            CardSection(
                title = "Commandes du serveur local"
            ) {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "1. Démarrer le serveur CodeStation",
                            style = Type.footnote.semibold,
                            color = Palette.secondary
                        )
                        CommandBlockRow("/root/CodeStation/start_code_station.sh start")
                    }
                }
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "2. Générer le lien d'appairage local",
                            style = Type.footnote.semibold,
                            color = Palette.secondary
                        )
                        CommandBlockRow("/root/CodeStation/start_code_station.sh pair")
                    }
                }
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "3. Afficher le jeton d'authentification",
                            style = Type.footnote.semibold,
                            color = Palette.secondary
                        )
                        CommandBlockRow("/root/CodeStation/start_code_station.sh token")
                    }
                }
            }

            // Step 2: Server URL & Token configuration
            CardSection(
                title = "Paramètres de connexion"
            ) {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Adresse du serveur hôte", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = state.serverUrl,
                            onValueChange = { viewModel.onServerUrlChanged(it) },
                            textStyle = Type.body.monospaced.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (state.serverUrl.isEmpty()) {
                                        Text("http://127.0.0.1:19222", style = Type.body, color = Palette.tertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }

                row {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jeton d'authentification (Bearer Token)", style = Type.caption, color = Palette.secondary)
                        BasicTextField(
                            value = state.pairingToken ?: "",
                            onValueChange = { viewModel.onPairingTokenChanged(it) },
                            textStyle = Type.body.monospaced.copy(color = Palette.text),
                            cursorBrush = SolidColor(Palette.accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (state.pairingToken.isNullOrEmpty()) {
                                        Text("Jeton local (dans ~/.codestation/token)", style = Type.body, color = Palette.tertiary)
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            // Connection status and diagnostics card
            CardSection(
                title = "État du diagnostic"
            ) {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol(
                            when {
                                isConnected -> "checkmark.circle.fill"
                                isError -> "exclamationmark.circle.fill"
                                isConnecting -> "arrow.clockwise"
                                else -> "circle.dotted"
                            },
                            size = 20.dp,
                            tint = when {
                                isConnected -> Palette.added
                                isError -> Palette.danger
                                else -> Palette.secondary
                            }
                        )

                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    isConnected -> "Serveur Opérationnel"
                                    isError -> "Connexion Échouée"
                                    isConnecting -> "Vérification en cours…"
                                    else -> "Non connecté"
                                },
                                style = Type.body.semibold,
                                color = Palette.text
                            )
                            if (isConnected && state.serverVersion != null) {
                                Text(
                                    "Version ${state.serverVersion} · Latence: ${state.latencyMs ?: 4}ms",
                                    style = Type.caption,
                                    color = Palette.secondary
                                )
                            } else if (isError && state.errorMessage != null) {
                                Text(
                                    state.errorMessage ?: "Erreur",
                                    style = Type.caption,
                                    color = Palette.danger,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isConnecting) {
                            Spinner(16.dp)
                        }
                    }
                }
            }

            // Multi-Environment Control Center (Section 6)
            if (isConnected) {
                CardSection(
                    title = "Ressources du smartphone"
                ) {
                    row {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Mémoire RAM", style = Type.caption, color = Palette.secondary)
                                Text(
                                    "${state.resources.ramAvailableMb} Mo dispo / ${state.resources.ramTotalMb} Mo",
                                    style = Type.footnote.bold,
                                    color = Palette.text
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Stockage Disque", style = Type.caption, color = Palette.secondary)
                                Text(
                                    "${state.resources.diskAvailableGb} Go dispo / ${state.resources.diskTotalGb} Go",
                                    style = Type.footnote.bold,
                                    color = Palette.text
                                )
                            }
                        }
                    }
                }

                CardSection(
                    title = "Conteneurs Ubuntu PRoot (1 Bot = 1 Rootfs)"
                ) {
                    row {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Orchestrateur Multi-Bots",
                                style = Type.footnote.bold,
                                color = Palette.text
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    title = if (state.isLoadingEnvironments) "…" else "Actualiser",
                                    enabled = !state.isLoadingEnvironments,
                                    action = { viewModel.refreshEnvironments() }
                                )
                                PrimaryButton(
                                    title = "Démarrer Tout",
                                    enabled = !state.isLoadingEnvironments,
                                    action = { viewModel.startAllEnvironments() }
                                )
                            }
                        }
                    }

                    if (state.environmentMessage != null) {
                        row {
                            Text(
                                state.environmentMessage!!,
                                style = Type.caption,
                                color = Palette.emeraldAccent
                            )
                        }
                    }

                    if (state.environments.isEmpty()) {
                        row {
                            Text(
                                if (state.isLoadingEnvironments) "Chargement des conteneurs..." else "Aucun conteneur configuré.",
                                style = Type.footnote,
                                color = Palette.secondary
                            )
                        }
                    } else {
                        state.environments.forEach { env ->
                            row {
                                BotEnvironmentRow(
                                    env = env,
                                    onStart = { viewModel.startEnvironment(env.botId) },
                                    onStop = { viewModel.stopEnvironment(env.botId) }
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(
                    title = if (isConnecting) "Test en cours…" else "Tester la Connexion",
                    enabled = !isConnecting,
                    action = { viewModel.testConnection() },
                    modifier = Modifier.weight(1f)
                )

                if (isConnected) {
                    SecondaryButton(
                        title = "Accéder aux Bots",
                        action = onPairingSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun BotEnvironmentRow(
    env: BotEnvironment,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val statusColor = when (env.status) {
        EnvironmentStatus.READY -> Palette.added
        EnvironmentStatus.STOPPED -> Palette.secondary
        EnvironmentStatus.STARTING -> Palette.warning
        EnvironmentStatus.CREATING, EnvironmentStatus.INSTALLING -> Palette.accent
        EnvironmentStatus.ERROR -> Palette.danger
        EnvironmentStatus.BUSY -> Palette.warning
        EnvironmentStatus.STOPPING -> Palette.secondary
        EnvironmentStatus.NOT_INSTALLED -> Palette.tertiary
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        env.botId,
                        style = Type.body.bold,
                        color = Palette.text
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            env.status.label,
                            style = Type.caption2.semibold,
                            color = statusColor
                        )
                    }
                }
                Text(
                    "Moteur: ${env.engine}${env.model?.let { " · $it" } ?: ""} · Disque: ${env.diskMb} Mo",
                    style = Type.caption,
                    color = Palette.secondary
                )
                if (env.rootfsPath != null) {
                    Text(
                        env.rootfsPath,
                        style = Type.caption2.monospaced,
                        color = Palette.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (env.lastError != null) {
                    Text(
                        env.lastError,
                        style = Type.caption2,
                        color = Palette.danger,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (env.status == EnvironmentStatus.STOPPED || env.status == EnvironmentStatus.ERROR || env.status == EnvironmentStatus.NOT_INSTALLED) {
                    SecondaryButton(
                        title = "Démarrer",
                        action = onStart
                    )
                } else if (env.status == EnvironmentStatus.READY) {
                    SecondaryButton(
                        title = "Arrêter",
                        action = onStop
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandBlockRow(command: String) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(10.dp))
            .background(Palette.codeBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            command,
            style = Type.footnote.monospaced,
            color = Palette.text,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            title = if (isCopied) "Copié" else "Copier",
            systemImage = if (isCopied) "checkmark" else "doc.on.doc",
            action = {
                clipboard?.setPrimaryClip(ClipData.newPlainText("Command", command))
                scope.launch {
                    isCopied = true
                    delay(1500)
                    isCopied = false
                }
            }
        )
    }
}
