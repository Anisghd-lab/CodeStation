package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ghdinteractivestudio.synapse.ui.components.EmptyStateView
import com.ghdinteractivestudio.synapse.ui.components.PageHeader
import com.ghdinteractivestudio.synapse.ui.theme.Palette

@Composable
fun OfflineStateScreen(
    onRetry: () -> Unit,
    onConfigureServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        EmptyStateView(
            title = "Connexion hôte interrompue",
            description = "Impossible de joindre le serveur Termux (127.0.0.1:19222). Assurez-vous que CodeStation est démarré avec ./start_code_station.sh start.",
            symbolName = "wifi.slash",
            actionLabel = "Reconfigurer le serveur",
            onActionClick = onConfigureServer
        )
    }
}

@Composable
fun PermissionDeniedScreen(
    permissionName: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        EmptyStateView(
            title = "Autorisation nécessaire",
            description = "L'accès à l'autorisation ($permissionName) a été refusé. Veuillez l'activer dans les réglages système pour utiliser cette fonction.",
            symbolName = "mic.slash",
            actionLabel = "Ouvrir les réglages",
            onActionClick = onOpenSettings
        )
    }
}
