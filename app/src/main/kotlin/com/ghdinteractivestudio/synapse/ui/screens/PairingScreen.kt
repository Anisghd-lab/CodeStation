package com.ghdinteractivestudio.synapse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.synapse.domain.model.ServerConnectionStatus
import com.ghdinteractivestudio.synapse.domain.model.ServerMode
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkError
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkPrimary
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkSuccess
import com.ghdinteractivestudio.synapse.viewmodel.PairingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    onNavigateBack: () -> Unit,
    onPairingSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration du Serveur", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Sélecteur de mode de serveur
            val modes = listOf(ServerMode.LOCAL_LOOPBACK, ServerMode.LAN_HOST, ServerMode.REMOTE_GATEWAY)
            val selectedTabIndex = modes.indexOf(state.serverMode).coerceAtLeast(0)

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                modes.forEachIndexed { index, mode ->
                    Tab(
                        selected = index == selectedTabIndex,
                        onClick = { viewModel.setServerMode(mode) },
                        text = {
                            Text(
                                text = when (mode) {
                                    ServerMode.LOCAL_LOOPBACK -> "Local"
                                    ServerMode.LAN_HOST -> "LAN"
                                    ServerMode.REMOTE_GATEWAY -> "Passerelle"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (index == selectedTabIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Explication du mode sélectionné
            Text(
                text = when (state.serverMode) {
                    ServerMode.LOCAL_LOOPBACK -> "Connexion directe au daemon local Termux / Ubuntu PRoot via 127.0.0.1:19222."
                    ServerMode.LAN_HOST -> "Connexion à une machine hôte de votre réseau Wi-Fi local."
                    ServerMode.REMOTE_GATEWAY -> "Connexion à un serveur d'agents sécurisé avec chiffrement TLS."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Champ URL du serveur
            OutlinedTextField(
                value = state.urlInput,
                onValueChange = viewModel::onUrlChanged,
                label = { Text("Adresse du serveur hôte") },
                placeholder = { Text("http://127.0.0.1:19222") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Champ Jeton de session / Appairage
            OutlinedTextField(
                value = state.tokenInput,
                onValueChange = viewModel::onTokenChanged,
                label = { Text("Jeton d'appairage (Optionnel)") },
                placeholder = { Text("Coller le jeton ou l'URI synapse://pair") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { /* Déclenchement scanner QR */ }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner QR")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Bouton de Test de Connexion
            Button(
                onClick = viewModel::testConnection,
                enabled = !state.isTesting && state.urlInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SynapseDarkPrimary)
            ) {
                if (state.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test en cours…", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tester la connexion", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Carte de résultat d'état
            when (state.connectionStatus) {
                ServerConnectionStatus.CONNECTED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SynapseDarkSuccess.copy(alpha = 0.15f))
                            .border(1.dp, SynapseDarkSuccess.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SynapseDarkSuccess, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Serveur connecté avec succès", color = SynapseDarkSuccess, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${state.serverVersion ?: "Hôte actif"} • Latence: ${state.latencyMs ?: 0}ms",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Button(
                        onClick = onPairingSuccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Accéder à mes agents", fontWeight = FontWeight.Bold)
                    }
                }
                ServerConnectionStatus.ERROR -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SynapseDarkError.copy(alpha = 0.15f))
                            .border(1.dp, SynapseDarkError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SynapseDarkError, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Échec de connexion", color = SynapseDarkError, fontWeight = FontWeight.Bold)
                            Text(
                                text = state.errorMessage ?: "Vérifiez que le daemon hôte est démarré sur le port spécifié.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
