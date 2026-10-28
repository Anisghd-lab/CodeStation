package com.ghdinteractivestudio.codestation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ghdinteractivestudio.codestation.ui.components.DynamicIslandBar
import com.ghdinteractivestudio.codestation.ui.navigation.Screen
import com.ghdinteractivestudio.codestation.ui.navigation.CodeStationNavHost
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var currentNavController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CodeStationApplication
        val container = app.container

        // Gestion de l'URI d'appairage depuis l'intent si présent
        handleIncomingIntent(intent, container)

        setContent {
            val themeMode by container.preferencesRepository.themeModeStream.collectAsState(initial = "SYSTEM")
            val isDark = when (themeMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            CodeStationTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    currentNavController = navController
                    Box(modifier = Modifier.fillMaxSize()) {
                        CodeStationNavHost(navController = navController)
                        DynamicIslandBar(
                            botRepository = container.botRepository,
                            onNavigateToBot = { botId ->
                                navController.navigate(Screen.Chat.createRoute(botId))
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val app = application as? CodeStationApplication ?: return
        val container = app.container
        CoroutineScope(Dispatchers.IO).launch {
            container.botRepository.refreshBots()
            container.serverRepository.checkHealth()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as CodeStationApplication
        handleIncomingIntent(intent, app.container)
    }

    private fun handleIncomingIntent(intent: Intent?, container: AppContainer) {
        val dataUri = intent?.dataString ?: return
        if (dataUri.startsWith("codestation://pair") || dataUri.startsWith("codestation-dev://pair") || dataUri.startsWith("synapse://pair") || dataUri.startsWith("codync://pair")) {
            CoroutineScope(Dispatchers.IO).launch {
                container.serverRepository.parseAndApplyPairingUri(dataUri)
            }
        } else if (dataUri.startsWith("codestation://chat")) {
            val botId = intent.getStringExtra("botId")
                ?: android.net.Uri.parse(dataUri).getQueryParameter("botId")
            if (botId != null) {
                currentNavController?.navigate(Screen.Chat.createRoute(botId))
            }
        }
    }
}
