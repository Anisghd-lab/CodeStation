package com.ghdinteractivestudio.codestation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.ui.screens.ApprovalsScreen
import com.ghdinteractivestudio.codestation.ui.screens.BotEditorScreen
import com.ghdinteractivestudio.codestation.ui.screens.ChatScreen
import com.ghdinteractivestudio.codestation.ui.screens.ExtensionsScreen
import com.ghdinteractivestudio.codestation.ui.screens.GroupEditorScreen
import com.ghdinteractivestudio.codestation.ui.screens.MemoryScreen
import com.ghdinteractivestudio.codestation.ui.screens.OnboardingScreen
import com.ghdinteractivestudio.codestation.ui.screens.PairingScreen
import com.ghdinteractivestudio.codestation.ui.screens.RosterScreen
import com.ghdinteractivestudio.codestation.ui.screens.RoutinesScreen
import com.ghdinteractivestudio.codestation.ui.screens.SettingsScreen
import com.ghdinteractivestudio.codestation.ui.screens.TraceScreen
import com.ghdinteractivestudio.codestation.ui.screens.VoiceScreen
import com.ghdinteractivestudio.codestation.viewmodel.BotEditorViewModel
import com.ghdinteractivestudio.codestation.viewmodel.ChatViewModel
import com.ghdinteractivestudio.codestation.viewmodel.PairingViewModel
import com.ghdinteractivestudio.codestation.viewmodel.RosterViewModel
import com.ghdinteractivestudio.codestation.viewmodel.SettingsViewModel

@Composable
fun CodeStationNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Roster.route,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as CodeStationApplication
    val container = app.container

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 1. Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onConfigureServer = { navController.navigate(Screen.Pairing.route) },
                onExploreDemo = { navController.navigate(Screen.Roster.route) }
            )
        }

        // 2. Configuration & Appairage Serveur
        composable(Screen.Pairing.route) {
            val pairingViewModel: PairingViewModel = viewModel(
                factory = PairingViewModel.provideFactory(container.serverRepository)
            )
            PairingScreen(
                viewModel = pairingViewModel,
                onNavigateBack = { navController.popBackStack() },
                onPairingSuccess = {
                    navController.navigate(Screen.Roster.route) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Roster des Bots
        composable(Screen.Roster.route) {
            val rosterViewModel: RosterViewModel = viewModel(
                factory = RosterViewModel.provideFactory(container.botRepository, container.serverRepository)
            )
            RosterScreen(
                viewModel = rosterViewModel,
                onBotClick = { botId -> navController.navigate(Screen.Chat.createRoute(botId)) },
                onCreateBotClick = { navController.navigate(Screen.BotEditor.createRoute()) },
                onCreateGroupClick = { navController.navigate(Screen.GroupEditor.route) },
                onConfigureServerClick = { navController.navigate(Screen.Pairing.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        // 4. Conversation / Chat
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("botId") { type = NavType.StringType })
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId") ?: return@composable
            val chatViewModel: ChatViewModel = viewModel(
                key = botId,
                factory = ChatViewModel.provideFactory(botId, container.botRepository, container.chatRepository)
            )
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onViewTrace = { navController.navigate(Screen.Trace.createRoute(botId)) },
                onEditBot = { id -> navController.navigate(Screen.BotEditor.createRoute(id)) },
                onVoiceCall = { navController.navigate(Screen.Voice.route) }
            )
        }

        // 5. Éditeur de Bot
        composable(
            route = Screen.BotEditor.route,
            arguments = listOf(navArgument("botId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId")
            val botEditorViewModel: BotEditorViewModel = viewModel(
                key = botId ?: "new",
                factory = BotEditorViewModel.provideFactory(botId, container.botRepository, container.antigravityModelRepository)
            )
            BotEditorScreen(
                viewModel = botEditorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Salon Multi-Bots (Group Editor)
        composable(Screen.GroupEditor.route) {
            val rosterViewModel: RosterViewModel = viewModel(
                factory = RosterViewModel.provideFactory(container.botRepository, container.serverRepository)
            )
            val state by rosterViewModel.uiState.collectAsState()
            val allBots = listOfNotNull(state.leadBot) + state.pinnedBots + state.otherBots

            GroupEditorScreen(
                availableBots = allBots,
                onCreateGroup = { name, desc, memberIds ->
                    val groupBot = com.ghdinteractivestudio.codestation.domain.model.Bot(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name,
                        description = desc,
                        avatarGlyph = "group",
                        avatarColorHex = "#9D4EDD",
                        memberBotIds = memberIds
                    )
                    rosterViewModel.refresh()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. Trace d'Exécution & Raisonnement
        composable(
            route = Screen.Trace.route,
            arguments = listOf(navArgument("botId") { type = NavType.StringType })
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId") ?: return@composable
            val chatViewModel: ChatViewModel = viewModel(
                key = botId,
                factory = ChatViewModel.provideFactory(botId, container.botRepository, container.chatRepository)
            )
            val chatState by chatViewModel.uiState.collectAsState()
            TraceScreen(
                steps = chatState.currentWorkingSteps,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 8. Approbations
        composable(Screen.Approvals.route) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.provideFactory("lead", container.botRepository, container.chatRepository)
            )
            val chatState by chatViewModel.uiState.collectAsState()
            ApprovalsScreen(
                approvals = chatState.pendingApprovals,
                onRespond = { id, status, always -> chatViewModel.respondApproval(id, status, always) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 9. Paramètres
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(container.serverRepository, container.preferencesRepository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onConfigureServer = { navController.navigate(Screen.Pairing.route) },
                onOpenMemory = { navController.navigate(Screen.Memory.route) },
                onOpenRoutines = { navController.navigate(Screen.Routines.route) },
                onOpenExtensions = { navController.navigate(Screen.Extensions.route) },
                onOpenVoice = { navController.navigate(Screen.Voice.route) }
            )
        }

        // 10. Pages Complémentaires
        composable(Screen.Memory.route) {
            MemoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Routines.route) {
            RoutinesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Extensions.route) {
            ExtensionsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.Voice.route) {
            VoiceScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
