package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ghdinteractivestudio.synapse.ui.components.EmptyStateView
import com.ghdinteractivestudio.synapse.ui.components.ErrorStateView

@Composable
fun OfflineStateScreen(
    onRetry: () -> Unit,
    onConfigureServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EmptyStateView(
                title = "Connexion hôte interrompue",
                description = "Impossible de joindre le daemon hôte. Assurez-vous que le script de démarrage local tourne dans Termux ou que votre Wi-Fi est actif.",
                icon = Icons.Default.WifiOff,
                actionLabel = "Reconfigurer le serveur",
                onActionClick = onConfigureServer
            )
        }
    }
}

@Composable
fun PermissionDeniedScreen(
    permissionName: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EmptyStateView(
                title = "Autorisation nécessaire",
                description = "L'accès à l'autorisation ($permissionName) a été refusé. Veuillez l'activer dans les réglages système pour utiliser cette fonction.",
                icon = Icons.Default.MicOff,
                actionLabel = "Ouvrir les réglages",
                onActionClick = onOpenSettings
            )
        }
    }
}
