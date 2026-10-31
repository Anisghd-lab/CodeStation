package com.ghdinteractivestudio.codestation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ghdinteractivestudio.codestation.notification.CodeStationNotificationManager
import com.ghdinteractivestudio.codestation.service.CodeStationTaskSyncService
import com.ghdinteractivestudio.codestation.ui.navigation.Screen
import com.ghdinteractivestudio.codestation.ui.navigation.CodeStationNavHost
import com.ghdinteractivestudio.codestation.ui.components.DynamicIslandBar
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var currentNavController: NavController? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CodeStationNotificationManager.createChannels(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CodeStationApplication
        val container = app.container

        // Demande prompte et standard de la permission de notification (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Initialisation des canaux de notification
        CodeStationNotificationManager.createChannels(this)

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
                    val overlayEnabled by container.preferencesRepository.dynamicIslandOverlayEnabledStream.collectAsState(initial = false)
                    val hasOverlayPermission = com.ghdinteractivestudio.codestation.util.OverlayPermissionHelper.canDrawOverlays(this@MainActivity)
                    val isSystemOverlayActive = overlayEnabled && hasOverlayPermission

                    Box(modifier = Modifier.fillMaxSize()) {
                        CodeStationNavHost(navController = navController)
                        if (!isSystemOverlayActive) {
                            DynamicIslandBar(
                                botRepository = container.botRepository,
                                approvalDao = container.approvalDao,
                                onNavigateToBot = { botId: String ->
                                    navController.navigate(Screen.Chat.createRoute(botId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Démarrage du service de synchronisation d'arrière-plan avec notification persistante
        CodeStationTaskSyncService.start(this)
        com.ghdinteractivestudio.codestation.service.DynamicIslandOverlayService.start(this)
    }

    override fun onResume() {
        super.onResume()
        val app = application as? CodeStationApplication ?: return
        val container = app.container
        CoroutineScope(Dispatchers.IO).launch {
            container.eventStreamClient.start()
            container.botRepository.refreshBots()
            container.serverRepository.testConnection()
            com.ghdinteractivestudio.codestation.widget.CodeStationWidgetManager.updateAllWidgets(this@MainActivity)
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
