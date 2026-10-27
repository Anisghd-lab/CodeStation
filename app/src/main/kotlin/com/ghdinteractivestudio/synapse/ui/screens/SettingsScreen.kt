package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.synapse.ui.components.SettingsRow
import com.ghdinteractivestudio.synapse.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onConfigureServer: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenRemoteScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres & Préférences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Section 1 : Apparence
            SettingsSectionHeader(title = "APPARENCE")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "Thème Sombre Obsidian",
                        subtitle = "Palette optimisée OLED pour le terminal et les chats",
                        icon = Icons.Default.Palette,
                        checked = state.themeMode == "DARK",
                        onCheckedChange = { isDark ->
                            viewModel.setThemeMode(if (isDark) "DARK" else "LIGHT")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2 : Expérience de Chat
            SettingsSectionHeader(title = "CHAT & ORCHESTRATION")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "Détails de réflexion (Thinking)",
                        subtitle = "Afficher les étapes et outils dans le fil",
                        icon = Icons.Default.Psychology,
                        checked = state.showThinkingDetails,
                        onCheckedChange = viewModel::setShowThinkingDetails
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsRow(
                        title = "Retour Haptique",
                        subtitle = "Vibrations légères lors de l'envoi et des approbations",
                        checked = state.hapticFeedback,
                        onCheckedChange = viewModel::setHapticFeedback
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3 : Modules Complémentaires
            SettingsSectionHeader(title = "MODULES COMPLÉMENTAIRES")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "Mémoire des Agents",
                        subtitle = "Consulter les faits mémorisés (MEMORY.md)",
                        icon = Icons.Default.Psychology,
                        onClick = onOpenMemory
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsRow(
                        title = "Routines & Automatisation",
                        subtitle = "Tâches planifiées et déclencheurs",
                        icon = Icons.Default.Schedule,
                        onClick = onOpenRoutines
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsRow(
                        title = "Extensions & Outils MCP",
                        subtitle = "Catalogue des intégrations",
                        icon = Icons.Default.Extension,
                        onClick = onOpenExtensions
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsRow(
                        title = "Appel Vocal Mains-Libres",
                        subtitle = "Contrôle vocal et synthèse",
                        icon = Icons.Default.Mic,
                        onClick = onOpenVoice
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    SettingsRow(
                        title = "Écran Distant (WebRTC)",
                        subtitle = "Visualisation et télécommande",
                        icon = Icons.Default.Cast,
                        onClick = onOpenRemoteScreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4 : Serveur & Diagnostic
            SettingsSectionHeader(title = "SERVEUR & DIAGNOSTIC")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "Hôte : ${state.serverConfig.url}",
                        subtitle = "${state.serverConfig.mode.displayName} • ${state.serverConfig.status.name}",
                        icon = Icons.Default.Dns,
                        onClick = onConfigureServer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 5 : À propos
            SettingsSectionHeader(title = "À PROPOS")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    SettingsRow(
                        title = "Synapse AI Studio",
                        subtitle = "Version ${state.appVersion} • GHD Interactive Studio",
                        icon = Icons.Default.Info,
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}
