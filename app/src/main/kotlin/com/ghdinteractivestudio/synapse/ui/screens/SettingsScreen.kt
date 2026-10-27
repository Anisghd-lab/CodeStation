package com.ghdinteractivestudio.synapse.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.synapse.ui.components.CardForm
import com.ghdinteractivestudio.synapse.ui.components.CardSection
import com.ghdinteractivestudio.synapse.ui.components.ChoicePicker
import com.ghdinteractivestudio.synapse.ui.components.CodyncSwitch
import com.ghdinteractivestudio.synapse.ui.components.IconButton
import com.ghdinteractivestudio.synapse.ui.components.LinkButton
import com.ghdinteractivestudio.synapse.ui.components.PageHeader
import com.ghdinteractivestudio.synapse.ui.components.SecondaryButton
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.plainButton
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
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    val state by viewModel.uiState.collectAsState()
    val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED

    var askedNotifications by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedNotifications = true
    }
    val notificationsAllowed = rememberOnResume { notificationsAllowed(context) } || (askedNotifications && notificationsAllowed(context))

    var showThinkingDetails by rememberSaveable { mutableStateOf(true) }
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
            // Card 1: Ordinateurs
            CardSection(
                title = "Ordinateurs",
                footer = "Chaque bot fonctionne sur son propre ordinateur. Connectez-vous pour voir les ordinateurs de votre compte."
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
                                Text("Termux PRoot", style = Type.body.semibold, color = Palette.text)
                                Text("127.0.0.1:19222 · En ligne", style = Type.footnote, color = Palette.secondary)
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

            // Card 2: Apparence
            CardSection(
                title = "Apparence",
                footer = "Modifie uniquement l’apparence sur cet appareil."
            ) {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Thème", style = Type.body, color = Palette.text)
                        ChoicePicker(
                            selection = state.themeMode,
                            options = listOf(
                                "system" to "Système",
                                "dark" to "Sombre",
                                "light" to "Clair"
                            ),
                            select = { viewModel.setThemeMode(it) },
                            modifier = Modifier.size(width = 200.dp, height = 36.dp)
                        )
                    }
                }
            }

            // Card 3: Langue
            CardSection(
                title = "Langue",
                footer = "Choisissez la langue de cet appareil. Les langues système non prises en charge utilisent l’anglais."
            ) {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Langue de l'app", style = Type.body, color = Palette.text)
                        ChoicePicker(
                            selection = appLanguage,
                            options = listOf(
                                "system" to "Suivre le système",
                                "fr" to "Français",
                                "en" to "English"
                            ),
                            select = { appLanguage = it },
                            modifier = Modifier.size(width = 200.dp, height = 36.dp)
                        )
                    }
                }
            }

            // Card 4: Discussion
            CardSection(
                title = "Discussion",
                footer = "Permet de déplier la réflexion et les étapes d’un bot au travail depuis son statut dans la discussion. Ne change que cet appareil."
            ) {
                row {
                    CodyncSwitch(
                        checked = state.showThinkingDetails,
                        onChange = { viewModel.setShowThinkingDetails(it) }
                    ) {
                        Text(
                            "Afficher les détails de la réflexion",
                            style = Type.body,
                            color = Palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Card 5: Notifications
            CardSection(
                title = "Notifications",
                footer = "Get a result summary, a request for input, or a failure notice. Notification previews follow your Android settings."
            ) {
                row {
                    Text(
                        "Push isn't set up in this build. Bots and chats still update while Codync is open.",
                        style = Type.subheadline,
                        color = Palette.secondary
                    )
                }
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notifications", style = Type.body, color = Palette.text)
                        SecondaryButton(
                            title = if (notificationsAllowed) "Activé" else "Activer",
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
            }

            // Card 6: Analytics
            CardSection(
                footer = "Les fonctionnalités que vous utilisez, jamais vos messages, votre code ni vos fichiers. Cela nous aide à repérer les bugs et à améliorer Codync."
            ) {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Partager des analyses d'utilisation", style = Type.body, color = Palette.text, modifier = Modifier.weight(1f))
                        CodyncSwitch(checked = shareAnalytics, onChange = { shareAnalytics = it }) { }
                    }
                }
            }

            // Card 7: Informations
            CardSection {
                row {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version de l'application", style = Type.body, color = Palette.text)
                        Text("2.28.0", style = Type.body, color = Palette.secondary)
                    }
                }
                row {
                    LinkButton("Code source", color = Palette.accent) {
                        uri.openUri("https://github.com/Anisghd-lab/CodeStation")
                    }
                }
            }

            // Card 8: Start over / Recommencer
            CardSection {
                row {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .plainButton(action = { confirmStartOver = true }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Symbol("arrow.counterclockwise", size = 20.dp, tint = Palette.danger)
                        Text("Recommencer", style = Type.body, color = Palette.danger)
                    }
                }
            }
        }
    }

    if (confirmStartOver) {
        AlertDialog(
            onDismissRequest = { confirmStartOver = false },
            title = { Text("Recommencer ?", style = Type.title3.bold, color = Palette.text) },
            text = {
                Text(
                    "Réinitialise les paramètres locaux et déconnecte les sessions sur cet appareil. Vos ordinateurs et agents Termux conservent leurs données.",
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
                    Text("Recommencer", color = Palette.danger, style = Type.body.semibold)
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
