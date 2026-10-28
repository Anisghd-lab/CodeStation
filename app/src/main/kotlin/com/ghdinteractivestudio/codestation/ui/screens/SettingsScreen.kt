package com.ghdinteractivestudio.codestation.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.ui.components.CardForm
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CodyncSwitch
import com.ghdinteractivestudio.codestation.ui.components.LinkButton
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.plainButton
import com.ghdinteractivestudio.codestation.ui.components.tr
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.viewmodel.SettingsViewModel

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
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val state by viewModel.uiState.collectAsState()
    val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED

    var askedNotifications by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedNotifications = true
    }
    val notificationsAllowed = rememberOnResume { notificationsAllowed(context) } || (askedNotifications && notificationsAllowed(context))

    var shareAnalytics by rememberSaveable { mutableStateOf(false) }
    var appLanguage by rememberSaveable { mutableStateOf("system") }
    var confirmStartOver by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        PageHeader(
            title = "Ordinateurs et paramètres",
            pop = onNavigateBack,
            icon = "gearshape"
        )

        CardForm(modifier = Modifier.weight(1f)) {
            // Section 1: Ordinateurs
            CardSection(
                title = "Ordinateurs",
                footer = "Chaque bot dispose de son propre conteneur Ubuntu PRoot indépendant sur Termux."
            ) {
                if (isConnected) {
                    row {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Symbol("desktopcomputer", size = 20.dp, tint = Palette.added)
                            Column(Modifier.weight(1f)) {
                                Text("Termux PRoot Local", style = Type.body.semibold, color = Palette.text)
                                Text("127.0.0.1:19222 · Serveur opérationnel", style = Type.footnote, color = Palette.emeraldAccent)
                            }
                            Symbol("checkmark.circle.fill", size = 18.dp, tint = Palette.added)
                        }
                    }
                }
                row {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .plainButton(action = onConfigureServer),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol("qrcode.viewfinder", size = 20.dp, tint = Palette.text)
                        Text(tr("Associer un ordinateur"), style = Type.body, color = Palette.text)
                    }
                }
            }

            // Section 2: Préférences & Discussion (Regroupées pour éliminer le vide artificiel)
            CardSection(
                title = "Préférences générales"
            ) {
                // Thème
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Thème de l'interface", style = Type.body, color = Palette.text)
                        CompactDropdownMenu(
                            selection = state.themeMode,
                            options = listOf(
                                "system" to "Système",
                                "dark" to "Sombre",
                                "light" to "Clair"
                            ),
                            onSelect = { viewModel.setThemeMode(it) }
                        )
                    }
                }

                // Langue
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Langue de l'application", style = Type.body, color = Palette.text)
                        CompactDropdownMenu(
                            selection = appLanguage,
                            options = listOf(
                                "system" to "Suivre le système",
                                "fr" to "Français",
                                "en" to "English"
                            ),
                            onSelect = { appLanguage = it }
                        )
                    }
                }

                // Discussion : Détails de la réflexion avec interrupteur accolé au texte
                row {
                    CodyncSwitch(
                        checked = state.showThinkingDetails,
                        onChange = { viewModel.setShowThinkingDetails(it) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Détails de la réflexion", style = Type.body, color = Palette.text)
                            Text("Déplie les étapes et raisonnements dans la discussion", style = Type.caption, color = Palette.secondary)
                        }
                    }
                }
            }

            // Section 3: Notifications & Analyses
            CardSection(
                title = "Notifications & Diagnostics"
            ) {
                // Notifications locales Android (état clair sans faux push)
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Notifications Android", style = Type.body, color = Palette.text)
                            Text(
                                if (notificationsAllowed) "Live Updates et alertes d'action actives"
                                else "Autorisation système requise pour les alertes",
                                style = Type.caption,
                                color = Palette.secondary
                            )
                        }
                        SecondaryButton(
                            title = if (notificationsAllowed) "Actif" else "Activer",
                            action = {
                                if (!notificationsAllowed && Build.VERSION.SDK_INT >= 33 && !askedNotifications) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openNotificationSettings(context)
                                }
                            }
                        )
                    }
                }

                // Analyses d'utilisation
                row {
                    CodyncSwitch(
                        checked = shareAnalytics,
                        onChange = { shareAnalytics = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Partager les diagnostics anonymes", style = Type.body, color = Palette.text)
                            Text("Aide à repérer les plantages sans transmettre vos données", style = Type.caption, color = Palette.secondary)
                        }
                    }
                }
            }

            // Section 4: Informations de version et Réinitialisation (Compact et directement visible)
            CardSection(
                title = "À propos de CodeStation"
            ) {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version de l'application", style = Type.body, color = Palette.text)
                        Text("1.0.3 (Build 4)", style = Type.body.semibold, color = Palette.secondary)
                    }
                }
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dépôt GitHub", style = Type.body, color = Palette.text)
                        LinkButton("Code source", color = Palette.accent) {
                            uri.openUri("https://github.com/Anisghd-lab/CodeStation")
                        }
                    }
                }
                row {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .plainButton(action = { confirmStartOver = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol("arrow.counterclockwise", size = 18.dp, tint = Palette.danger)
                        Text("Réinitialiser les sessions locales", style = Type.body.semibold, color = Palette.danger)
                    }
                }
            }
        }
    }

    if (confirmStartOver) {
        AlertDialog(
            onDismissRequest = { confirmStartOver = false },
            title = { Text("Réinitialiser les sessions ?", style = Type.title3.bold, color = Palette.text) },
            text = {
                Text(
                    "Réinitialise les préférences locales de l'application sur cet appareil. Vos environnements et agents Ubuntu PRoot dans Termux sont entièrement préservés.",
                    style = Type.body,
                    color = Palette.secondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStartOver = false
                        viewModel.resetPreferences()
                    }
                ) {
                    Text("Réinitialiser", color = Palette.danger, style = Type.body.semibold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStartOver = false }) {
                    Text("Annuler", color = Palette.secondary)
                }
            },
            containerColor = Palette.surface
        )
    }
}

@Composable
private fun <T> CompactDropdownMenu(
    selection: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.first == selection }?.second ?: ""

    Box(modifier) {
        Row(
            Modifier
                .clip(Capsule())
                .background(Palette.bubbleUser)
                .border(0.5.dp, Palette.glassBorder, Capsule())
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentLabel,
                style = Type.body,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Symbol(
                name = "chevron.down",
                size = 12.dp,
                tint = Palette.secondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Palette.surface)
                .border(0.5.dp, Palette.glassBorder, RoundedRectangle(12.dp))
        ) {
            options.forEach { (key, label) ->
                val isSelected = key == selection
                DropdownMenuItem(
                    text = {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = label,
                                style = Type.body,
                                color = if (isSelected) Palette.emeraldAccent else Palette.text,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Symbol("checkmark", size = 14.dp, tint = Palette.emeraldAccent)
                            }
                        }
                    },
                    onClick = {
                        onSelect(key)
                        expanded = false
                    }
                )
            }
        }
    }
}
