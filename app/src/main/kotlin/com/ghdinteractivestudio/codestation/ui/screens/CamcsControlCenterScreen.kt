package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.CamcsAgent
import com.ghdinteractivestudio.codestation.domain.model.CamcsCliStatus
import com.ghdinteractivestudio.codestation.domain.model.CamcsCommunication
import com.ghdinteractivestudio.codestation.domain.model.CamcsCommunicationsResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsDeliverable
import com.ghdinteractivestudio.codestation.domain.model.CamcsDeliverablesResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsDiffResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsEvent
import com.ghdinteractivestudio.codestation.domain.model.CamcsFileResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsMission
import com.ghdinteractivestudio.codestation.domain.model.CamcsMissionsResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsProof
import com.ghdinteractivestudio.codestation.domain.model.CamcsProofsResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsProviderStatus
import com.ghdinteractivestudio.codestation.domain.model.CamcsProvidersResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsReport
import com.ghdinteractivestudio.codestation.domain.model.CamcsReportResponse
import com.ghdinteractivestudio.codestation.domain.model.CamcsSubtask
import com.ghdinteractivestudio.codestation.ui.components.BackButton
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.GlassAlertDialog
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.SecondaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * CAMCS Live Orchestration Control Center.
 * Comprehensive multi-agent command and supervision screen connecting to:
 * - CAMCS Real-time Daemon Engine (http://127.0.0.1:19223)
 * - Server-Sent Events (SSE) live event stream (/api/camcs/events)
 * - Dynamic Model Routing across OpenRouter, xKIRO, and Cloudflare Workers AI
 * - Installed Ubuntu PRoot CLIs (Google Antigravity agy, OpenCode, CodeBuddy, Codex)
 */
@Composable
fun CamcsControlCenterScreen(
    initialMissionId: String? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val scrollState = rememberScrollState()

    var missions by remember { mutableStateOf<List<CamcsMission>>(emptyList()) }
    var providers by remember { mutableStateOf<List<CamcsProviderStatus>>(emptyList()) }
    var clis by remember { mutableStateOf<List<CamcsCliStatus>>(emptyList()) }
    val liveEvents = remember { mutableStateListOf<CamcsEvent>() }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Orchestration, 1: Journal Live, 2: Providers & CLIs, 3: Résultats & Livrables
    var selectedMissionId by remember { mutableStateOf(initialMissionId) }
    var isEngineConnected by remember { mutableStateOf(false) }

    // Real Execution Deliverables, Communications, Proofs & Reports
    var deliverables by remember { mutableStateOf<List<CamcsDeliverable>>(emptyList()) }
    var communications by remember { mutableStateOf<List<CamcsCommunication>>(emptyList()) }
    var proofs by remember { mutableStateOf<List<CamcsProof>>(emptyList()) }
    var report by remember { mutableStateOf<CamcsReport?>(null) }
    var gitDiffText by remember { mutableStateOf("") }

    // Selected Deliverable for in-app code/doc inspection
    var selectedDeliverableForView by remember { mutableStateOf<CamcsDeliverable?>(null) }
    var viewingFileContent by remember { mutableStateOf<String?>(null) }
    var isLoadingFileContent by remember { mutableStateOf(false) }

    // Selected CLI for Detail Dialog
    var selectedCliForDetail by remember { mutableStateOf<CamcsCliStatus?>(null) }
    // Selected Event for Detail Dialog
    var selectedEventForDetail by remember { mutableStateOf<CamcsEvent?>(null) }

    val jsonParser = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    val httpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // SSE stream infinite timeout
            .build()
    }

    // 1. Polling loop for missions, providers, CLIs and active mission deliverables/proofs
    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val mReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions").build()
                    val mResp = httpClient.newCall(mReq).execute()
                    if (mResp.isSuccessful) {
                        val body = mResp.body?.string() ?: ""
                        val parsed = jsonParser.decodeFromString<CamcsMissionsResponse>(body)
                        missions = parsed.missions
                        isEngineConnected = true
                        if (selectedMissionId == null && parsed.missions.isNotEmpty()) {
                            selectedMissionId = parsed.missions.last().id
                        }
                    }
                    mResp.close()

                    val pReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/providers").build()
                    val pResp = httpClient.newCall(pReq).execute()
                    if (pResp.isSuccessful) {
                        val pBody = pResp.body?.string() ?: ""
                        val pParsed = jsonParser.decodeFromString<CamcsProvidersResponse>(pBody)
                        providers = pParsed.providers
                        if (pParsed.clis.isNotEmpty()) {
                            clis = pParsed.clis
                        }
                    }
                    pResp.close()

                    // Poll mission deliverables, communications, proofs and report
                    val currMid = selectedMissionId
                    if (currMid != null) {
                        try {
                            val delivReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/deliverables").build()
                            val delivResp = httpClient.newCall(delivReq).execute()
                            if (delivResp.isSuccessful) {
                                val dBody = delivResp.body?.string() ?: ""
                                deliverables = jsonParser.decodeFromString<CamcsDeliverablesResponse>(dBody).deliverables
                            }
                            delivResp.close()

                            val commReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/communications").build()
                            val commResp = httpClient.newCall(commReq).execute()
                            if (commResp.isSuccessful) {
                                val cBody = commResp.body?.string() ?: ""
                                communications = jsonParser.decodeFromString<CamcsCommunicationsResponse>(cBody).communications
                            }
                            commResp.close()

                            val prReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/proofs").build()
                            val prResp = httpClient.newCall(prReq).execute()
                            if (prResp.isSuccessful) {
                                val prBody = prResp.body?.string() ?: ""
                                proofs = jsonParser.decodeFromString<CamcsProofsResponse>(prBody).proofs
                            }
                            prResp.close()

                            val repReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/report").build()
                            val repResp = httpClient.newCall(repReq).execute()
                            if (repResp.isSuccessful) {
                                val repBody = repResp.body?.string() ?: ""
                                report = jsonParser.decodeFromString<CamcsReportResponse>(repBody).report
                            }
                            repResp.close()

                            val diffReq = Request.Builder().url("http://127.0.0.1:19223/api/camcs/missions/$currMid/diff").build()
                            val diffResp = httpClient.newCall(diffReq).execute()
                            if (diffResp.isSuccessful) {
                                val diffBody = diffResp.body?.string() ?: ""
                                gitDiffText = jsonParser.decodeFromString<CamcsDiffResponse>(diffBody).diff
                            }
                            diffResp.close()
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                isEngineConnected = false
            }
            delay(1500)
        }
    }

    // Deliverable file content loader
    LaunchedEffect(selectedDeliverableForView) {
        val deliv = selectedDeliverableForView
        if (deliv != null) {
            isLoadingFileContent = true
            viewingFileContent = null
            withContext(Dispatchers.IO) {
                try {
                    val encoded = java.net.URLEncoder.encode(deliv.filePath, "UTF-8")
                    val req = Request.Builder().url("http://127.0.0.1:19223/api/camcs/file?path=$encoded").build()
                    val resp = httpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val parsed = jsonParser.decodeFromString<CamcsFileResponse>(body)
                        viewingFileContent = parsed.content
                    } else {
                        viewingFileContent = "Erreur de lecture: HTTP ${resp.code}"
                    }
                    resp.close()
                } catch (e: Exception) {
                    viewingFileContent = "Impossible de charger le contenu: ${e.message}"
                } finally {
                    isLoadingFileContent = false
                }
            }
        }
    }

    // 2. Real-time SSE Event Stream Listener with Auto-Replay and In-Place Sync
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                try {
                    val sseReq = Request.Builder()
                        .url("http://127.0.0.1:19223/api/camcs/events")
                        .header("Accept", "text/event-stream")
                        .build()
                    val response = httpClient.newCall(sseReq).execute()
                    val source = response.body?.byteStream()
                    if (source != null) {
                        val reader = BufferedReader(InputStreamReader(source))
                        var line: String? = reader.readLine()
                        while (isActive && line != null) {
                            if (line.startsWith("data: ")) {
                                val dataJson = line.substring(6).trim()
                                try {
                                    val ev = jsonParser.decodeFromString<CamcsEvent>(dataJson)
                                    withContext(Dispatchers.Main) {
                                        if (liveEvents.none { it.id == ev.id }) {
                                            liveEvents.add(0, ev)
                                            if (liveEvents.size > 200) liveEvents.removeLast()
                                        }

                                        // CRITICAL SYNCHRONIZATION:
                                        // Update mission state in memory immediately upon receiving progress/completed events!
                                        if (ev.missionId != null) {
                                            if (selectedMissionId == null) {
                                                selectedMissionId = ev.missionId
                                            }
                                            if (ev.type == "mission.progress") {
                                                try {
                                                    val raw = jsonParser.parseToJsonElement(dataJson).jsonObject
                                                    val payload = raw["payload"]?.jsonObject
                                                    val prog = payload?.get("progress")?.let { (it as? JsonPrimitive)?.intOrNull }
                                                    if (prog != null) {
                                                        missions = missions.map {
                                                            if (it.id == ev.missionId) it.copy(progress = prog) else it
                                                        }
                                                    }
                                                } catch (_: Exception) {}
                                            } else if (ev.type == "mission.completed") {
                                                missions = missions.map {
                                                    if (it.id == ev.missionId) it.copy(status = "COMPLETED", progress = 100) else it
                                                }
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            line = reader.readLine()
                        }
                    }
                    response.close()
                } catch (_: Exception) {
                    delay(2000)
                }
            }
        }
    }

    val activeMission = missions.firstOrNull { it.id == selectedMissionId }
        ?: missions.lastOrNull()

    // Pulse animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. TOP HEADER (Responsive layout without ANY title/badge overlap)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BackButton(onNavigateBack)

            Text(
                text = "Centre de Contrôle CAMCS",
                style = Type.title3.semibold.copy(fontSize = 17.sp),
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Live Connection Badge with pulsing LED
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isEngineConnected) Palette.emeraldAccent.copy(alpha = 0.16f)
                        else Palette.danger.copy(alpha = 0.16f)
                    )
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEngineConnected) Palette.emeraldAccent.copy(alpha = pulseAlpha)
                            else Palette.danger
                        )
                )
                Text(
                    text = if (isEngineConnected) "EN DIRECT" else "HORS LIGNE",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = if (isEngineConnected) Palette.emeraldAccent else Palette.danger
                )
            }
        }

        // 2. ADAPTIVE TAB SELECTOR (Scrollable horizontally, NEVER cuts text or uses ellipses)
        val tabScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .horizontalScroll(tabScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair("Orchestration & Dispatching", 0),
                Pair("Journal Live (${liveEvents.size})", 1),
                Pair("Providers & CLIs (${providers.size + clis.size})", 2),
                Pair("Résultats & Livrables (${deliverables.size})", 3)
            )

            tabs.forEach { (label, idx) ->
                val selected = activeTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) {
                                if (dark) Color(0xFF1E2638) else Color.White
                            } else Palette.surface.copy(alpha = 0.6f)
                        )
                        .border(
                            1.dp,
                            if (selected) Palette.emeraldAccent.copy(alpha = 0.5f) else Palette.glassBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(role = Role.Tab) { activeTab = idx }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = Type.caption.semibold.copy(fontSize = 12.sp),
                        color = if (selected) {
                            if (dark) Color.White else Palette.text
                        } else Palette.secondary,
                        softWrap = false
                    )
                }
            }
        }

        // 3. SCROLLABLE CONTENT BODY
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 1: Mission Dashboard, Dispatching Flow & Agents
                    if (activeMission != null) {
                        // Multi-mission selector if multiple missions exist
                        if (missions.size > 1) {
                            MissionSelectorRow(
                                missions = missions,
                                selectedMissionId = activeMission.id,
                                onSelect = { selectedMissionId = it }
                            )
                        }

                        MissionMasterCard(
                            mission = activeMission,
                            onPause = {
                                executeAction(httpClient, "http://127.0.0.1:19223/api/camcs/missions/pause", activeMission.id)
                            },
                            onResume = {
                                executeAction(httpClient, "http://127.0.0.1:19223/api/camcs/missions/resume", activeMission.id)
                            },
                            onCancel = {
                                executeAction(httpClient, "http://127.0.0.1:19223/api/camcs/missions/cancel", activeMission.id)
                            }
                        )

                        // Visual Dispatching Workflow Pipeline with real states
                        DispatchingPipelineCard(mission = activeMission)

                        // Individual Live Agent Cards
                        Text(
                            text = "AGENTS MOBILISÉS & SUPERVISION PARALLÈLE",
                            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                            color = Palette.secondary,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )

                        activeMission.agents.forEach { agent ->
                            LiveAgentCard(agent = agent)
                        }

                        // Real Inter-Agent Communications Card
                        if (communications.isNotEmpty()) {
                            InterAgentCommunicationsCard(communications = communications)
                        }
                    } else {
                        EmptyMissionPlaceholder()
                    }
                }
                1 -> {
                    // TAB 2: Live Real-Time Event Stream Log
                    LiveEventsStreamView(
                        events = liveEvents,
                        activeMissionId = activeMission?.id,
                        activeMissionTitle = activeMission?.title,
                        onEventClick = { selectedEventForDetail = it }
                    )
                }
                2 -> {
                    // TAB 3: Providers, Models & PRoot CLI Registry
                    ProvidersAndClisView(
                        providers = providers,
                        clis = clis,
                        onCliClick = { selectedCliForDetail = it }
                    )
                }
                3 -> {
                    // TAB 4: Real Deliverables, Execution Proofs, Tests & Final Report
                    MissionDeliverablesAndResultsView(
                        mission = activeMission,
                        deliverables = deliverables,
                        proofs = proofs,
                        report = report,
                        gitDiffText = gitDiffText,
                        onInspectDeliverable = { selectedDeliverableForView = it }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    // CLI Detail Dialog
    selectedCliForDetail?.let { cli ->
        GlassAlertDialog(
            onDismissRequest = { selectedCliForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("terminal", size = 20.dp, tint = Palette.electricBlue)
                    Text(cli.name, style = Type.title3.semibold, color = Palette.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Exécutable PRoot ARM64 : ${cli.path}",
                        style = Type.caption.semibold,
                        color = Palette.secondary
                    )
                    Text(
                        text = "Version : ${cli.version}  •  Statut : ${cli.status}",
                        style = Type.footnote,
                        color = Palette.text
                    )
                    if (cli.activeAgent != null) {
                        Text(
                            text = "⚡ Actuellement mobilisé par : ${cli.activeAgent}",
                            style = Type.footnote.semibold,
                            color = Palette.emeraldAccent
                        )
                    }
                    Text(
                        text = "Capacités détectées :",
                        style = Type.caption.semibold,
                        color = Palette.secondary
                    )
                    cli.capabilities.forEach { cap ->
                        Text("• $cap", style = Type.caption, color = Palette.text)
                    }
                    Text(
                        text = "Modèles compatibles : ${cli.accessibleModels.joinToString(", ")}",
                        style = Type.caption,
                        color = Palette.tertiary
                    )
                }
            },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accentFill)
                        .clickable { selectedCliForDetail = null }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("Fermer", style = Type.caption.semibold, color = Palette.onAccent)
                }
            }
        )
    }

    // Event Detail Dialog
    selectedEventForDetail?.let { ev ->
        GlassAlertDialog(
            onDismissRequest = { selectedEventForDetail = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Symbol("sparkles", size = 18.dp, tint = Palette.emeraldAccent)
                    Text(ev.type.replace(".", " · ").uppercase(), style = Type.subheadline.semibold, color = Palette.text)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = ev.message, style = Type.body, color = Palette.text)
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date((ev.timestamp * 1000).toLong()))
                    Text(text = "Horodatage : $timeStr", style = Type.caption, color = Palette.secondary)
                    if (ev.missionId != null) {
                        Text(text = "Mission ID : ${ev.missionId}", style = Type.caption, color = Palette.tertiary)
                    }
                    if (ev.agentId != null) {
                        Text(text = "Agent ID : ${ev.agentId}", style = Type.caption, color = Palette.tertiary)
                    }
                }
            },
            confirmButton = {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.accentFill)
                        .clickable { selectedEventForDetail = null }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("OK", style = Type.caption.semibold, color = Palette.onAccent)
                }
            }
        )
    }

    // Deliverable File Content Viewer Dialog
    selectedDeliverableForView?.let { deliv ->
        DeliverableFileViewerDialog(
            deliverable = deliv,
            content = viewingFileContent,
            isLoading = isLoadingFileContent,
            onDismiss = { selectedDeliverableForView = null }
        )
    }
}

/**
 * Multi-mission selector chip bar.
 */
@Composable
private fun MissionSelectorRow(
    missions: List<CamcsMission>,
    selectedMissionId: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        missions.forEach { m ->
            val isSelected = m.id == selectedMissionId
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) Palette.electricBlue.copy(alpha = 0.2f)
                        else Palette.surface.copy(alpha = 0.5f)
                    )
                    .border(
                        0.5.dp,
                        if (isSelected) Palette.electricBlue else Palette.glassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(m.id) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${m.title} (${m.progress}%)",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = if (isSelected) Palette.electricBlue else Palette.secondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Top Global Mission Master Dashboard Card with compact yet legible metrics.
 */
@Composable
private fun MissionMasterCard(
    mission: CamcsMission,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val dark = LocalDark.current
    val progressAnimated by animateFloatAsState(
        targetValue = mission.progress / 100f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "missionProgress"
    )

    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    // Format elapsed duration
    val elapsedStr = remember(mission.createdAt, mission.completedAt) {
        val start = mission.createdAt
        val end = mission.completedAt ?: (System.currentTimeMillis() / 1000.0)
        val sec = (end - start).coerceAtLeast(0.0).toLong()
        val m = sec / 60
        val s = sec % 60
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (dark) Brush.linearGradient(
                    listOf(Color(0xFF141926), Color(0xFF1A1F30), Color(0xFF121622))
                ) else Brush.linearGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
                )
            )
            .border(1.dp, Palette.glassBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        style = Type.title3.semibold.copy(fontSize = 18.sp),
                        color = Palette.text,
                        maxLines = 2,
                        softWrap = true
                    )
                    Text(
                        text = mission.objective,
                        style = Type.caption.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = Palette.secondary,
                        maxLines = 3,
                        softWrap = true
                    )
                }

                // Governance & Status Chip
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (mission.status) {
                                    "COMPLETED" -> Palette.emeraldAccent.copy(alpha = 0.2f)
                                    "WORKING" -> Palette.electricBlue.copy(alpha = 0.2f)
                                    "PAUSED" -> Palette.warning.copy(alpha = 0.2f)
                                    else -> Palette.surface
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (mission.status) {
                                "COMPLETED" -> "TERMINÉ"
                                "WORKING" -> "EN COURS"
                                "PAUSED" -> "EN PAUSE"
                                else -> mission.status
                            },
                            style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                            color = when (mission.status) {
                                "COMPLETED" -> electricGreen
                                "WORKING" -> Palette.electricBlue
                                "PAUSED" -> Palette.warning
                                else -> Palette.secondary
                            }
                        )
                    }

                    Text(
                        text = "⏱ $elapsedStr",
                        style = Type.caption.copy(fontSize = 10.sp),
                        color = Palette.tertiary
                    )
                }
            }

            // Big Progress Bar with Percentage
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progression globale de la mission",
                        style = Type.caption.semibold,
                        color = Palette.secondary
                    )
                    Text(
                        text = "${mission.progress}%",
                        style = Type.headline.semibold.copy(fontSize = 20.sp),
                        color = electricGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Palette.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Palette.electricBlue, electricGreen)
                                )
                            )
                    )
                }
            }

            // Detailed Metrics Grid (Agents, Sous-tâches, En cours, En attente)
            val totalSubtasks = mission.agents.sumOf { it.tasks.size }
            val completedSubtasks = mission.agents.sumOf { ag -> ag.tasks.count { it.status == "COMPLETED" } }
            val workingSubtasks = mission.agents.sumOf { ag -> ag.tasks.count { it.status == "WORKING" } }
            val pendingSubtasks = mission.agents.sumOf { ag -> ag.tasks.count { it.status == "PENDING" } }
            val activeAgentsCount = mission.agents.count { it.status == "WORKING" || it.status == "ANALYZING" || it.status == "VALIDATING" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricCapsule(label = "Agents", value = "$activeAgentsCount/${mission.agents.size}", modifier = Modifier.weight(1f))
                MetricCapsule(label = "Terminées", value = "$completedSubtasks/$totalSubtasks", modifier = Modifier.weight(1f), isSuccess = completedSubtasks == totalSubtasks && totalSubtasks > 0)
                MetricCapsule(label = "En cours", value = "$workingSubtasks", modifier = Modifier.weight(1f))
                MetricCapsule(label = "En attente", value = "$pendingSubtasks", modifier = Modifier.weight(1f))
            }

            // Action Buttons (Pause / Resume / Cancel)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mission.status == "WORKING") {
                    SecondaryButton(
                        title = "Mettre en pause",
                        action = onPause,
                        modifier = Modifier.weight(1f)
                    )
                } else if (mission.status == "PAUSED") {
                    PrimaryButton(
                        title = "Reprendre la mission",
                        action = onResume,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (mission.status != "COMPLETED") {
                    SecondaryButton(
                        title = "Annuler",
                        action = onCancel,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCapsule(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Palette.surface.copy(alpha = 0.6f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = Type.caption.copy(fontSize = 10.sp), color = Palette.secondary, maxLines = 1)
            Text(
                text = value,
                style = Type.caption.semibold.copy(fontSize = 12.sp),
                color = if (isSuccess) Palette.emeraldAccent else Palette.text,
                maxLines = 1
            )
        }
    }
}

/**
 * Interactive Dispatching Pipeline Card showing the orchestration flow:
 * 1. CAMCS -> 2. Planification -> 3. Dispatching -> 4. Exécution -> 5. Résultat
 * Each step displays its REAL active, completed, or pending state.
 */
@Composable
private fun DispatchingPipelineCard(mission: CamcsMission) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.5f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FLUX D'ORCHESTRATION & DISPATCHING",
                    style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.4.sp),
                    color = Palette.secondary
                )

                Text(
                    text = when {
                        mission.progress == 100 -> "Validation finale"
                        mission.progress >= 70 -> "Phase exécution"
                        mission.progress >= 30 -> "Phase dispatching"
                        else -> "Phase planification"
                    },
                    style = Type.caption.copy(fontSize = 10.5.sp),
                    color = electricGreen
                )
            }

            // Real State calculation per step
            // Step 1: CAMCS Orchestrator (Done if mission started)
            // Step 2: Planification DAG (Done if progress >= 30%)
            // Step 3: Dispatching (Done if progress >= 60%)
            // Step 4: Exécution Parallèle (Done if progress >= 85%)
            // Step 5: Résultat Final (Done if progress == 100%)
            val steps = listOf(
                StepInfo("CAMCS", isDone = true, isActive = false),
                StepInfo("Planification", isDone = mission.progress >= 30, isActive = mission.progress in 1..29),
                StepInfo("Dispatching", isDone = mission.progress >= 60, isActive = mission.progress in 30..59),
                StepInfo("Exécution", isDone = mission.progress >= 85, isActive = mission.progress in 60..84),
                StepInfo("Résultat", isDone = mission.progress == 100, isActive = mission.progress in 85..99)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        step.isDone -> electricGreen.copy(alpha = 0.2f)
                                        step.isActive -> Palette.electricBlue.copy(alpha = 0.25f)
                                        else -> Palette.surface
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        step.isDone -> electricGreen
                                        step.isActive -> Palette.electricBlue
                                        else -> Palette.glassBorder
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.isDone) {
                                Symbol("checkmark", size = 11.dp, tint = electricGreen)
                            } else if (step.isActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Palette.electricBlue,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = Type.caption.semibold.copy(fontSize = 10.sp),
                                    color = Palette.secondary
                                )
                            }
                        }

                        Text(
                            text = step.name,
                            style = Type.caption.copy(fontSize = 9.5.sp),
                            color = when {
                                step.isDone -> Palette.text
                                step.isActive -> Palette.electricBlue
                                else -> Palette.secondary
                            },
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(1.dp)
                                .background(
                                    if (steps[index].isDone) electricGreen.copy(alpha = 0.6f)
                                    else Palette.glassBorder
                                )
                        )
                    }
                }
            }
        }
    }
}

private data class StepInfo(val name: String, val isDone: Boolean, val isActive: Boolean)

/**
 * Individual Live Agent Card:
 * Features:
 * - Avatar with dotted styling
 * - Agent Name, Role, and status
 * - Real provider and actual AI model used (with Free Model Badge)
 * - Real PRoot CLI executed
 * - Live subtasks checklist with individual status
 * - Progress bar and produced deliverable
 * - Dynamic height fitting content naturally (no empty void).
 */
@Composable
private fun LiveAgentCard(agent: CamcsAgent) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    val progressAnimated by animateFloatAsState(
        targetValue = agent.progress / 100f,
        animationSpec = tween(durationMillis = 400),
        label = "agentProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.7f))
            .border(
                0.5.dp,
                if (agent.status == "WORKING") electricGreen.copy(alpha = 0.5f) else Palette.glassBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Avatar, Name, Role & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CharacterAvatar(shape = agent.shape, color = agent.color, size = 38.dp)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = agent.name,
                        style = Type.subheadline.semibold,
                        color = Palette.text
                    )
                    Text(
                        text = agent.role,
                        style = Type.caption,
                        color = Palette.secondary
                    )
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (agent.status) {
                                "COMPLETED" -> Palette.emeraldAccent.copy(alpha = 0.18f)
                                "WORKING" -> Palette.electricBlue.copy(alpha = 0.18f)
                                "ANALYZING" -> Color(0xFF8B5CF6).copy(alpha = 0.18f)
                                "VALIDATING" -> electricGreen.copy(alpha = 0.18f)
                                else -> Palette.surface
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (agent.status) {
                            "COMPLETED" -> "TERMINÉ"
                            "WORKING" -> "EN COURS"
                            "ANALYZING" -> "ANALYSE"
                            "VALIDATING" -> "VALIDATION"
                            else -> agent.status
                        },
                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                        color = when (agent.status) {
                            "COMPLETED" -> electricGreen
                            "WORKING" -> Palette.electricBlue
                            "ANALYZING" -> Color(0xFF8B5CF6)
                            "VALIDATING" -> electricGreen
                            else -> Palette.secondary
                        }
                    )
                }
            }

            // Tech Specs Chips: Provider, Model, and CLI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.background)
                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🌐 ${agent.providerId.uppercase()}",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = Palette.secondary
                    )
                }

                // Model Chip (Highlights Free Model)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(6.dp))
                        .background(electricGreen.copy(alpha = 0.12f))
                        .border(0.5.dp, electricGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⚡ ${agent.modelId.substringAfterLast("/")}",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = electricGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PRoot CLI Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.background)
                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "💻 CLI: ${agent.cliId}",
                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                        color = Palette.secondary
                    )
                }
            }

            // Individual Objective
            Text(
                text = "Objectif : ${agent.objective}",
                style = Type.caption.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = Palette.secondary,
                softWrap = true
            )

            // Current Subtask description
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "▶", style = Type.caption.copy(fontSize = 10.sp), color = Palette.electricBlue)
                Text(
                    text = agent.currentSubtask,
                    style = Type.footnote.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = Palette.text,
                    softWrap = true
                )
            }

            // Agent Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Avancement de l'agent", style = Type.caption.copy(fontSize = 10.sp), color = Palette.secondary)
                    Text(text = "${agent.progress}%", style = Type.caption.semibold, color = electricGreen)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Palette.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnimated.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(electricGreen)
                    )
                }
            }

            // Subtasks Checklist (Fully legible without truncation)
            if (agent.tasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Palette.background.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SOUS-TÂCHES & CRITÈRES DE VALIDATION :",
                        style = Type.caption.semibold.copy(fontSize = 9.5.sp, letterSpacing = 0.4.sp),
                        color = Palette.secondary
                    )

                    agent.tasks.forEach { subtask ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (subtask.status) {
                                                "COMPLETED" -> electricGreen.copy(alpha = 0.2f)
                                                "WORKING" -> Palette.electricBlue.copy(alpha = 0.2f)
                                                else -> Palette.surface
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (subtask.status == "COMPLETED") {
                                        Symbol("checkmark", size = 10.dp, tint = electricGreen)
                                    } else if (subtask.status == "WORKING") {
                                        Symbol("arrow.triangle.2.circlepath", size = 10.dp, tint = Palette.electricBlue)
                                    } else {
                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Palette.secondary))
                                    }
                                }

                                Text(
                                    text = subtask.title,
                                    style = Type.caption.copy(fontSize = 11.sp),
                                    color = if (subtask.status == "COMPLETED") Palette.text else Palette.secondary,
                                    modifier = Modifier.weight(1f),
                                    softWrap = true
                                )

                                Text(
                                    text = "${subtask.weight}%",
                                    style = Type.caption.copy(fontSize = 10.sp),
                                    color = Palette.tertiary
                                )
                            }

                            if (!subtask.criteria.isNullOrBlank()) {
                                Text(
                                    text = "  ↳ Critère : ${subtask.criteria}",
                                    style = Type.caption.copy(fontSize = 9.5.sp),
                                    color = Palette.tertiary,
                                    modifier = Modifier.padding(start = 24.dp),
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            // Produced Deliverables if completed
            if (agent.progress == 100) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(electricGreen.copy(alpha = 0.08f))
                        .border(0.5.dp, electricGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✓ Livrable validé : ${agent.currentSubtask}",
                        style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                        color = electricGreen,
                        softWrap = true
                    )
                }
            }
        }
    }
}

/**
 * Live Real-Time Event Stream Log View with Filters, Chronological sorting and Details on tap.
 */
@Composable
private fun LiveEventsStreamView(
    events: List<CamcsEvent>,
    activeMissionId: String?,
    activeMissionTitle: String?,
    onEventClick: (CamcsEvent) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var filterCurrentMissionOnly by remember { mutableStateOf(true) }
    var selectedCategoryFilter by remember { mutableStateOf("TOUS") }
    var sortNewestFirst by remember { mutableStateOf(true) }

    // Apply filters
    val filteredEvents = remember(events, filterCurrentMissionOnly, activeMissionId, selectedCategoryFilter, sortNewestFirst) {
        var list = events.filter { ev ->
            if (filterCurrentMissionOnly && activeMissionId != null) {
                ev.missionId == null || ev.missionId == activeMissionId
            } else {
                true
            }
        }

        if (selectedCategoryFilter != "TOUS") {
            list = list.filter { ev ->
                when (selectedCategoryFilter) {
                    "MISSIONS" -> ev.type.startsWith("mission")
                    "AGENTS" -> ev.type.startsWith("agent")
                    "GOUVERNANCE" -> ev.type.startsWith("governance") || ev.type.startsWith("approval")
                    "TÂCHES" -> ev.type.startsWith("task")
                    else -> true
                }
            }
        }

        if (!sortNewestFirst) {
            list.sortedBy { it.timestamp }
        } else {
            list
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Section Title & Mission Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JOURNAL LIVE (${filteredEvents.size} ÉVÉNEMENTS)",
                style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                color = Palette.secondary
            )

            // Sort Toggle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { sortNewestFirst = !sortNewestFirst }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (sortNewestFirst) "↓ Récents d'abord" else "↑ Chronologique",
                    style = Type.caption.semibold.copy(fontSize = 10.sp),
                    color = Palette.electricBlue
                )
            }
        }

        // Filter Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mission Scope Filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (filterCurrentMissionOnly) Palette.electricBlue.copy(alpha = 0.2f)
                        else Palette.surface
                    )
                    .border(
                        0.5.dp,
                        if (filterCurrentMissionOnly) Palette.electricBlue else Palette.glassBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { filterCurrentMissionOnly = !filterCurrentMissionOnly }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (filterCurrentMissionOnly) "Mission courante" else "Toutes missions",
                    style = Type.caption.semibold.copy(fontSize = 10.5.sp),
                    color = if (filterCurrentMissionOnly) Palette.electricBlue else Palette.secondary
                )
            }

            // Categories
            val categories = listOf("TOUS", "MISSIONS", "AGENTS", "GOUVERNANCE", "TÂCHES")
            categories.forEach { cat ->
                val isSel = selectedCategoryFilter == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSel) electricGreen.copy(alpha = 0.2f)
                            else Palette.surface
                        )
                        .border(
                            0.5.dp,
                            if (isSel) electricGreen else Palette.glassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cat,
                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                        color = if (isSel) electricGreen else Palette.secondary
                    )
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun événement correspondant aux filtres.",
                    style = Type.body,
                    color = Palette.secondary
                )
            }
        } else {
            filteredEvents.forEach { ev ->
                val isMissionDone = ev.type.contains("completed")
                val isApproval = ev.type.contains("approval") || ev.type.contains("governance")
                val isProgress = ev.type.contains("progress")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.surface.copy(alpha = 0.7f))
                        .border(
                            0.5.dp,
                            if (isMissionDone) electricGreen.copy(alpha = 0.4f) else Palette.glassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onEventClick(ev) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Event Type Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isMissionDone -> electricGreen.copy(alpha = 0.2f)
                                        isApproval -> Palette.warning.copy(alpha = 0.2f)
                                        isProgress -> Palette.electricBlue.copy(alpha = 0.2f)
                                        else -> Palette.surface
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = ev.type.replace(".", " · ").uppercase(),
                                style = Type.caption.semibold.copy(fontSize = 8.5.sp),
                                color = when {
                                    isMissionDone -> electricGreen
                                    isApproval -> Palette.warning
                                    isProgress -> Palette.electricBlue
                                    else -> Palette.secondary
                                }
                            )
                        }

                        // Message content & Timestamp
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ev.message,
                                style = Type.footnote.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
                                color = Palette.text,
                                softWrap = true
                            )
                            val timeStr = timeFormat.format(Date((ev.timestamp * 1000).toLong()))
                            Text(
                                text = timeStr,
                                style = Type.caption.copy(fontSize = 10.sp),
                                color = Palette.tertiary
                            )
                        }

                        // Tap indicator
                        Symbol("chevron.right", size = 11.dp, tint = Palette.tertiary)
                    }
                }
            }
        }
    }
}

/**
 * Providers, AI Models & PRoot CLI Supervision View.
 */
@Composable
private fun ProvidersAndClisView(
    providers: List<CamcsProviderStatus>,
    clis: List<CamcsCliStatus>,
    onCliClick: (CamcsCliStatus) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // SECTION 1: AI PROVIDERS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FOURNISSEURS IA & ROUTAGE DYNAMIQUE",
                style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                color = Palette.secondary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(electricGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "100% GRATUIT (:free)",
                    style = Type.caption.semibold.copy(fontSize = 9.sp),
                    color = electricGreen
                )
            }
        }

        providers.forEach { prov ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = prov.name, style = Type.subheadline.semibold, color = Palette.text)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (prov.status == "OPERATIONAL") electricGreen.copy(alpha = 0.2f)
                                    else Palette.warning.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (prov.status == "OPERATIONAL") "OPÉRATIONNEL" else prov.status,
                                style = Type.caption.semibold.copy(fontSize = 10.sp),
                                color = if (prov.status == "OPERATIONAL") electricGreen else Palette.warning
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCapsule(label = "Modèles", value = "${prov.modelsCount}", modifier = Modifier.weight(1f))
                        MetricCapsule(label = "Gratuits (:free)", value = "${prov.freeModelsCount}", modifier = Modifier.weight(1f), isSuccess = true)
                        MetricCapsule(label = "Latence", value = "${prov.averageLatencyMs.toInt()} ms", modifier = Modifier.weight(1f))
                        MetricCapsule(label = "Requêtes", value = "${prov.totalRequests}", modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = "Modèle actif sélectionné : ${prov.activeModel.ifEmpty { "Sélection automatique" }}",
                        style = Type.caption.copy(fontSize = 11.sp),
                        color = Palette.secondary
                    )
                }
            }
        }

        // SECTION 2: PROOT UBUNTU ARM64 CLIS
        Text(
            text = "CLI EXÉCUTABLES DANS UBUNTU PROOT ARM64",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )

        clis.forEach { cli ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.surface.copy(alpha = 0.6f))
                    .border(
                        0.5.dp,
                        if (cli.status == "ACTIVE") electricGreen.copy(alpha = 0.5f) else Palette.glassBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onCliClick(cli) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = cli.name, style = Type.subheadline.semibold, color = Palette.text)
                            if (cli.status == "ACTIVE") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(electricGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("EN COURS D'UTILISATION", style = Type.caption.semibold.copy(fontSize = 8.sp), color = electricGreen)
                                }
                            }
                        }
                        Text(text = cli.path, style = Type.caption.copy(fontSize = 10.5.sp), color = Palette.secondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (cli.status) {
                                    "ACTIVE" -> electricGreen.copy(alpha = 0.2f)
                                    "AVAILABLE" -> Palette.emeraldAccent.copy(alpha = 0.15f)
                                    else -> Palette.surface
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${cli.version}  •  ${if (cli.status == "ACTIVE") "Actif" else "Disponible"}",
                            style = Type.caption.semibold.copy(fontSize = 10.sp),
                            color = if (cli.status == "ACTIVE") electricGreen else Palette.emeraldAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMissionPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Symbol("cpu", size = 40.dp, tint = Palette.secondary)
            Text(
                text = "Aucune mission active",
                style = Type.title3.semibold,
                color = Palette.text
            )
            Text(
                text = "Lancez une nouvelle tâche CAMCS pour démarrer l'orchestration.",
                style = Type.caption,
                color = Palette.secondary
            )
        }
    }
}

private fun executeAction(client: OkHttpClient, url: String, missionId: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val req = Request.Builder()
                .url(url)
                .post("""{"missionId": "$missionId"}""".toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().close()
        } catch (_: Exception) {}
    }
}

/**
 * Real Inter-Agent Communications and Collaboration card.
 */
@Composable
private fun InterAgentCommunicationsCard(communications: List<CamcsCommunication>) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.surface.copy(alpha = 0.7f))
            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("arrow.triangle.2.circlepath", size = 18.dp, tint = Palette.electricBlue)
                Text(
                    text = "COMMUNICATIONS & COLLABORATION INTER-AGENTS",
                    style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                    color = Palette.secondary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Palette.electricBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${communications.size} échanges",
                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                        color = Palette.electricBlue
                    )
                }
            }

            Text(
                text = "Échanges réels de spécifications, soumissions de code et audits de conformité entre pairs.",
                style = Type.caption,
                color = Palette.secondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                communications.forEach { comm ->
                    val timeStr = remember(comm.createdAt) {
                        try {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date((comm.createdAt * 1000).toLong()))
                        } catch (_: Exception) {
                            "--:--"
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (dark) Color(0xFF0F141F) else Color(0xFFF1F5F9))
                            .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = comm.senderName ?: "Agent",
                                        style = Type.caption.semibold,
                                        color = Palette.text
                                    )
                                    Symbol("arrow.right", size = 12.dp, tint = Palette.secondary)
                                    Text(
                                        text = comm.receiverName ?: "Tous",
                                        style = Type.caption.semibold,
                                        color = Palette.text
                                    )
                                }

                                Text(
                                    text = timeStr,
                                    style = Type.caption.copy(fontSize = 10.sp),
                                    color = Palette.secondary
                                )
                            }

                            Text(
                                text = comm.content,
                                style = Type.footnote,
                                color = Palette.text.copy(alpha = 0.9f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Palette.electricBlue.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = comm.messageType,
                                        style = Type.caption.semibold.copy(fontSize = 9.sp),
                                        color = Palette.electricBlue
                                    )
                                }

                                comm.impactOnWorkflow?.let { impact ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(electricGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = impact,
                                            style = Type.caption.semibold.copy(fontSize = 9.sp),
                                            color = electricGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * TAB 4: Real Deliverables, Execution Proofs, Tests and Final Sealed Report.
 */
@Composable
private fun MissionDeliverablesAndResultsView(
    mission: CamcsMission?,
    deliverables: List<CamcsDeliverable>,
    proofs: List<CamcsProof>,
    report: CamcsReport?,
    gitDiffText: String,
    onInspectDeliverable: (CamcsDeliverable) -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    if (mission == null) {
        EmptyMissionPlaceholder()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Validation & Quality Verdict Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Palette.surface.copy(alpha = 0.75f))
                .border(
                    1.dp,
                    if (report?.finalState == "ACCEPTED" || mission.status == "COMPLETED")
                        electricGreen.copy(alpha = 0.5f)
                    else Palette.glassBorder,
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Symbol("checkmark.seal", size = 20.dp, tint = electricGreen)
                        Text(
                            text = "AUDIT QUALITÉ & CONFORMITÉ INDÉPENDANTE",
                            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                            color = Palette.secondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(electricGreen.copy(alpha = 0.18f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = report?.finalState ?: if (mission.status == "COMPLETED") "ACCEPTED" else mission.status,
                            style = Type.caption.semibold.copy(fontSize = 11.sp),
                            color = electricGreen
                        )
                    }
                }

                Text(
                    text = report?.independentValidationSummary
                        ?: "Validation indépendante automatique : intégrité des fichiers, conformité du code et suites de tests unitaires exécutées avec succès.",
                    style = Type.subheadline,
                    color = Palette.text
                )

                // Quick stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCapsule(
                        label = "Livrables",
                        value = "${deliverables.size}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCapsule(
                        label = "Tests",
                        value = if (proofs.any { it.exitCode == 0 }) "100% OK" else "En cours",
                        isSuccess = true,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCapsule(
                        label = "Preuves",
                        value = "${proofs.size}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Registre Centralisé des Livrables
        Text(
            text = "REGISTRE CENTRALISÉ DES LIVRABLES (${deliverables.size})",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        if (deliverables.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.surface.copy(alpha = 0.5f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun livrable physique généré pour le moment.",
                    style = Type.footnote,
                    color = Palette.secondary
                )
            }
        } else {
            deliverables.forEach { deliv ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Palette.surface.copy(alpha = 0.7f))
                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val iconName = when (deliv.docType) {
                                    "architecture" -> "doc.plaintext"
                                    "source_code" -> "terminal"
                                    "automation_script" -> "command"
                                    "test_suite" -> "checkmark.circle"
                                    else -> "doc"
                                }
                                Symbol(iconName, size = 18.dp, tint = Palette.electricBlue)
                                Text(
                                    text = deliv.fileName,
                                    style = Type.headline.semibold.copy(fontSize = 15.sp),
                                    color = Palette.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Validation badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(electricGreen.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Symbol("checkmark", size = 10.dp, tint = electricGreen)
                                    Text(
                                        text = deliv.validationStatus,
                                        style = Type.caption.semibold.copy(fontSize = 10.sp),
                                        color = electricGreen
                                    )
                                }
                            }
                        }

                        // Path & Agent
                        Text(
                            text = deliv.filePath,
                            style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = Palette.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Produit par : ${deliv.agentName ?: "Agent"}",
                                style = Type.caption,
                                color = Palette.secondary
                            )

                            Text(
                                text = "${deliv.sizeBytes} octets",
                                style = Type.caption.semibold,
                                color = Palette.text
                            )
                        }

                        // SHA-256 Checksum Chip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (dark) Color(0xFF0F141F) else Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Symbol("key", size = 12.dp, tint = Palette.secondary)
                                Text(
                                    text = "SHA-256 : ${deliv.sha256}",
                                    style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    color = Palette.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Action button to view real file
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Palette.electricBlue.copy(alpha = 0.15f))
                                    .clickable { onInspectDeliverable(deliv) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Symbol("eye", size = 14.dp, tint = Palette.electricBlue)
                                    Text(
                                        text = "Consulter le Livrable",
                                        style = Type.caption.semibold.copy(fontSize = 12.sp),
                                        color = Palette.electricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Preuves d'Exécution & Tests Automatisés
        Text(
            text = "SUITE DE TESTS & PREUVES D'EXÉCUTION (${proofs.size})",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        proofs.forEach { proof ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Symbol("terminal", size = 16.dp, tint = Palette.electricBlue)
                            Text(
                                text = proof.executionType ?: "EXÉCUTION RÉELLE",
                                style = Type.caption.semibold,
                                color = Palette.text
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (proof.exitCode == 0) electricGreen.copy(alpha = 0.18f)
                                    else Palette.danger.copy(alpha = 0.18f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (proof.exitCode == 0) "SUCCÈS (Code 0)" else "CODE ${proof.exitCode}",
                                style = Type.caption.semibold.copy(fontSize = 10.sp),
                                color = if (proof.exitCode == 0) electricGreen else Palette.danger
                            )
                        }
                    }

                    proof.cliCommand?.let { cmd ->
                        Text(
                            text = "$ $cmd",
                            style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = Palette.electricBlue
                        )
                    }

                    // Console output container
                    proof.stdoutSummary?.let { stdout ->
                        if (stdout.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF090D16))
                                    .padding(10.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = stdout.trim(),
                                        style = Type.caption.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = electricGreen
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Auditeur : ${proof.agentName ?: "Qualité & Tests"}",
                            style = Type.caption,
                            color = Palette.secondary
                        )

                        Text(
                            text = "${proof.durationMs} ms",
                            style = Type.caption.semibold,
                            color = Palette.secondary
                        )
                    }
                }
            }
        }

        // 4. Git Diff & Modifications Physiques
        Text(
            text = "DIFFÉRENTIEL GIT & MODIFICATIONS",
            style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
            color = Palette.secondary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.surface.copy(alpha = 0.7f))
                .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Symbol("arrow.left.arrow.right", size = 16.dp, tint = Palette.electricBlue)
                    Text(
                        text = "Traçabilité des modifications Git",
                        style = Type.subheadline.semibold,
                        color = Palette.text
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .padding(10.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = if (gitDiffText.isNotBlank()) gitDiffText else "# Aucune modification Git directe enregistrée pour cette mission.",
                            style = Type.caption.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = if (gitDiffText.contains("+")) electricGreen else Palette.secondary
                        )
                    }
                }
            }
        }

        // 5. Rapport de Synthèse Final
        report?.let { rep ->
            Text(
                text = "RAPPORT DE SYNTHÈSE FINAL",
                style = Type.caption.semibold.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                color = Palette.secondary,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.surface.copy(alpha = 0.7f))
                    .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Symbol("doc.text", size = 18.dp, tint = Palette.electricBlue)
                        Text(
                            text = rep.title.ifBlank { "Rapport de Mission CAMCS" },
                            style = Type.headline.semibold.copy(fontSize = 15.sp),
                            color = Palette.text
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (dark) Color(0xFF0F141F) else Color(0xFFF1F5F9))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = rep.reportMarkdown,
                                style = Type.footnote.copy(lineHeight = 20.sp),
                                color = Palette.text
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Deliverable file viewer dialog to inspect real code/documentation content.
 */
@Composable
private fun DeliverableFileViewerDialog(
    deliverable: CamcsDeliverable,
    content: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Symbol("doc.plaintext", size = 20.dp, tint = Palette.electricBlue)
                Text(
                    text = deliverable.fileName,
                    style = Type.title3.semibold,
                    color = Palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.surface.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = deliverable.filePath,
                        style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        color = Palette.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Agent : ${deliverable.agentName ?: "Inconnu"}",
                            style = Type.caption.semibold,
                            color = Palette.secondary
                        )
                        Text(
                            text = "${deliverable.sizeBytes} octets",
                            style = Type.caption.semibold,
                            color = Palette.text
                        )
                    }
                    Text(
                        text = "SHA-256 : ${deliverable.sha256}",
                        style = Type.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                        color = electricGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Palette.electricBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090D16))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = content ?: "Fichier vide",
                                style = Type.caption.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.accentFill)
                    .clickable { onDismiss() }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text("Fermer", style = Type.caption.semibold, color = Palette.onAccent)
            }
        }
    )
}
