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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.synapse.domain.model.ExtensionItem
import com.ghdinteractivestudio.synapse.domain.model.MemoryFact
import com.ghdinteractivestudio.synapse.domain.model.Routine
import com.ghdinteractivestudio.synapse.ui.components.EmptyStateView
import com.ghdinteractivestudio.synapse.ui.components.ThinkingOrb
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkPrimary
import com.ghdinteractivestudio.synapse.ui.theme.SynapseDarkSuccess

// 1. Écran de Mémoire des Agents
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    facts: List<MemoryFact> = listOf(
        MemoryFact("1", "lead", "L'utilisateur développe sous Android avec Termux et PRoot.", "Environnement"),
        MemoryFact("2", "lead", "Le préfixe obligatoire d'application est com.ghdinteractivestudio.", "Architecture"),
        MemoryFact("3", "lead", "Le projet Synapse est 100% Kotlin et Jetpack Compose sans Flutter.", "Spécifications")
    ),
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mémoire des Agents (MEMORY.md)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Les faits ci-dessous sont injectés de manière pérenne dans le contexte des agents pour assurer la cohérence entre sessions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(facts) { fact ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = fact.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (fact.isShared) "Partagé" else "Privé",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = fact.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// 2. Écran des Routines
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onNavigateBack: () -> Unit
) {
    var routines by remember {
        mutableStateOf(
            listOf(
                Routine("1", "lead", "Check-in Matinal", "Vérifie l'état des services et synthétise les commits.", "DAILY", "0 9 * * *", true, "Aujourd'hui à 09:00", lastResultSummary = "Succès"),
                Routine("2", "codex", "Nettoyage & Diagnostic Git", "Exécute git status et vérifie les branches locales.", "WEEKDAYS", "0 18 * * 1-5", true, "Demain à 18:00", lastResultSummary = "Succès")
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routines Automatisées", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(routines) { routine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(routine.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Switch(
                                checked = routine.isEnabled,
                                onCheckedChange = { checked ->
                                    routines = routines.map { if (it.id == routine.id) it.copy(isEnabled = checked) else it }
                                }
                            )
                        }
                        Text(routine.instruction, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Prochaine exécution : ${routine.nextRunDescription}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            OutlinedButton(
                                onClick = { /* Déclenchement test */ },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tester", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Écran du Catalogue d'Extensions & MCP
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    onNavigateBack: () -> Unit
) {
    val extensions = listOf(
        ExtensionItem("mcp-chat", "MCP Chat Core", "Communication bidirectionnelle et transmission de fichiers", "MCP", isInstalled = true, isConfigured = true),
        ExtensionItem("mcp-git", "MCP Git Tools", "Inspection de dépôts, commits, diffs et statuts", "MCP", isInstalled = true, isConfigured = true),
        ExtensionItem("mcp-bash", "MCP Command Runner", "Exécution de commandes shell sous sandbox sécurisé", "SHELL", isInstalled = true, isConfigured = true),
        ExtensionItem("mcp-browser", "MCP Web Browser", "Navigation et extraction de sources web distantes", "SEARCH", isInstalled = false, isConfigured = false, isAvailableLocally = false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions & Outils MCP", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(extensions) { ext ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ext.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(ext.description, style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (ext.isInstalled) "Actif et opérationnel" else "Non installé sur l'hôte local",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ext.isInstalled) SynapseDarkSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. Écran d'Appel Vocal Mains-Libres
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    onNavigateBack: () -> Unit
) {
    var isListening by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appel Vocal Mains-Libres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Session Vocale Synapse", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isListening) "À votre écoute… Parlez normalement." else "Appuyez sur le micro pour parler.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(if (isListening) SynapseDarkPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, if (isListening) SynapseDarkPrimary else MaterialTheme.colorScheme.outline, CircleShape)
            ) {
                if (isListening) {
                    ThinkingOrb(size = 80.dp, color = Palette.accent)
                }
                IconButton(
                    onClick = { isListening = !isListening },
                    modifier = Modifier.size(90.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = if (isListening) SynapseDarkPrimary else Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                text = "Utilise la reconnaissance vocale Android locale et la synthèse TextToSpeech intégrée.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 5. État Honnête d'Indisponibilité de l'Écran Distant
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreenUnavailable(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Écran Distant", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        EmptyStateView(
            title = "Fonctionnalité Indisponible",
            description = "La visualisation et la télécommande d'écran WebRTC nécessitent un serveur hôte de bureau équipé d'une session graphique Wayland/X11 ou macOS avec pilote de capture H.264 actif.\n\nSur un hôte local Termux / PRoot sans serveur d'affichage X11, ce service n'est pas activé.",
            symbolName = "laptopcomputer",
            actionLabel = "Retour aux paramètres",
            onActionClick = onNavigateBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
