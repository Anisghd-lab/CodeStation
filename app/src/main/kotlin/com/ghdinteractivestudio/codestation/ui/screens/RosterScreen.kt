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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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

                        DropdownMenu(
                            expanded = isComputerMenuOpen,
                            onDismissRequest = { isComputerMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isConnected) "Termux (127.0.0.1) · En ligne" else "Termux (127.0.0.1) · Déconnecté",
                                        style = Type.body
                                    )
                                },
                                leadingIcon = {
                                    Symbol(
                                        if (isConnected) "checkmark.circle.fill" else "exclamationmark.circle.fill",
                                        size = 18.dp,
                                        tint = if (isConnected) Palette.added else Palette.danger
                                    )
                                },
                                onClick = { isComputerMenuOpen = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Reconnecter", style = Type.body) },
                                leadingIcon = { Symbol("arrow.clockwise", size = 18.dp, tint = Palette.text) },
                                onClick = {
                                    isComputerMenuOpen = false
                                    viewModel.refresh()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gérer les ordinateurs", style = Type.body) },
                                leadingIcon = { Symbol("desktopcomputer", size = 18.dp, tint = Palette.text) },
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

                        DropdownMenu(
                            expanded = isNewMenuOpen,
                            onDismissRequest = { isNewMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New bot", style = Type.body) },
                                leadingIcon = { Symbol("plus", size = 18.dp, tint = Palette.text) },
                                onClick = {
                                    isNewMenuOpen = false
                                    onCreateBotClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New group chat", style = Type.body) },
                                leadingIcon = { Symbol("person.2", size = 18.dp, tint = Palette.text) },
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
                        BotCard(
                            bot = bot,
                            bots = allBots,
                            onClick = { onBotClick(bot.id) }
                        )
                    }
                }
            }
        }
    }
}
