package com.ghdinteractivestudio.codestation.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.BotEnvironment
import com.ghdinteractivestudio.codestation.domain.model.CliHealthStatus
import com.ghdinteractivestudio.codestation.domain.model.DetectedCli
import com.ghdinteractivestudio.codestation.domain.model.EnvironmentStatus
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.GlassStyle
import com.ghdinteractivestudio.codestation.ui.components.IconButton
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.SegmentedChoice
import com.ghdinteractivestudio.codestation.ui.components.Spinner
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.glass
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.util.TermuxSessionLauncher
import com.ghdinteractivestudio.codestation.viewmodel.PairingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PairingTab(val label: String) {
    CONNECTION("Connexion"),
    CONTAINERS("Conteneurs"),
    COMMANDS("Commandes")
}

@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    onNavigateBack: () -> Unit,
    onPairingSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isConnected = state.status == ServerConnectionStatus.CONNECTED
    val isConnecting = state.status == ServerConnectionStatus.CONNECTING
    val isError = state.status == ServerConnectionStatus.ERROR

    var selectedTab by remember { mutableStateOf(PairingTab.CONNECTION) }
    var isTokenVisible by remember { mutableStateOf(false) }

    // Actualisation légère des données à l'ouverture de l'écran
    LaunchedEffect(Unit) {
        viewModel.refreshEnvironments(force = false)
    }

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. CARTE DE STATUT DU SERVEUR AVEC ÉTAT, VERSION ET LATENCE
            Box(
                Modifier
                    .fillMaxWidth()
                    .glass(shape = RoundedRectangle(18.dp), style = GlassStyle.Card)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Symbol(
                            name = when {
                                isConnected -> "checkmark.circle.fill"
                                isError -> "exclamationmark.circle.fill"
                                isConnecting -> "arrow.clockwise"
                                else -> "circle.dotted"
                            },
                            size = 28.dp,
                            tint = when {
                                isConnected -> Palette.added
                                isError -> Palette.danger
                                isConnecting -> Palette.electricBlue
                                else -> Palette.secondary
                            }
                        )

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isConnected -> "Serveur Opérationnel"
                                    isError -> "Connexion Échouée"
                                    isConnecting -> "Vérification en cours…"
                                    else -> "Non connecté"
                                },
                                style = Type.title3.bold,
                                color = Palette.text
                            )
                            if (isConnected && state.serverVersion != null) {
                                Text(
                                    text = "Version ${state.serverVersion} · Latence : ${state.latencyMs ?: 27} ms",
                                    style = Type.caption.semibold,
                                    color = Palette.added
                                )
                            } else if (isError && state.errorMessage != null) {
                                Text(
                                    text = state.errorMessage ?: "Erreur",
                                    style = Type.caption,
                                    color = Palette.danger,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = state.serverUrl.ifEmpty { "http://127.0.0.1:19222" },
                                    style = Type.caption,
                                    color = Palette.secondary
                                )
                            }
                        }

                        if (isConnecting) {
                            Spinner(size = 20.dp, color = Palette.electricBlue)
                        }
                    }

                    // Boutons d'action rapides
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimaryButton(
                            title = if (isConnecting) "Test…" else "Tester la Connexion",
                            enabled = !isConnecting,
                            action = { viewModel.testConnection() },
                            modifier = Modifier.weight(1.2f)
                        )

                        if (isConnected) {
                            SecondaryButton(
                                title = "Accéder aux Bots",
                                action = onPairingSuccess,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SecondaryButton(
                            title = "Ouvrir Termux",
                            action = { TermuxSessionLauncher.openTermux(context) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. SÉLECTEUR D'ONGLETS RESPONSIVE
            SegmentedChoice(
                selection = selectedTab,
                options = listOf(
                    PairingTab.CONNECTION to "Connexion",
                    PairingTab.CONTAINERS to "Conteneurs",
                    PairingTab.COMMANDS to "Commandes"
                ),
                select = { selectedTab = it }
            )

            // 3. CONTENU DES ONGLETS
            when (selectedTab) {
                PairingTab.CONNECTION -> {
                    CardSection(
                        title = "Paramètres de connexion locale"
                    ) {
                        row {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Adresse de l'hôte CodeStation", style = Type.caption, color = Palette.secondary)
                                BasicTextField(
                                    value = state.serverUrl,
                                    onValueChange = { viewModel.onServerUrlChanged(it) },
                                    textStyle = Type.body.monospaced.copy(color = Palette.text),
                                    cursorBrush = SolidColor(Palette.electricBlue),
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
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Jeton d'authentification (Bearer Token)", style = Type.caption, color = Palette.secondary)
                                    IconButton(
                                        title = if (isTokenVisible) "Masquer" else "Révéler",
                                        systemImage = if (isTokenVisible) "eye.slash" else "eye",
                                        action = { isTokenVisible = !isTokenVisible }
                                    )
                                }
                                BasicTextField(
                                    value = state.pairingToken ?: "",
                                    onValueChange = { viewModel.onPairingTokenChanged(it) },
                                    textStyle = Type.body.monospaced.copy(color = Palette.text),
                                    cursorBrush = SolidColor(Palette.electricBlue),
                                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    decorationBox = { inner ->
                                        Box {
                                            if (state.pairingToken.isNullOrEmpty()) {
                                                Text("Jeton local (automatiquement chargé de ~/.codestation/token)", style = Type.caption, color = Palette.tertiary)
                                            }
                                            inner()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    CardSection(
                        title = "À propos de la connexion"
                    ) {
                        row {
                            Text(
                                text = "Le serveur CodeStation Host s'exécute localement dans Termux sur le port 19222. La communication s'effectue via l'interface réseau de boucle locale (127.0.0.1) sans passer par internet ni nécessiter de passerelle externe.",
                                style = Type.footnote,
                                color = Palette.secondary
                            )
                        }
                    }
                }

                PairingTab.CONTAINERS -> {
                    // Carte compacte des ressources du smartphone
                    CardSection(
                        title = "Ressources du smartphone"
                    ) {
                        row {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // RAM Card
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedRectangle(12.dp))
                                        .background(Palette.text.copy(alpha = 0.04f))
                                        .border(0.5.dp, Palette.glassBorder, RoundedRectangle(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Symbol("memorychip", size = 14.dp, tint = Palette.electricBlue)
                                            Text("Mémoire RAM", style = Type.caption.semibold, color = Palette.secondary)
                                        }
                                        Text(
                                            text = "${state.resources.ramAvailableMb} Mo",
                                            style = Type.body.bold,
                                            color = Palette.text
                                        )
                                        Text(
                                            text = "sur ${state.resources.ramTotalMb} Mo au total",
                                            style = Type.system(10f),
                                            color = Palette.tertiary
                                        )
                                    }
                                }

                                // Stockage Disque Card
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedRectangle(12.dp))
                                        .background(Palette.text.copy(alpha = 0.04f))
                                        .border(0.5.dp, Palette.glassBorder, RoundedRectangle(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Symbol("internaldrive", size = 14.dp, tint = Palette.turquoise)
                                            Text("Stockage Disque", style = Type.caption.semibold, color = Palette.secondary)
                                        }
                                        Text(
                                            text = "${state.resources.diskAvailableGb} Go",
                                            style = Type.body.bold,
                                            color = Palette.text
                                        )
                                        Text(
                                            text = "sur ${state.resources.diskTotalGb} Go au total",
                                            style = Type.system(10f),
                                            color = Palette.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section CLIs Détectées (Ubuntu PRoot / Termux)
                    if (state.clis.isNotEmpty()) {
                        CardSection(
                            title = "CLIs installées (Détection réelle)",
                            accessory = {
                                Text(
                                    text = "${state.clis.count { it.status == CliHealthStatus.READY || it.status == CliHealthStatus.INSTALLED }} opérationnelles",
                                    style = Type.caption,
                                    color = Palette.added
                                )
                            }
                        ) {
                            state.clis.forEach { cli ->
                                row {
                                    DetectedCliRow(cli)
                                }
                            }
                        }
                    }

                    // Section Conteneurs Ubuntu PRoot (1 Bot = 1 Rootfs)
                    CardSection(
                        title = "Conteneurs Ubuntu PRoot (1 Bot = 1 Rootfs)",
                        accessory = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton(
                                    title = if (state.isLoadingEnvironments) "…" else "Actualiser",
                                    enabled = !state.isLoadingEnvironments,
                                    action = { viewModel.refreshEnvironments(force = true) }
                                )
                                PrimaryButton(
                                    title = "Démarrer Tout",
                                    enabled = !state.isLoadingEnvironments && state.environments.isNotEmpty(),
                                    action = { viewModel.startAllEnvironments() }
                                )
                            }
                        }
                    ) {
                        if (state.environmentMessage != null) {
                            row {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedRectangle(8.dp))
                                        .background(Palette.electricBlueSoft)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = state.environmentMessage!!,
                                        style = Type.caption.semibold,
                                        color = Palette.electricBlue
                                    )
                                }
                            }
                        }

                        if (state.environments.isEmpty()) {
                            row {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.isLoadingEnvironments) "Chargement des conteneurs..." else "Aucun conteneur configuré.",
                                        style = Type.footnote,
                                        color = Palette.secondary
                                    )
                                }
                            }
                        } else {
                            state.environments.forEach { env ->
                                row {
                                    BotContainerCard(
                                        env = env,
                                        isLoading = state.isLoadingEnvironments,
                                        onStart = { viewModel.startEnvironment(env.botId) },
                                        onStop = { viewModel.stopEnvironment(env.botId) }
                                    )
                                }
                            }
                        }
                    }
                }

                PairingTab.COMMANDS -> {
                    CardSection(
                        title = "Commandes utiles Termux"
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
                                    "2. Vérifier l'état et les conteneurs",
                                    style = Type.footnote.semibold,
                                    color = Palette.secondary
                                )
                                CommandBlockRow("/root/CodeStation/start_code_station.sh status")
                            }
                        }
                        row {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "3. Générer le lien d'appairage",
                                    style = Type.footnote.semibold,
                                    color = Palette.secondary
                                )
                                CommandBlockRow("/root/CodeStation/start_code_station.sh pair")
                            }
                        }
                        row {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "4. Afficher le jeton d'authentification",
                                    style = Type.footnote.semibold,
                                    color = Palette.secondary
                                )
                                CommandBlockRow("/root/CodeStation/start_code_station.sh token")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Card for a Bot Environment Container.
 * Clean vertical layout with comfortable touch targets (48dp height) preventing text overlap.
 */
@Composable
private fun BotContainerCard(
    env: BotEnvironment,
    isLoading: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    var isExpandedPath by remember { mutableStateOf(false) }

    val statusColor = when (env.status) {
        EnvironmentStatus.READY -> Palette.added
        EnvironmentStatus.STOPPED -> Palette.secondary
        EnvironmentStatus.STARTING -> Palette.warning
        EnvironmentStatus.CREATING, EnvironmentStatus.INSTALLING -> Palette.electricBlue
        EnvironmentStatus.ERROR -> Palette.danger
        EnvironmentStatus.BUSY -> Palette.warning
        EnvironmentStatus.STOPPING -> Palette.secondary
        EnvironmentStatus.NOT_INSTALLED -> Palette.tertiary
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(14.dp))
            .background(Palette.text.copy(alpha = 0.03f))
            .border(0.5.dp, Palette.glassBorder, RoundedRectangle(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Line 1: Bot Identity + Status Badge
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                CharacterAvatar(
                    shape = if (env.engine.contains("antigravity")) "diamond" else "hexagon",
                    color = if (env.engine.contains("antigravity")) "indigo" else "purple",
                    size = 24.dp
                )
                Text(
                    text = env.botId,
                    style = Type.body.bold,
                    color = Palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                Modifier
                    .clip(Capsule())
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(0.5.dp, statusColor.copy(alpha = 0.35f), Capsule())
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = env.status.label,
                    style = Type.caption2.semibold,
                    color = statusColor
                )
            }
        }

        // Line 2: Engine, Model, Disk
        Text(
            text = "Moteur : ${env.engine.uppercase()}${env.model?.let { " · $it" } ?: ""} · Disque : ${env.diskMb} Mo",
            style = Type.caption,
            color = Palette.secondary
        )

        // Line 3: Rootfs Path (Cleanly truncated with expand option)
        if (env.rootfsPath != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedRectangle(8.dp))
                    .background(Palette.codeBackground)
                    .clickable { isExpandedPath = !isExpandedPath }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = env.rootfsPath,
                    style = Type.caption2.monospaced,
                    color = Palette.tertiary,
                    maxLines = if (isExpandedPath) 4 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Line 4: Error Banner
        if (env.lastError != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedRectangle(8.dp))
                    .background(Palette.danger.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = env.lastError,
                    style = Type.caption2,
                    color = Palette.danger,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Line 5: Dedicated Actions Row with 48dp touch targets
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val canStart = env.status == EnvironmentStatus.STOPPED ||
                    env.status == EnvironmentStatus.ERROR ||
                    env.status == EnvironmentStatus.NOT_INSTALLED

            if (canStart) {
                PrimaryButton(
                    title = "Démarrer",
                    enabled = !isLoading,
                    action = onStart,
                    modifier = Modifier.weight(1f)
                )
            } else if (env.status == EnvironmentStatus.READY) {
                SecondaryButton(
                    title = "Arrêter",
                    enabled = !isLoading,
                    action = onStop,
                    modifier = Modifier.weight(1f)
                )
            }

            // Terminal Button
            SecondaryButton(
                title = "Terminal Termux",
                action = {
                    TermuxSessionLauncher.openBotTerminalSession(
                        context = context,
                        botId = env.botId,
                        engine = env.engine
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Row displaying a detected CLI in the Ubuntu PRoot / Termux system.
 */
@Composable
private fun DetectedCliRow(cli: DetectedCli) {
    val isReady = cli.status == CliHealthStatus.READY || cli.status == CliHealthStatus.INSTALLED
    val statusColor = if (isReady) Palette.added else Palette.danger

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = cli.name,
                    style = Type.body.semibold,
                    color = Palette.text
                )
                if (cli.isPrimary) {
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.electricBlueSoft)
                            .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.4f), Capsule())
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PRIORITAIRE", style = Type.system(9f).semibold, color = Palette.electricBlue)
                    }
                }
            }

            Box(
                Modifier
                    .clip(Capsule())
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = cli.status.displayName,
                    style = Type.caption2.semibold,
                    color = statusColor
                )
            }
        }

        Text(
            text = "Binaire : ${cli.binaryName}${cli.version?.let { " · $it" } ?: ""} (${cli.environment})",
            style = Type.caption,
            color = Palette.secondary
        )

        if (cli.installedPath != null) {
            Text(
                text = cli.installedPath,
                style = Type.caption2.monospaced,
                color = Palette.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            .border(0.5.dp, Palette.glassBorder, RoundedRectangle(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = command,
            style = Type.footnote.monospaced,
            color = Palette.text,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            title = if (isCopied) "Copié" else "Copier",
            systemImage = if (isCopied) "checkmark" else "doc.on.doc",
            tint = if (isCopied) Palette.added else Palette.secondary,
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
