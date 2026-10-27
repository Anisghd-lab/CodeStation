package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.domain.model.Bot
import com.ghdinteractivestudio.synapse.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.synapse.ui.components.BotCard
import com.ghdinteractivestudio.synapse.ui.components.GlassStage
import com.ghdinteractivestudio.synapse.ui.components.GlassTabBar
import com.ghdinteractivestudio.synapse.ui.components.IconButton
import com.ghdinteractivestudio.synapse.ui.components.ScrollEdge
import com.ghdinteractivestudio.synapse.ui.components.Edge
import com.ghdinteractivestudio.synapse.ui.components.Spinner
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.TabItem
import com.ghdinteractivestudio.synapse.ui.components.rememberPress
import com.ghdinteractivestudio.synapse.ui.components.pressScale
import com.ghdinteractivestudio.synapse.ui.components.glass
import com.ghdinteractivestudio.synapse.ui.components.PrimaryButton
import com.ghdinteractivestudio.synapse.ui.components.SecondaryButton
import com.ghdinteractivestudio.synapse.ui.theme.Capsule
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold
import com.ghdinteractivestudio.synapse.ui.theme.medium
import com.ghdinteractivestudio.synapse.ui.theme.semibold
import com.ghdinteractivestudio.synapse.viewmodel.RosterViewModel

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
    var selectedTab by remember { mutableStateOf("bots") }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isConnected = state.serverConfig.status == ServerConnectionStatus.CONNECTED
    val isConnecting = state.serverConfig.status == ServerConnectionStatus.CONNECTING

    GlassStage(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background),
        chrome = {
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
                // Left: Synapse Identity / Account Button
                Row(
                    Modifier
                        .clip(Capsule())
                        .clickable { onConfigureServerClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Symbol("sparkles", size = 18.dp, tint = Palette.accent)
                    Text("Synapse", style = Type.body.semibold, color = Palette.text)
                }

                // Center: Termux Host connection badge
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.surface)
                            .clickable { onConfigureServerClick() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isConnecting) {
                            Spinner(10.dp)
                        } else {
                            Symbol(
                                if (isConnected) "wifi" else "wifi.slash",
                                size = 12.dp,
                                tint = if (isConnected) Palette.added else Palette.danger
                            )
                        }
                        Text(
                            if (isConnected) "Termux (127.0.0.1)" else "Termux Déconnecté",
                            style = Type.caption.medium,
                            color = Palette.secondary
                        )
                    }
                }

                // Right: New bot/group menu button (+)
                Box {
                    IconButton("New", "plus", action = { isNewMenuOpen = true })

                    DropdownMenu(
                        expanded = isNewMenuOpen,
                        onDismissRequest = { isNewMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nouvel Agent", style = Type.body) },
                            leadingIcon = { Symbol("person.fill", size = 18.dp, tint = Palette.text) },
                            onClick = {
                                isNewMenuOpen = false
                                onCreateBotClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nouveau Groupe", style = Type.body) },
                            leadingIcon = { Symbol("person.2", size = 18.dp, tint = Palette.text) },
                            onClick = {
                                isNewMenuOpen = false
                                onCreateGroupClick()
                            }
                        )
                    }
                }
            }

            // Floating Glass Tab Bar at the bottom
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                GlassTabBar(
                    selection = selectedTab,
                    tabs = listOf(
                        TabItem("bots", "Agents", "bubble.left.and.bubble.right.fill", iconSize = 22.dp),
                        TabItem("settings", "Paramètres", "gearshape", iconSize = 22.dp)
                    ),
                    select = {
                        selectedTab = it
                        if (it == "settings") onSettingsClick()
                    }
                )
            }
        }
    ) {
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Symbol("bubble.left.and.bubble.right", size = 48.dp, tint = Palette.tertiary)
                        Text(
                            "Aucun agent actif",
                            style = Type.title3.bold,
                            color = Palette.text
                        )
                        Text(
                            "Vos agents IA s'exécutent dans Termux / Ubuntu PRoot.\nCréez un agent ou vérifiez la connexion locale.",
                            style = Type.subheadline,
                            color = Palette.secondary,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton("Créer un Agent", action = onCreateBotClick)
                            SecondaryButton("Connexion", action = onConfigureServerClick)
                        }
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
