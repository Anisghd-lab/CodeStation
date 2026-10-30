package com.ghdinteractivestudio.codestation.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ghdinteractivestudio.codestation.domain.model.ApprovalStatus
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.ChatMessage
import com.ghdinteractivestudio.codestation.domain.model.MessageAuthorType
import com.ghdinteractivestudio.codestation.domain.model.PendingAttachment
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import com.ghdinteractivestudio.codestation.util.TranscriptionResult
import com.ghdinteractivestudio.codestation.util.VoiceRecorder
import com.ghdinteractivestudio.codestation.util.VoiceTranscriptionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import com.ghdinteractivestudio.codestation.ui.components.ApprovalCardItem
import com.ghdinteractivestudio.codestation.ui.components.BotAvatar
import com.ghdinteractivestudio.codestation.ui.components.ChatMessageItem
import com.ghdinteractivestudio.codestation.ui.components.ChatMessageItem
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.GlassCodeBlocksDialog
import com.ghdinteractivestudio.codestation.ui.components.GlassMenu
import com.ghdinteractivestudio.codestation.ui.components.GlassMenuDivider
import com.ghdinteractivestudio.codestation.ui.components.GlassMenuItem
import com.ghdinteractivestudio.codestation.ui.components.GlassStage
import com.ghdinteractivestudio.codestation.ui.components.GlassTextSelectionDialog
import com.ghdinteractivestudio.codestation.ui.components.IconButton
import com.ghdinteractivestudio.codestation.ui.components.MarkdownCodeBlock
import com.ghdinteractivestudio.codestation.ui.components.MessageComposer
import com.ghdinteractivestudio.codestation.ui.components.MessageContextMenu
import com.ghdinteractivestudio.codestation.ui.components.ScrollEdge
import com.ghdinteractivestudio.codestation.ui.components.Edge
import com.ghdinteractivestudio.codestation.ui.components.Spinner
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.ThinkingOrb
import com.ghdinteractivestudio.codestation.ui.components.copyToClipboard
import com.ghdinteractivestudio.codestation.ui.components.extractMarkdownCodeBlocks
import com.ghdinteractivestudio.codestation.ui.components.glass
import com.ghdinteractivestudio.codestation.ui.components.rememberPress
import com.ghdinteractivestudio.codestation.ui.components.pressScale
import com.ghdinteractivestudio.codestation.ui.components.tr
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.util.TermuxSessionLauncher
import com.ghdinteractivestudio.codestation.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onViewTrace: () -> Unit,
    onEditBot: (String) -> Unit,
    onVoiceCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    var activeQuotedReply by remember { mutableStateOf<QuotedReply?>(null) }
    var activeContextMenuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var messageForSelection by remember { mutableStateOf<ChatMessage?>(null) }
    var codeBlocksForDialog by remember { mutableStateOf<List<MarkdownCodeBlock>?>(null) }

    // Réinitialisation des menus et du mode édition lors d'un changement de conversation
    LaunchedEffect(state.bot?.id) {
        activeContextMenuMessage = null
        messageForSelection = null
        codeBlocksForDialog = null
        if (state.editingMessage != null) {
            viewModel.cancelEditingMessage()
        }
    }

    // Gestion du bouton Retour Android : fermeture prioritaire des menus et dialogues actifs
    BackHandler(
        enabled = activeContextMenuMessage != null || messageForSelection != null || codeBlocksForDialog != null || state.editingMessage != null
    ) {
        when {
            activeContextMenuMessage != null -> activeContextMenuMessage = null
            messageForSelection != null -> messageForSelection = null
            codeBlocksForDialog != null -> codeBlocksForDialog = null
            state.editingMessage != null -> viewModel.cancelEditingMessage()
        }
    }

    val voiceRecorder = remember { VoiceRecorder(context) }
    val transcriptionService = remember { VoiceTranscriptionService(context) }
    var recordingTimerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Launcher pour sélection de photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val details = getChatUriDetails(context, uri)
            viewModel.addAttachment(
                PendingAttachment(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    name = details.first,
                    mimeType = details.second.ifBlank { "image/jpeg" },
                    sizeBytes = details.third,
                    isImage = true
                )
            )
        }
    }

    // Launcher pour sélection de vidéos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val details = getChatUriDetails(context, uri)
            viewModel.addAttachment(
                PendingAttachment(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    name = details.first,
                    mimeType = details.second.ifBlank { "video/mp4" },
                    sizeBytes = details.third,
                    isVideo = true
                )
            )
        }
    }

    // Launcher pour sélection de documents et fichiers
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val details = getChatUriDetails(context, uri)
            viewModel.addAttachment(
                PendingAttachment(
                    id = UUID.randomUUID().toString(),
                    uri = uri,
                    name = details.first,
                    mimeType = details.second.ifBlank { "application/octet-stream" },
                    sizeBytes = details.third
                )
            )
        }
    }

    // Permission audio pour enregistrement vocal
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun startVoiceRecordingAction() {
        val result = voiceRecorder.startRecording()
        if (result.isSuccess) {
            viewModel.setVoiceRecording(true, 0)
            recordingTimerJob?.cancel()
            recordingTimerJob = coroutineScope.launch {
                while (voiceRecorder.isRecording) {
                    delay(1000)
                    viewModel.setVoiceRecording(true, voiceRecorder.getDurationSeconds())
                }
            }
        } else {
            viewModel.onVoiceTranscriptionFailed("Échec de démarrage de l'enregistrement : ${result.exceptionOrNull()?.message}")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            startVoiceRecordingAction()
        } else {
            viewModel.onVoiceTranscriptionFailed("Permission microphone refusée. Impossible d'enregistrer.")
        }
    }

    fun handleStartVoice() {
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startVoiceRecordingAction()
        }
    }

    fun handleStopVoice() {
        recordingTimerJob?.cancel()
        val duration = voiceRecorder.getDurationSeconds()
        val audioFile = voiceRecorder.stopRecording()
        viewModel.setVoiceRecording(false, 0)

        if (audioFile == null || !audioFile.exists()) {
            viewModel.onVoiceTranscriptionFailed("Aucun fichier audio enregistré.")
            return
        }

        viewModel.setTranscribing(true)

        if (transcriptionService.isAvailable()) {
            coroutineScope.launch {
                transcriptionService.startListeningFlow().collect { res ->
                    when (res) {
                        is TranscriptionResult.Success -> {
                            viewModel.onVoiceTranscriptionCompleted(res.text)
                            voiceRecorder.cleanup(audioFile)
                        }
                        is TranscriptionResult.Error -> {
                            // Si la reconnaissance échoue, conserver l'enregistrement audio comme pièce jointe vocale
                            val fileUri = android.net.Uri.fromFile(audioFile)
                            viewModel.addAttachment(
                                PendingAttachment(
                                    id = java.util.UUID.randomUUID().toString(),
                                    uri = fileUri,
                                    name = "Message vocal (${duration}s).m4a",
                                    mimeType = "audio/mp4",
                                    sizeBytes = audioFile.length(),
                                    isAudio = true
                                )
                            )
                            viewModel.setTranscribing(false)
                        }
                        is TranscriptionResult.Partial -> {
                            viewModel.onVoiceTranscriptionCompleted(res.text)
                        }
                    }
                }
            }
        } else {
            // Moteur de transcription non présent : conservation garantie du message vocal en pièce jointe
            val fileUri = android.net.Uri.fromFile(audioFile)
            viewModel.addAttachment(
                PendingAttachment(
                    id = java.util.UUID.randomUUID().toString(),
                    uri = fileUri,
                    name = "Message vocal (${duration}s).m4a",
                    mimeType = "audio/mp4",
                    sizeBytes = audioFile.length(),
                    isAudio = true
                )
            )
            viewModel.setTranscribing(false)
        }
    }

    fun handleCancelVoice() {
        recordingTimerJob?.cancel()
        voiceRecorder.cancelRecording()
        viewModel.setVoiceRecording(false, 0)
    }

    val bot = state.bot
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = if (imeBottom > navBottom) imeBottom else navBottom

    var userJustSentMessage by remember { mutableStateOf(false) }
    var messagePendingDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var initialScrolledBotId by remember { mutableStateOf<String?>(null) }

    // 6.1 — Ouverture d'un bot : positionnement automatique et instantané sur le dernier message
    LaunchedEffect(state.messages.size, bot?.id) {
        if (state.messages.isNotEmpty()) {
            val currentBotId = bot?.id
            if (initialScrolledBotId != currentBotId) {
                // Accès direct sans animation lente à la fin de la conversation (dernier message)
                listState.scrollToItem(
                    index = state.messages.size - 1,
                    scrollOffset = 100000
                )
                initialScrolledBotId = currentBotId
            } else if (userJustSentMessage) {
                // L'utilisateur vient d'envoyer un message : défilement fluide vers son message
                listState.animateScrollToItem(state.messages.size - 1)
                userJustSentMessage = false
            } else {
                // Réception de nouveaux messages ou progression streaming :
                // Suivre le défilement uniquement si l'utilisateur est déjà en bas
                val totalItems = listState.layoutInfo.totalItemsCount
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val isNearBottom = totalItems <= 1 || lastVisible >= (totalItems - 3).coerceAtLeast(0)
                if (isNearBottom) {
                    listState.animateScrollToItem(state.messages.size - 1)
                }
            }
        }
    }

    // Garder le bas de la conversation visible lors de l'ouverture du clavier
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0.dp && state.messages.isNotEmpty()) {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (totalItems <= 1 || lastVisible >= (totalItems - 3).coerceAtLeast(0)) {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    GlassStage(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background),
        chrome = {
            ScrollEdge(Edge.Top, topInset + 64.dp)

            // Thread Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 6.dp)
                    .padding(horizontal = 16.dp)
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (Glass capsule)
                val (backSource, backPressed) = rememberPress()
                Box(
                    Modifier
                        .size(44.dp)
                        .glass(Capsule(), pressed = backPressed)
                        .clickable(backSource, null, role = Role.Button, onClick = onNavigateBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center
                ) {
                    Symbol("chevron.left", size = 28.dp, tint = Palette.text)
                }

                // Centered Bot Identity & Host Subtitle
                Box(
                    Modifier
                        .weight(1f)
                        .clickable { bot?.id?.let { onEditBot(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            if (bot != null) {
                                BotAvatar(bot = bot, size = 22.dp)
                                Text(
                                    bot.name,
                                    style = Type.system(14f).semibold,
                                    color = Palette.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Symbol("wifi", size = 10.dp, tint = Palette.secondary)
                            Text(
                                "Termux 127.0.0.1",
                                style = Type.system(10f).medium,
                                color = Palette.secondary
                            )
                        }
                    }
                }

                // Right: Computer / Trace / Actions
                Box {
                    IconButton("More", "ellipsis.circle", action = { isMenuOpen = true })

                    GlassMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        width = 250.dp
                    ) {
                        GlassMenuItem(
                            title = "Session Termux",
                            subtitle = "Console interactive PRoot",
                            icon = "terminal",
                            tint = Palette.electricBlue,
                            onClick = {
                                isMenuOpen = false
                                val targetBot = bot
                                if (targetBot != null) {
                                    TermuxSessionLauncher.openBotTerminalSession(
                                        context = context,
                                        botId = targetBot.id,
                                        engine = targetBot.provider.backendId
                                    )
                                } else {
                                    TermuxSessionLauncher.openTermux(context)
                                }
                            }
                        )
                        GlassMenuDivider()
                        GlassMenuItem(
                            title = "Traces d'exécution",
                            subtitle = "Logs et étapes du raisonnement",
                            icon = "doc.text",
                            tint = Palette.text,
                            onClick = {
                                isMenuOpen = false
                                onViewTrace()
                            }
                        )
                        GlassMenuDivider()
                        GlassMenuItem(
                            title = "Paramètres de l'Agent",
                            subtitle = "Modèle, prompt et permissions",
                            icon = "gearshape",
                            tint = Palette.secondary,
                            onClick = {
                                isMenuOpen = false
                                bot?.id?.let { onEditBot(it) }
                            }
                        )
                        GlassMenuDivider()
                        GlassMenuItem(
                            title = "Effacer l'historique",
                            subtitle = "Supprimer les messages locaux",
                            icon = "trash",
                            destructive = true,
                            onClick = {
                                isMenuOpen = false
                                viewModel.clearHistory()
                            }
                        )
                    }
                }
            }

            // Bottom Message Composer
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomInset)
            ) {
                // Banner d'erreur / expiration de délai avec bouton Réessayer
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    val errShape = RoundedRectangle(14.dp)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .background(Palette.danger.copy(alpha = 0.12f), errShape)
                            .border(0.5.dp, Palette.danger.copy(alpha = 0.3f), errShape)
                            .clip(errShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Symbol("exclamationmark.triangle.fill", size = 16.dp, tint = Palette.danger)
                        Text(
                            state.errorMessage ?: "Erreur",
                            style = Type.caption,
                            color = Palette.danger,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            Modifier
                                .clip(Capsule())
                                .background(Palette.accent)
                                .clickable { viewModel.retryLastMessage() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Réessayer", style = Type.caption.semibold, color = Palette.onAccent)
                        }
                        IconButton("Fermer", "xmark", action = { viewModel.dismissError() })
                    }
                }

                MessageComposer(
                    botName = bot?.name ?: "Agent",
                    isWorking = state.isBotWorking,
                    replyTo = activeQuotedReply,
                    onCancelReply = { activeQuotedReply = null },
                    editingMessage = state.editingMessage,
                    onCancelEdit = { viewModel.cancelEditingMessage() },
                    attachments = state.attachments,
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickVideo = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onPickDocument = {
                        documentPickerLauncher.launch(arrayOf("*/*"))
                    },
                    isVoiceRecording = state.isVoiceRecording,
                    voiceDurationSeconds = state.voiceDurationSeconds,
                    isTranscribing = state.isTranscribing,
                    onStartVoice = { handleStartVoice() },
                    onStopVoice = { handleStopVoice() },
                    onCancelVoice = { handleCancelVoice() },
                    onSend = { text ->
                        userJustSentMessage = true
                        viewModel.onInputChanged(text)
                        viewModel.sendMessage()
                        activeQuotedReply = null
                    },
                    onStop = { viewModel.stopTurn() }
                )
            }
        }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomInset),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topInset + 64.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Approval cards
            items(state.pendingApprovals, key = { it.id }) { approval ->
                ApprovalCardItem(
                    request = approval,
                    onApprove = { always ->
                        viewModel.respondApproval(approval.id, ApprovalStatus.APPROVED, always)
                    },
                    onReject = {
                        viewModel.respondApproval(approval.id, ApprovalStatus.REJECTED, false)
                    }
                )
            }

            // Chat Messages (Bulle adaptative, menu contextuel en glassmorphism par appui long)
            items(state.messages, key = { it.id }) { msg ->
                val isUserMsg = msg.author.type == MessageAuthorType.USER
                val alignment = if (isUserMsg) Alignment.CenterEnd else Alignment.CenterStart

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = alignment
                ) {
                    ChatMessageItem(
                        message = msg,
                        onQuoteReply = {
                            activeQuotedReply = QuotedReply(
                                messageId = it.id,
                                authorName = it.author.displayName,
                                textExcerpt = it.text.take(60)
                            )
                        },
                        onLongClick = {
                            activeContextMenuMessage = it
                        }
                    )

                    val codeBlocks = remember(msg.text) { extractMarkdownCodeBlocks(msg.text) }

                    MessageContextMenu(
                        message = msg,
                        expanded = activeContextMenuMessage?.id == msg.id,
                        isBotWorking = state.isBotWorking,
                        codeBlocksCount = codeBlocks.size,
                        onDismissRequest = { activeContextMenuMessage = null },
                        onCopy = {
                            copyToClipboard(
                                context = context,
                                text = msg.text,
                                label = if (isUserMsg) "Message CodeStation" else "Réponse CodeStation",
                                successMessage = if (isUserMsg) "Message copié dans le presse-papiers" else "Réponse copiée dans le presse-papiers"
                            )
                        },
                        onEdit = {
                            viewModel.startEditingMessage(msg)
                        },
                        onRegenerate = {
                            if (state.isBotWorking) {
                                android.widget.Toast.makeText(context, "Une génération est déjà en cours…", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.regenerateResponse(msg.id)
                            }
                        },
                        onCopyCode = {
                            if (codeBlocks.size == 1) {
                                val block = codeBlocks.first()
                                copyToClipboard(
                                    context = context,
                                    text = block.code,
                                    label = "Code",
                                    successMessage = "Bloc de code (${block.language ?: "code"}) copié !"
                                )
                            } else if (codeBlocks.size > 1) {
                                codeBlocksForDialog = codeBlocks
                            }
                        },
                        onSelectText = {
                            messageForSelection = msg
                        }
                    )
                }
            }

            // Live reasoning thinking orb indicator
            if (state.isBotWorking) {
                item("thinking") {
                    Row(
                        Modifier.padding(start = 16.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThinkingOrb(size = 18.dp, color = Palette.secondary)
                        Text(
                            "Réflexion en cours…",
                            style = Type.caption,
                            color = Palette.secondary
                        )
                    }
                }
            }
        }

        // Boîte de dialogue de confirmation de suppression de message en Frosted Liquid Glass
        if (messagePendingDelete != null) {
            val target = messagePendingDelete!!
            GlassAlertDialog(
                onDismissRequest = { messagePendingDelete = null },
                title = {
                    Text("Supprimer le message", style = Type.title3.semibold, color = Palette.text)
                },
                text = {
                    Text(
                        "Voulez-vous supprimer ce message de la discussion ? Cette action est irréversible.",
                        style = Type.subheadline,
                        color = Palette.secondary
                    )
                },
                confirmButton = {
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.danger)
                            .clickable {
                                viewModel.deleteMessage(target.id)
                                messagePendingDelete = null
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Supprimer", style = Type.caption.bold, color = Color.White)
                    }
                },
                dismissButton = {
                    Box(
                        Modifier
                            .clip(Capsule())
                            .background(Palette.text.copy(alpha = 0.1f))
                            .clickable { messagePendingDelete = null }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Annuler", style = Type.caption.semibold, color = Palette.text)
                    }
                }
            )
        }

        // Dialogue de sélection de texte précise sans conflit de geste
        if (messageForSelection != null) {
            val selMsg = messageForSelection!!
            GlassTextSelectionDialog(
                text = selMsg.text,
                title = if (selMsg.author.type == MessageAuthorType.USER) "Sélectionner le message" else "Sélectionner la réponse",
                onDismissRequest = { messageForSelection = null }
            )
        }

        // Dialogue de sélection des blocs de code pour une réponse IA
        if (codeBlocksForDialog != null) {
            GlassCodeBlocksDialog(
                codeBlocks = codeBlocksForDialog!!,
                onDismissRequest = { codeBlocksForDialog = null },
                onCopyBlock = { block ->
                    copyToClipboard(
                        context = context,
                        text = block.code,
                        label = "Code CodeStation",
                        successMessage = "Bloc (${block.language ?: "code"}) copié !"
                    )
                },
                onCopyAll = {
                    val all = codeBlocksForDialog?.joinToString("\n\n") { it.code } ?: ""
                    copyToClipboard(
                        context = context,
                        text = all,
                        label = "Code complet",
                        successMessage = "Tous les blocs de code (${codeBlocksForDialog?.size}) ont été copiés !"
                    )
                }
            )
        }
    }
}

private fun getChatUriDetails(context: Context, uri: Uri): Triple<String, String, Long> {
    var name = "fichier"
    var size = 0L
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "fichier"
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }
    return Triple(name, mimeType, size)
}
