package com.ghdinteractivestudio.synapse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.ghdinteractivestudio.synapse.ui.navigation.Screen
import com.ghdinteractivestudio.synapse.ui.navigation.SynapseNavHost
import com.ghdinteractivestudio.synapse.ui.theme.SynapseTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SynapseApplication
        val container = app.container

        // Gestion de l'URI d'appairage depuis l'intent si présent
        handleIncomingIntent(intent, container)

        setContent {
            val themeMode by container.preferencesRepository.themeModeStream.collectAsState(initial = "SYSTEM")
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            SynapseTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    SynapseNavHost(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val app = application as SynapseApplication
        handleIncomingIntent(intent, app.container)
    }

    private fun handleIncomingIntent(intent: Intent?, container: AppContainer) {
        val dataUri = intent?.dataString ?: return
        if (dataUri.startsWith("synapse://pair") || dataUri.startsWith("codestation://pair") || dataUri.startsWith("codync://pair")) {
            CoroutineScope(Dispatchers.IO).launch {
                container.serverRepository.parseAndApplyPairingUri(dataUri)
            }
        }
    }
}
