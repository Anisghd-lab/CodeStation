package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.synapse.ui.components.CardForm
import com.ghdinteractivestudio.synapse.ui.components.CardSection
import com.ghdinteractivestudio.synapse.ui.components.PageHeader
import com.ghdinteractivestudio.synapse.ui.components.SegmentedChoice
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.tr
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold
import com.ghdinteractivestudio.synapse.ui.theme.semibold
import com.ghdinteractivestudio.synapse.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onConfigureServer: () -> Unit = {},
    onConfigureServerClick: () -> Unit = onConfigureServer,
    onOpenMemory: () -> Unit = {},
    onOpenRoutines: () -> Unit = {},
    onOpenExtensions: () -> Unit = {},
    onOpenVoice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        PageHeader(
            title = "Paramètres",
            pop = onNavigateBack
        )

        CardForm(modifier = Modifier.weight(1f)) {
            // Appearance Section
            CardSection(title = "Apparence") {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Thème de l'application", style = Type.caption, color = Palette.secondary)
                        SegmentedChoice(
                            selection = state.themeMode,
                            options = listOf(
                                "system" to "Système",
                                "dark" to "Sombre",
                                "light" to "Clair"
                            ),
                            select = { viewModel.setThemeMode(it) }
                        )
                    }
                }
            }

            // Connection Section
            CardSection(
                title = "Connexion Locale Termux",
                footer = "Synapse communique via l'interface loopback 127.0.0.1 avec le serveur d'agents exécuté dans Ubuntu PRoot."
            ) {
                row {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onConfigureServer() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol(
                            if (isConnected) "wifi" else "wifi.slash",
                            size = 18.dp,
                            tint = if (isConnected) Palette.added else Palette.danger
                        )
                        Column(Modifier.weight(1f)) {
                            Text("Hôte Termux", style = Type.body.semibold, color = Palette.text)
                            Text(state.serverConfig.url, style = Type.caption, color = Palette.secondary)
                        }
                        Symbol("chevron.right", size = 14.dp, tint = Palette.tertiary)
                    }
                }
            }

            // Advanced Agent Features
            CardSection(title = "Écosystème des Agents") {
                row {
                    SettingsNavigationRow("Mémoire Partagée", "brain", onOpenMemory)
                }
                row {
                    SettingsNavigationRow("Routines & Tâches de fond", "clock", onOpenRoutines)
                }
                row {
                    SettingsNavigationRow("Connecteurs & Extensions MCP", "puzzlepiece", onOpenExtensions)
                }
                row {
                    SettingsNavigationRow("Moteur Vocal", "waveform", onOpenVoice)
                }
            }

            // Independent Identity & Legal Notices
            CardSection(
                title = "À Propos de Synapse",
                footer = "Synapse est une application Android native indépendante développée par GHD Interactive Studio.\nComporte du code sous licence Apache-2.0 développé par Po Kai Lee et les contributeurs de Codync."
            ) {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Version", style = Type.body, color = Palette.text)
                        Spacer(Modifier.weight(1f))
                        Text("1.0.0 (Build 1)", style = Type.body, color = Palette.secondary)
                    }
                }

                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Package ID", style = Type.body, color = Palette.text)
                        Spacer(Modifier.weight(1f))
                        Text("com.ghdinteractivestudio.synapse", style = Type.caption, color = Palette.secondary)
                    }
                }

                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Architecture", style = Type.body, color = Palette.text)
                        Spacer(Modifier.weight(1f))
                        Text("Kotlin 2.0 · Jetpack Compose · Room", style = Type.caption, color = Palette.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    systemImage: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Symbol(systemImage, size = 18.dp, tint = Palette.secondary)
        Text(tr(title), style = Type.body, color = Palette.text, modifier = Modifier.weight(1f))
        Symbol("chevron.right", size = 14.dp, tint = Palette.tertiary)
    }
}
