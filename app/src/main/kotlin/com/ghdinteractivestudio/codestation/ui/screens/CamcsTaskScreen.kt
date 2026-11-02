package com.ghdinteractivestudio.codestation.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.CamcsGovernanceMode
import com.ghdinteractivestudio.codestation.ui.components.CardForm
import com.ghdinteractivestudio.codestation.ui.components.CardSection
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.CodeStationSwitch
import com.ghdinteractivestudio.codestation.ui.components.PageHeader
import com.ghdinteractivestudio.codestation.ui.components.PrimaryButton
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.abs

/**
 * Functional Task Creation & CAMCS Multi-Agent Orchestration Screen.
 * Prepares the multi-agent orchestration session:
 * - Goal & objective description with full keyboard typing support.
 * - Automatic or manual agent selection (Antigravity, OpenCode, CodeBuddy, Codex).
 * - Enabled capabilities (Files, Web Search, CLI, Tools).
 * - Governance & Security mode selection (Autonomous, Human Validation, Hybrid).
 * - System-aware docked launch button with loading state.
 * - Keyboard dismiss ONLY on user finger drag (not on programmatic scroll).
 */
@Composable
fun CamcsTaskScreen(
    availableBots: List<Bot>,
    onLaunchTask: (objective: String, missionId: String?, governanceMode: CamcsGovernanceMode) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var objective by remember { mutableStateOf("") }
    var selectedGovernanceMode by remember { mutableStateOf(CamcsGovernanceMode.HYBRID) }
    var isLaunching by remember { mutableStateOf(false) }

    // ONLY dismiss keyboard when user physically drags with finger, NEVER on programmatic auto-scroll
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && abs(available.y) > 14f) {
                    keyboardController?.hide()
                }
                return Offset.Zero
            }
        }
    }

    // Capabilities toggles
    var allowFiles by remember { mutableStateOf(true) }
    var allowWebSearch by remember { mutableStateOf(true) }
    var allowCli by remember { mutableStateOf(true) }
    var allowAutomation by remember { mutableStateOf(true) }

    // Agent selection
    val selectedAgentIds = remember {
        mutableStateListOf<String>().apply {
            addAll(availableBots.map { it.id })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        PageHeader(
            title = "Nouvelle tâche CAMCS",
            pop = onNavigateBack
        )

        CardForm(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(nestedScrollConnection),
            scrollState = scrollState
        ) {
            // 1. Goal Description Section
            CardSection(title = "Objectif de la mission") {
                row {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Décrivez précisément le résultat attendu :",
                            style = Type.caption,
                            color = Palette.secondary
                        )

                        BasicTextField(
                            value = objective,
                            onValueChange = { objective = it },
                            textStyle = Type.body.copy(color = Palette.text, lineHeight = 22.sp),
                            cursorBrush = SolidColor(Palette.accent),
                            minLines = 4,
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Default
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (objective.isEmpty()) {
                                        Text(
                                            text = "ex: Analyse les logs d'erreurs, corrige les fuites mémoire dans le service de streaming et lance la suite de tests unitaires.",
                                            style = Type.body,
                                            color = Palette.tertiary
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }
                }
            }

            // 2. Agents Selection Section
            CardSection(title = "Agents mobilisés") {
                row {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Orchestration multi-agents coordonnée",
                            style = Type.subheadline.semibold,
                            color = Palette.text
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val defaultAgents = listOf(
                                Triple("Google Antigravity", "green", "squircle"),
                                Triple("OpenCode", "magenta", "blob"),
                                Triple("CodeBuddy Code", "blue", "tablet"),
                                Triple("Codex", "violet", "pebble")
                            )

                            defaultAgents.forEach { (name, color, shape) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Palette.surface.copy(alpha = 0.8f))
                                        .border(0.5.dp, Palette.glassBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CharacterAvatar(shape, color, 20.dp)
                                        Text(
                                            text = name.split(" ").first(),
                                            style = Type.caption.semibold,
                                            color = Palette.text
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Capabilities and Permissions Section
            CardSection(title = "Capacités & Outils") {
                row {
                    CodeStationSwitch(
                        checked = allowFiles,
                        onChange = { allowFiles = it }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Symbol("folder", size = 18.dp, tint = Palette.electricBlue)
                            Text("📁 Fichiers & projet", style = Type.body, color = Palette.text)
                        }
                    }
                }

                row {
                    CodeStationSwitch(
                        checked = allowWebSearch,
                        onChange = { allowWebSearch = it }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Symbol("globe", size = 18.dp, tint = Color(0xFF8B5CF6))
                            Text("🌐 Recherche web", style = Type.body, color = Palette.text)
                        }
                    }
                }

                row {
                    CodeStationSwitch(
                        checked = allowCli,
                        onChange = { allowCli = it }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Symbol("terminal", size = 18.dp, tint = Color(0xFF00C49F))
                            Text("💻 Commande CLI Termux", style = Type.body, color = Palette.text)
                        }
                    }
                }

                row {
                    CodeStationSwitch(
                        checked = allowAutomation,
                        onChange = { allowAutomation = it }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Symbol("gearshape", size = 18.dp, tint = Color(0xFFFBA44A))
                            Text("⚙️ Automatisation & outils", style = Type.body, color = Palette.text)
                        }
                    }
                }
            }

            // 4. Orchestration Mode & Governance (3 Interactive Radio Selection Cards)
            CardSection(title = "Gouvernance & Sécurité") {
                row {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CamcsGovernanceMode.entries.forEach { mode ->
                            GovernanceOptionCard(
                                mode = mode,
                                isSelected = selectedGovernanceMode == mode,
                                onSelect = { selectedGovernanceMode = mode }
                            )
                        }
                    }
                }
            }

            // Bottom breathing room so last card scrolls cleanly above docked bar
            Spacer(Modifier.height(16.dp))
        }

        // 5. Docked Bottom Action Bar (Adapts to IME & System Navigation Bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (dark) Color(0xF2121622) else Color(0xF5FFFFFF)
                )
                .border(0.5.dp, Palette.glassDivider, RectangleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            PrimaryButton(
                action = {
                    if (!isLaunching && objective.isNotBlank()) {
                        isLaunching = true
                        keyboardController?.hide()
                        focusManager.clearFocus()

                        val targetBot = availableBots.firstOrNull { it.isLead } ?: availableBots.firstOrNull()
                        val trimmedObjective = objective.trim()
                        val chosenGovernance = selectedGovernanceMode

                        CoroutineScope(Dispatchers.IO).launch {
                            var createdMissionId: String? = null
                            try {
                                val client = OkHttpClient()
                                val payload = """
                                    {
                                        "title": "Mission CAMCS",
                                        "objective": ${Json.encodeToString(trimmedObjective)},
                                        "governanceMode": "${chosenGovernance.name}"
                                    }
                                """.trimIndent()
                                val req = Request.Builder()
                                    .url("http://127.0.0.1:19223/api/camcs/missions/create")
                                    .post(payload.toRequestBody("application/json".toMediaType()))
                                    .build()
                                val response = client.newCall(req).execute()
                                val responseBody = response.body?.string() ?: ""
                                try {
                                    val json = Json.parseToJsonElement(responseBody)
                                    createdMissionId = json.jsonObject["missionId"]?.jsonPrimitive?.contentOrNull
                                } catch (_: Exception) {}
                                response.close()
                            } catch (_: Exception) {}

                            withContext(Dispatchers.Main) {
                                isLaunching = false
                                onLaunchTask(trimmedObjective, createdMissionId, chosenGovernance)
                            }
                        }
                    }
                },
                enabled = objective.isNotBlank() && !isLaunching,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLaunching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Palette.onAccent,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Initialisation CAMCS...",
                            style = Type.body.semibold,
                            color = Palette.onAccent
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Symbol("play.fill", size = 16.dp, tint = Palette.onAccent)
                        Text(
                            text = "Lancer l'orchestration CAMCS",
                            style = Type.body.semibold,
                            color = Palette.onAccent
                        )
                    }
                }
            }
        }
    }
}

/**
 * Premium Glassmorphism Card for Governance Radio Selection.
 * Features:
 * - Entire card clickable with Role.RadioButton
 * - High-visibility animated radio indicator
 * - Electric green border (#00E676 / #059669) & subtle luminous gradient when selected
 * - Dark, discrete glass background when unselected
 * - Fluid animation transitions
 */
@Composable
private fun GovernanceOptionCard(
    mode: CamcsGovernanceMode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = LocalDark.current
    val electricGreen = if (dark) Color(0xFF00E676) else Color(0xFF059669)
    val electricGreenGlow = if (dark) Color(0x2800E676) else Color(0x18059669)

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 0.5.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderWidth"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) electricGreen else Palette.glassBorder,
        animationSpec = tween(durationMillis = 200),
        label = "borderColor"
    )

    val backgroundBrush = if (isSelected) {
        if (dark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x2800E676),
                    Color(0x0E00E676)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0x18059669),
                    Color(0x06059669)
                )
            )
        }
    } else {
        SolidColor(if (dark) Color(0x55171B24) else Color(0x88F0F2F5))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundBrush)
            .clickable(
                role = Role.RadioButton,
                onClick = onSelect
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radio Indicator Circle
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) electricGreen else Palette.secondary.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .background(
                        if (isSelected) electricGreenGlow else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                val dotScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f),
                    label = "radioDotScale"
                )
                if (dotScale > 0.05f) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .graphicsLayer(scaleX = dotScale, scaleY = dotScale)
                            .clip(CircleShape)
                            .background(electricGreen)
                    )
                }
            }

            // Texts: Title, Badge, Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = mode.title,
                        style = Type.subheadline.semibold,
                        color = if (isSelected) (if (dark) Color(0xFFF9FAFB) else Palette.text) else Palette.text
                    )

                    if (mode.badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) (if (dark) Color(0x3300E676) else Color(0x22059669))
                                    else Palette.surface
                                )
                                .border(
                                    0.5.dp,
                                    if (isSelected) (if (dark) Color(0x6600E676) else Color(0x44059669))
                                    else Palette.glassBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mode.badge,
                                style = Type.caption.semibold.copy(fontSize = 10.sp),
                                color = if (isSelected) electricGreen else Palette.secondary
                            )
                        }
                    }
                }

                Text(
                    text = mode.description,
                    style = Type.footnote.copy(lineHeight = 18.sp),
                    color = if (isSelected) Palette.text.copy(alpha = 0.85f) else Palette.secondary
                )
            }
        }
    }
}
