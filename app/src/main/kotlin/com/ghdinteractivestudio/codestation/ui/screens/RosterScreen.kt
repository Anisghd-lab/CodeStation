package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.codestation.ui.components.BotCard
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.Edge
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.GlassMenu
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.GlassMenuDivider
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.GlassMenuItem
import com.ghdinteractivestudio.codestation.ui.components.GlassStage
import com.ghdinteractivestudio.codestation.ui.components.GlassTabBar
import com.ghdinteractivestudio.codestation.ui.components.IconButton
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.ScrollEdge
import com.ghdinteractivestudio.codestation.ui.components.Spinner
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.TabItem
import com.ghdinteractivestudio.codestation.ui.components.glass
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.viewmodel.RosterViewModel

@Composable
fun RosterScreen(
    viewModel: RosterViewModel,
    onBotClick: (String) -> Unit,
    onCreateBotClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onConfigureServerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var isNewMenuOpen by remember { mutableStateOf(false) }
    var isComputerMenuOpen by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf("bots") }
    var botPendingDelete by remember { mutableStateOf<Bot?>(null) }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED
    val isConnecting = state.serverConfig.status == ServerConnectionStatus.CONNECTING

    GlassStage(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background),
        chrome = {
            if (selectedTab == "bots") {
                ScrollEdge(Edge.Top, topInset + 64.dp)

                // Top Header Bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 6.dp)
                        .padding(horizontal = 16.dp)
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Account Switcher Capsule (👤 ˅)
                    Row(
                        Modifier
                            .height(40.dp)
                            .glass(Capsule())
                            .clickable(role = Role.Button, onClick = onSettingsClick)
                            .padding(start = 6.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Symbol("person.crop.circle", size = 26.dp, tint = Palette.text)
                        Symbol("chevron.down", size = 14.dp, tint = Palette.secondary)
                    }

                    // Center: Computers Filter Header (Computers ˅)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Row(
                            Modifier
                                .clip(Capsule())
                                .clickable { isComputerMenuOpen = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isConnecting) {
                                Spinner(12.dp)
                            }
                            Text(
                                "Computers",
                                style = Type.system(12f).medium,
                                color = Palette.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Symbol("chevron.down", size = 13.dp, tint = Palette.secondary)
                        }

                        GlassMenu(
                            expanded = isComputerMenuOpen,
                            onDismissRequest = { isComputerMenuOpen = false },
                            width = 260.dp
                        ) {
                            GlassMenuItem(
                                title = "Termux (127.0.0.1)",
                                subtitle = if (isConnected) "Serveur opérationnel" else "Hôte déconnecté",
                                icon = if (isConnected) "checkmark.circle.fill" else "exclamationmark.circle.fill",
                                tint = if (isConnected) Palette.added else Palette.danger,
                                onClick = { isComputerMenuOpen = false }
                            )
                            GlassMenuDivider()
                            GlassMenuItem(
                                title = "Tester / Reconnecter",
                                subtitle = "Interroger l'hôte local",
                                icon = "arrow.clockwise",
                                tint = Palette.electricBlue,
                                onClick = {
                                    isComputerMenuOpen = false
                                    viewModel.refresh()
                                }
                            )
                            GlassMenuDivider()
                            GlassMenuItem(
                                title = "Gérer les ordinateurs",
                                subtitle = "Configuration IP & Conteneurs",
                                icon = "desktopcomputer",
                                tint = Palette.secondary,
                                onClick = {
                                    isComputerMenuOpen = false
                                    onConfigureServerClick()
                                }
                            )
                        }
                    }

                    // Right: New bot/group menu button (+)
                    Box {
                        Box(
                            Modifier
                                .size(40.dp)
                                .glass(Capsule())
                                .clickable(role = Role.Button) { isNewMenuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Symbol("plus", size = 22.dp, tint = Palette.text)
                        }

                        GlassMenu(
                            expanded = isNewMenuOpen,
                            onDismissRequest = { isNewMenuOpen = false },
                            width = 240.dp
                        ) {
                            GlassMenuItem(
                                title = "Nouvel Agent",
                                subtitle = "Créer un bot dédié",
                                icon = "plus.circle.fill",
                                tint = Palette.electricBlue,
                                onClick = {
                                    isNewMenuOpen = false
                                    onCreateBotClick()
                                }
                            )
                            GlassMenuDivider()
                            GlassMenuItem(
                                title = "Nouvelle Discussion",
                                subtitle = "Groupe multi-agents",
                                icon = "person.2.fill",
                                tint = Palette.electricBlue,
                                onClick = {
                                    isNewMenuOpen = false
                                    onCreateGroupClick()
                                }
                            )
                        }
                    }
                }
            }

            // Floating Glass Tab Bar at the bottom
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                GlassTabBar(
                    selection = selectedTab,
                    tabs = listOf(
                        TabItem("bots", "Bots", "bubble.left.and.bubble.right.fill", iconSize = 24.dp),
                        TabItem("state", "État", "rectangle.stack.fill", iconSize = 22.dp)
                    ),
                    select = { selectedTab = it }
                )
            }
        }
    ) {
        if (selectedTab == "state") {
            StateScreen(
                onUsageLimitsClick = { },
                bottomInset = 96.dp,
                isHostConnected = isConnected
            )
        } else {
            val allBots = buildList {
                state.leadBot?.let { add(it) }
                addAll(state.pinnedBots)
                addAll(state.otherBots)
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = topInset + 56.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isConnected && !isConnecting) {
                    item("offline_banner") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(Capsule())
                                .background(Palette.danger.copy(alpha = 0.12f))
                                .clickable { onConfigureServerClick() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Symbol("wifi.exclamationmark", size = 16.dp, tint = Palette.danger)
                                Text(
                                    "Serveur local Termux hors-ligne (127.0.0.1:19222). Touchez pour reconnecter.",
                                    style = Type.footnote,
                                    color = Palette.danger,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (allBots.isEmpty()) {
                    item("empty") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CharacterAvatar("cloud", "green", 72.dp)
                            Text("No bots yet", style = Type.title3.semibold, color = Palette.text)
                            Text(
                                "Ask one of your account's computers for access, or pair one with its code.",
                                style = Type.subheadline,
                                color = Palette.secondary,
                                textAlign = TextAlign.Center
                            )
                            PrimaryButton("Ordinateurs", action = onConfigureServerClick)
                        }
                    }
                } else {
                    items(allBots, key = { it.id }) { bot ->
                        SwipeToDismissBotItem(
                            bot = bot,
                            allBots = allBots,
                            onBotClick = { onBotClick(bot.id) },
                            onRequestDelete = { botPendingDelete = it }
                        )
                    }
                }
            }
        }

        // Boîte de dialogue de confirmation de suppression en Frosted Liquid Glass
        if (botPendingDelete != null) {
            val target = botPendingDelete!!
            GlassAlertDialog(
                onDismissRequest = { botPendingDelete = null },
                title = {
                    Text("Supprimer « ${target.name} »", style = Type.title3.semibold, color = Palette.text)
                },
                text = {
                    Text(
                        "Voulez-vous vraiment supprimer cet agent ? Ses messages et sa configuration locale seront effacés.",
                        style = Type.subheadline,
                        color = Palette.secondary
                    )
                },
                confirmButton = {
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.danger)
                            .clickable {
                                viewModel.deleteBot(target.id)
                                botPendingDelete = null
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Supprimer", style = Type.caption.bold, color = Color.White)
                    }
                },
                dismissButton = {
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.text.copy(alpha = 0.1f))
                            .clickable { botPendingDelete = null }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Annuler", style = Type.caption.semibold, color = Palette.text)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissBotItem(
    bot: Bot,
    allBots: List<Bot>,
    onBotClick: () -> Unit,
    onRequestDelete: (Bot) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete(bot)
                false // Garde la carte visible en attendant la confirmation
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ||
                dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedRectangle(16.dp))
                        .background(Palette.danger.copy(alpha = 0.85f))
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Symbol("trash.fill", size = 20.dp, tint = Color.White)
                        Text("Supprimer", style = Type.caption.bold, color = Color.White)
                    }
                }
            }
        },
        content = {
            BotCard(
                bot = bot,
                bots = allBots,
                onClick = onBotClick
            )
        }
    )
}
