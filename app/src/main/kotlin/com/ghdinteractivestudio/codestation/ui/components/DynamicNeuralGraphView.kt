package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.MemoryExperience
import com.ghdinteractivestudio.codestation.domain.model.MemoryKnowledge
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class GraphNodeType {
    AGENT,
    PROJECT,
    EXPERIENCE,
    KNOWLEDGE,
    INTERNEURON
}

data class GraphNode3D(
    val id: String,
    val type: GraphNodeType,
    val label: String,
    val subtitle: String,
    // 3D coordinates in normalized organic brain space
    val normX: Float,
    val normY: Float,
    val normZ: Float,
    val color: Color,
    val baseRadius: Float,
    val isRealData: Boolean = true,
    val dataRefId: String? = null,
    val clusterId: Int = 0 // 0=Core, 1=Antigravity, 2=CodeBuddy, 3=OpenCode, 4=Codex
)

private data class GraphEdge3D(
    val sourceIndex: Int,
    val targetIndex: Int,
    val color: Color,
    val isRealRelation: Boolean = true,
    val isRamification: Boolean = false
)

private data class SynapsePulse3D(
    val edgeIndex: Int,
    val speed: Float,
    val offsetProgress: Float,
    val color: Color
)

private data class Projected3DPoint(
    val screenX: Float,
    val screenY: Float,
    val depthZ: Float,
    val perspective: Float
)

/**
 * Dense Organic 3D Neural Network with Smoked Glassmorphic Vessel for CodeStation Memory Page.
 *
 * Implements:
 * - Rich biological/computational arborization (dense center, interconnected cortical lobes, short multiple synapses).
 * - Strict viewport boundary clipping (no graphical overflow on header, stats, sync button or search).
 * - Interactive dynamic expansion with blossoming ramifications and finger manipulation.
 * - Double-tap radial dispersion and spring reconstitution.
 * - Strictly preserved data colors for Agents, Project, Experiences and Knowledge.
 */
@Composable
fun DynamicNeuralGraphView(
    experiences: List<MemoryExperience>,
    knowledge: List<MemoryKnowledge>,
    onSelectExperience: (MemoryExperience) -> Unit,
    onSelectKnowledge: (MemoryKnowledge) -> Unit,
    modifier: Modifier = Modifier,
    bots: List<Bot> = emptyList(),
    height: Dp = 230.dp,
    onNodeSelected: ((GraphNode3D?) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalDark.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLifecycleActive by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleActive = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Camera Navigation and Zoom (Strictly clamped)
    var targetPanOffset by remember { mutableStateOf(Offset.Zero) }
    var targetZoomScale by remember { mutableFloatStateOf(1.0f) }

    // Manual 3D rotation controlled by user drag
    var userRotX by remember { mutableFloatStateOf(0.10f) }
    var userRotY by remember { mutableFloatStateOf(-0.15f) }

    // Dynamic Ramification Expansion (0f = Compact Core Mesh, 1f = Fully Blossomed Dendritic Mesh)
    var isExpanded by remember { mutableStateOf(false) }
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 160f),
        label = "ExpansionProgress"
    )

    // Touch Ripple Wave Animation
    val rippleAnim = remember { Animatable(0f) }
    var rippleCenter by remember { mutableStateOf(Offset.Zero) }

    // Double-tap Radial Dispersion
    val dispersionAnim = remember { Animatable(0f) }

    // Selected node state for info preview overlay
    var selectedNode by remember { mutableStateOf<GraphNode3D?>(null) }

    val animatedPanOffset by animateOffsetAsState(
        targetValue = targetPanOffset,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "PanAnimation"
    )
    val animatedZoomScale by animateFloatAsState(
        targetValue = targetZoomScale,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "ZoomAnimation"
    )

    // Continuous Frame Time for living wave, organic drift and pulse propagation
    val infiniteTransition = rememberInfiniteTransition(label = "NeuralLoop")
    val timeParam by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI * 100).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(240000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ContinuousTime"
    )

    val breathingFactor by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NeuralBreathing"
    )

    // Strictly Preserved Real Data Colors
    val blueAgent = Color(0xFF438DFF)      // Antigravity
    val greenAgent = Color(0xFF00E676)     // CodeBuddy
    val purpleAgent = Color(0xFF9D4EDD)    // OpenCode
    val amberAgent = Color(0xFFF59E0B)     // Codex
    val projectColor = Color(0xFFEC4899)   // CodeStation Core
    val expColor = Color(0xFF10B981)       // Experiences (Emerald)
    val knwColor = Color(0xFF38BDF8)       // Knowledge (Sky Blue)

    // Dense Synaptic Palette (Cyan, Turquoise, Electric Blue, Violet, Silver)
    val cyanSynapse = Color(0xFF06B6D4)
    val turquoiseSynapse = Color(0xFF14B8A6)
    val electricBlueSynapse = Color(0xFF3B82F6)
    val violetSynapse = Color(0xFF8B5CF6)
    val silverSynapse = Color(0x60CBD5E1)

    // Build or memoize the Dense Organic 3D Neural Network strictly from real memory entities
    val (nodes, edges, pulses) = remember(experiences, knowledge, bots) {
        val nodeList = mutableListOf<GraphNode3D>()
        val edgeList = mutableListOf<GraphEdge3D>()
        val pulseList = mutableListOf<SynapsePulse3D>()

        // 1. Central Soma Nucleus (CodeStation Core)
        val prjIndex = 0
        nodeList.add(
            GraphNode3D(
                id = "prj-codestation",
                type = GraphNodeType.PROJECT,
                label = "CodeStation Core",
                subtitle = "Soma Central · Orchestrateur Neural",
                normX = 0f,
                normY = 0f,
                normZ = 0f,
                color = projectColor,
                baseRadius = 11.5f,
                dataRefId = "prj-codestation",
                clusterId = 0
            )
        )

        // 2. Core Agent Lobes (dynamic from registered/installed CLI & AI bots)
        val activeBots = if (bots.isNotEmpty()) {
            bots
        } else {
            listOf(
                Bot(id = "agent-antigravity", name = "Google Antigravity", avatarColorHex = "#4285F4"),
                Bot(id = "agent-codebuddy", name = "CodeBuddy Code", avatarColorHex = "#10B981"),
                Bot(id = "agent-opencode", name = "OpenCode GLM", avatarColorHex = "#A855F7"),
                Bot(id = "agent-codex", name = "OpenAI Codex", avatarColorHex = "#F59E0B")
            )
        }

        val numAgents = activeBots.size
        val agentIndices = mutableListOf<Int>()
        for ((idx, bot) in activeBots.withIndex()) {
            val phi = Math.acos(-1.0 + (2.0 * idx + 1.0) / numAgents).toFloat()
            val theta = (Math.sqrt(numAgents * Math.PI) * phi).toFloat()
            val radius = 0.52f
            val ax = radius * Math.cos(theta.toDouble()).toFloat() * Math.sin(phi.toDouble()).toFloat()
            val ay = radius * Math.sin(theta.toDouble()).toFloat() * Math.sin(phi.toDouble()).toFloat()
            val az = radius * Math.cos(phi.toDouble()).toFloat()

            val botColor = runCatching {
                Color(android.graphics.Color.parseColor(bot.avatarColorHex))
            }.getOrDefault(
                when (idx % 4) {
                    0 -> blueAgent
                    1 -> greenAgent
                    2 -> purpleAgent
                    else -> amberAgent
                }
            )

            val nodeIdx = nodeList.size
            nodeList.add(
                GraphNode3D(
                    id = bot.id,
                    type = GraphNodeType.AGENT,
                    label = bot.name,
                    subtitle = bot.description.ifEmpty { "Agent IA Mobilisé" },
                    normX = ax,
                    normY = ay,
                    normZ = az,
                    color = botColor,
                    baseRadius = 9.5f,
                    dataRefId = bot.id,
                    clusterId = idx + 1
                )
            )
            agentIndices.add(nodeIdx)
            // Primary axon highway connecting Core to Agent
            edgeList.add(GraphEdge3D(prjIndex, nodeIdx, botColor.copy(alpha = 0.85f), isRealRelation = true))
        }

        // Inter-agent synaptic highways (creating an interconnected brain mesh)
        for (i in 0 until agentIndices.size) {
            val next = (i + 1) % agentIndices.size
            val edgeColor = when (i % 4) {
                0 -> cyanSynapse
                1 -> electricBlueSynapse
                2 -> turquoiseSynapse
                else -> violetSynapse
            }
            edgeList.add(GraphEdge3D(agentIndices[i], agentIndices[next], edgeColor, isRealRelation = true))
        }

        // 3. Real Experiences Cluster (Dendritic Branching per Agent Domain)
        val expIndices = mutableListOf<Int>()
        for ((i, exp) in experiences.withIndex()) {
            // Find parent agent
            val parentAgentIdx = agentIndices.firstOrNull {
                val agentNode = nodeList[it]
                val cleanId = agentNode.id.removePrefix("bot-").removePrefix("agent-")
                exp.agent.contains(cleanId, ignoreCase = true) ||
                exp.agent.contains(agentNode.label, ignoreCase = true)
            } ?: agentIndices[i % agentIndices.size]

            val parentNode = nodeList[parentAgentIdx]
            // Branch organically around the parent agent in 3D
            val angle = (i * 1.618f) * 2 * PI.toFloat()
            val branchRadius = 0.22f + (i % 3) * 0.08f
            val bx = parentNode.normX + cos(angle) * branchRadius * 0.85f
            val by = parentNode.normY + sin(angle) * branchRadius * 0.75f
            val bz = parentNode.normZ + (if (i % 2 == 0) 0.12f else -0.12f)

            val nodeIdx = nodeList.size
            nodeList.add(
                GraphNode3D(
                    id = exp.id,
                    type = GraphNodeType.EXPERIENCE,
                    label = exp.title,
                    subtitle = "Expérience · ${exp.agent}",
                    normX = bx.coerceIn(-0.85f, 0.85f),
                    normY = by.coerceIn(-0.85f, 0.85f),
                    normZ = bz.coerceIn(-0.85f, 0.85f),
                    color = expColor,
                    baseRadius = 7.0f,
                    dataRefId = exp.id,
                    clusterId = parentNode.clusterId
                )
            )
            expIndices.add(nodeIdx)
            // Dendritic connection to parent agent
            edgeList.add(GraphEdge3D(parentAgentIdx, nodeIdx, expColor.copy(alpha = 0.75f), isRealRelation = true))
            // Cross-connection to Core if near center
            if (i % 2 == 0) {
                edgeList.add(GraphEdge3D(prjIndex, nodeIdx, turquoiseSynapse.copy(alpha = 0.55f), isRealRelation = true))
            }
        }

        // 4. Real Knowledge Cluster (Interconnecting Synaptic Hubs)
        val knwIndices = mutableListOf<Int>()
        for ((i, knw) in knowledge.withIndex()) {
            val angle = (i * 2.3999f) + 0.5f
            val rad = 0.45f + (i % 3) * 0.12f
            val kx = cos(angle) * rad
            val ky = (sin(angle) * rad * 0.70f) + (if (i % 2 == 0) -0.1f else 0.1f)
            val kz = sin(angle * 1.5f) * rad * 0.60f

            val nodeIdx = nodeList.size
            nodeList.add(
                GraphNode3D(
                    id = knw.id,
                    type = GraphNodeType.KNOWLEDGE,
                    label = knw.title,
                    subtitle = "Connaissance validée · v${knw.version}",
                    normX = kx.coerceIn(-0.85f, 0.85f),
                    normY = ky.coerceIn(-0.85f, 0.85f),
                    normZ = kz.coerceIn(-0.85f, 0.85f),
                    color = knwColor,
                    baseRadius = 7.0f,
                    dataRefId = knw.id,
                    clusterId = (i % 4) + 1
                )
            )
            knwIndices.add(nodeIdx)

            // Connect to related experiences
            for (relId in knw.derivedFromExperiences) {
                val matchedExpIdx = expIndices.firstOrNull { nodeList[it].id == relId }
                if (matchedExpIdx != null) {
                    edgeList.add(GraphEdge3D(matchedExpIdx, nodeIdx, knwColor.copy(alpha = 0.75f), isRealRelation = true))
                }
            }
            // Interconnection with the nearest agent
            val nearestAgent = agentIndices[i % agentIndices.size]
            edgeList.add(GraphEdge3D(nearestAgent, nodeIdx, cyanSynapse.copy(alpha = 0.65f), isRealRelation = true))
        }

        // 5. Pre-generate Continuous Synapse Pulses across real synaptic relations
        val rng = Random(1337)
        for ((idx, edge) in edgeList.withIndex()) {
            val pulseColor = when (idx % 4) {
                0 -> Color(0xFF67E8F9) // Bright Cyan
                1 -> Color(0xFF38BDF8) // Sky Blue
                2 -> Color(0xFFC084FC) // Violet
                else -> Color(0xFFFFFFFF) // Pure White
            }
            pulseList.add(
                SynapsePulse3D(
                    edgeIndex = idx,
                    speed = 0.50f + rng.nextFloat() * 0.75f,
                    offsetProgress = rng.nextFloat(),
                    color = pulseColor
                )
            )
        }

        Triple(nodeList, edgeList, pulseList)
    }

    // High performance 3D Projector with strict boundary scaling
    fun project3D(
        normX: Float,
        normY: Float,
        normZ: Float,
        rotX: Float,
        rotY: Float,
        baseRadius: Float,
        expansion: Float
    ): Projected3DPoint {
        // Organic radial blossom during expansion (ramifications branch outwards)
        val blossomFactor = 1.0f + 0.22f * expansion
        val px = normX * baseRadius * blossomFactor
        val py = normY * baseRadius * blossomFactor
        val pz = normZ * baseRadius * blossomFactor

        // Rotate Y (yaw)
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val x1 = px * cosY + pz * sinY
        val z1 = -px * sinY + pz * cosY

        // Rotate X (pitch)
        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val y2 = py * cosX - z1 * sinX
        val z2 = py * sinX + z1 * cosX

        // Perspective Camera
        val cameraDist = 700f
        val perspective = cameraDist / (cameraDist + z2).coerceAtLeast(100f)

        return Projected3DPoint(
            screenX = x1 * perspective,
            screenY = y2 * perspective,
            depthZ = z2 / (baseRadius * 2.2f).coerceAtLeast(1f),
            perspective = perspective
        )
    }

    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .clipToBounds() // Strict viewport boundary: ZERO bleed over adjacent UI elements
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                // Unified Transform Gestures: Pinch-to-zoom, Pan when zoomed, and 3D Rotation at overview
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            // Smooth pinch zoom with wide inspection range
                            targetZoomScale = (targetZoomScale * zoom).coerceIn(0.75f, 3.2f)
                        }
                        if (pan != Offset.Zero) {
                            if (targetZoomScale > 1.15f) {
                                // Pan across the graph during close-up examination
                                targetPanOffset = Offset(
                                    (targetPanOffset.x + pan.x).coerceIn(-250f, 250f),
                                    (targetPanOffset.y + pan.y).coerceIn(-180f, 180f)
                                )
                            } else {
                                // Smooth 3D sphere rotation during global overview
                                userRotY += pan.x * 0.0070f
                                userRotX = (userRotX - pan.y * 0.0070f).coerceIn(-1.2f, 1.2f)
                            }
                        }
                    }
                }
                // Tap Gestures: Single tap (focus node) & Double tap (zoom onto node or reset)
                .pointerInput(nodes, expansionProgress) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val currentCenterX = size.width / 2f
                            val currentCenterY = size.height / 2f
                            val baseRadius = min(size.width, size.height).toFloat() * 0.34f

                            val isInspecting = selectedNode != null || targetZoomScale > 1.15f
                            val t = if (isLifecycleActive && !isInspecting) timeParam else 0f
                            val autoRotY = t * 0.14f
                            val autoRotX = sin(t * 0.10f) * 0.10f
                            val curRotX = autoRotX + userRotX
                            val curRotY = autoRotY + userRotY

                            val transformedTapX = (tapOffset.x - currentCenterX - targetPanOffset.x) / targetZoomScale
                            val transformedTapY = (tapOffset.y - currentCenterY - targetPanOffset.y) / targetZoomScale

                            var closestNode: GraphNode3D? = null
                            var minDistance = Float.MAX_VALUE
                            val hitRadiusThreshold = 45f

                            for (node in nodes) {
                                val pt = project3D(
                                    node.normX, node.normY, node.normZ,
                                    curRotX, curRotY,
                                    baseRadius,
                                    expansionProgress
                                )
                                val d = hypot(pt.screenX - transformedTapX, pt.screenY - transformedTapY)
                                if (d < hitRadiusThreshold && d < minDistance) {
                                    minDistance = d
                                    closestNode = node
                                }
                            }

                            if (closestNode != null) {
                                // Double-tap on node: smoothly zoom and focus on it for examination
                                selectedNode = closestNode
                                onNodeSelected?.invoke(closestNode)
                                targetZoomScale = 2.0f
                                val pt = project3D(
                                    closestNode.normX, closestNode.normY, closestNode.normZ,
                                    curRotX, curRotY,
                                    baseRadius,
                                    expansionProgress
                                )
                                targetPanOffset = Offset(-pt.screenX * 0.5f, -pt.screenY * 0.5f)
                            } else {
                                // Double-tap empty area: smoothly reset camera to overview
                                targetZoomScale = 1.0f
                                targetPanOffset = Offset.Zero
                                userRotX = 0.10f
                                userRotY = -0.15f
                                selectedNode = null
                                onNodeSelected?.invoke(null)
                            }
                        },
                        onTap = { tapOffset ->
                            rippleCenter = tapOffset
                            coroutineScope.launch {
                                rippleAnim.snapTo(0f)
                                rippleAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
                            }

                            val currentCenterX = size.width / 2f
                            val currentCenterY = size.height / 2f
                            val baseRadius = min(size.width, size.height).toFloat() * 0.34f

                            val isInspecting = selectedNode != null || targetZoomScale > 1.15f
                            val t = if (isLifecycleActive && !isInspecting) timeParam else 0f
                            val autoRotY = t * 0.14f
                            val autoRotX = sin(t * 0.10f) * 0.10f
                            val curRotX = autoRotX + userRotX
                            val curRotY = autoRotY + userRotY

                            val transformedTapX = (tapOffset.x - currentCenterX - targetPanOffset.x) / targetZoomScale
                            val transformedTapY = (tapOffset.y - currentCenterY - targetPanOffset.y) / targetZoomScale

                            var closestNode: GraphNode3D? = null
                            var minDistance = Float.MAX_VALUE
                            val hitRadiusThreshold = 42f

                            for (node in nodes) {
                                val pt = project3D(
                                    node.normX, node.normY, node.normZ,
                                    curRotX, curRotY,
                                    baseRadius,
                                    expansionProgress
                                )
                                val d = hypot(pt.screenX - transformedTapX, pt.screenY - transformedTapY)
                                if (d < hitRadiusThreshold && d < minDistance) {
                                    minDistance = d
                                    closestNode = node
                                }
                            }

                            if (closestNode != null) {
                                selectedNode = closestNode
                                onNodeSelected?.invoke(closestNode)
                                if (targetZoomScale < 1.30f) {
                                    targetZoomScale = 1.45f
                                }
                            } else {
                                if (selectedNode != null) {
                                    selectedNode = null
                                    onNodeSelected?.invoke(null)
                                } else {
                                    isExpanded = !isExpanded
                                }
                            }
                        }
                    )
                }
        ) {
            // Strict hardware clipRect ensuring nothing can ever draw outside [0, width] x [0, height]
            clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val isInspecting = selectedNode != null || targetZoomScale > 1.15f
                val t = if (isLifecycleActive && !isInspecting) timeParam else 0f
                val dispersion = dispersionAnim.value

                val autoRotY = t * 0.14f
                val autoRotX = sin(t * 0.10f) * 0.10f
                val curRotX = autoRotX + userRotX
                val curRotY = autoRotY + userRotY

                // Base radius calibrated to comfortably fit inside 230dp with breathing room
                val baseRadius = min(size.width, size.height) * 0.34f
                val sphereRadius = baseRadius * (1.0f + 0.10f * expansionProgress)

                translate(left = center.x + animatedPanOffset.x, top = center.y + animatedPanOffset.y) {
                    scale(scale = animatedZoomScale, pivot = Offset.Zero) {
                        val sphereCenter = Offset.Zero

                        // ==========================================
                        // 1. FROSTED GLASS BACKGROUND SOFT SHADOW / CAUSTIC GLOW
                        // ==========================================
                        val shadowColors = if (isDark) {
                            listOf(
                                Color(0x22000000),
                                Color(0x0C000000),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0x1800E5FF), // Subtle cyan crystal glow
                                Color(0x1038BDF8), // Electric sky glow
                                Color(0x066366F1), // Soft indigo refraction
                                Color.Transparent
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = shadowColors,
                                center = Offset(0f, sphereRadius * 0.08f),
                                radius = sphereRadius * 1.25f
                            ),
                            radius = sphereRadius * 1.25f,
                            center = Offset(0f, sphereRadius * 0.08f)
                        )

                        // ==========================================
                        // 2. TRANSLUCENT GLASS VESSEL (LIQUID CRYSTAL IN LIGHT, SMOKED IN DARK)
                        // ==========================================
                        val glassPath = Path().apply {
                            addOval(Rect(center = sphereCenter, radius = sphereRadius))
                        }

                        // Glass Base: Translucent Charcoal & Graphite in Dark mode, Liquid Crystal in Light mode
                        val glassBaseColors = if (isDark) {
                            listOf(
                                Color(0x35F8FAFC), // Top-left frosty silver highlight
                                Color(0x2594A3B8), // Slate silver
                                Color(0x381E293B), // Charcoal graphite
                                Color(0x650F172A)  // Smoked obsidian depth
                            )
                        } else {
                            listOf(
                                Color(0x65FFFFFF), // Liquid glass top-left highlight
                                Color(0x2867E8F9), // Ethereal cyan reflection
                                Color(0x1838BDF8), // Translucent electric blue
                                Color(0x22C084FC)  // Iridescent violet refraction at rim
                            )
                        }
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = glassBaseColors,
                                start = Offset(-sphereRadius * 0.85f, -sphereRadius * 0.85f),
                                end = Offset(sphereRadius * 0.85f, sphereRadius * 0.85f)
                            ),
                            radius = sphereRadius,
                            center = sphereCenter
                        )

                        // Clipped Glass Interior Refractions
                        clipPath(glassPath) {
                            // Volumetric Core: Smoked in dark mode, Iridescent crystal depth in light mode
                            val volumetricColors = if (isDark) {
                                listOf(
                                    Color(0x28334155),
                                    Color(0x450F172A),
                                    Color(0x65020617)
                                )
                            } else {
                                listOf(
                                    Color(0x1200E5FF), // Soft cyan heart
                                    Color(0x10818CF8), // Ethereal indigo/violet
                                    Color(0x16E879F9), // Soft magenta prismatic refraction
                                    Color(0x05FFFFFF)
                                )
                            }
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = volumetricColors,
                                    center = sphereCenter,
                                    radius = sphereRadius
                                ),
                                radius = sphereRadius,
                                center = sphereCenter
                            )

                            // Iconic Diagonal Specular Sheen (Matching Glassmorphism Reference)
                            val sheenPath = Path().apply {
                                val startY = -sphereRadius * 0.15f
                                val endY = -sphereRadius * 0.50f
                                moveTo(-sphereRadius * 1.1f, startY)
                                lineTo(sphereRadius * 1.1f, endY)
                                lineTo(sphereRadius * 1.1f, -sphereRadius * 1.1f)
                                lineTo(-sphereRadius * 1.1f, -sphereRadius * 1.1f)
                                close()
                            }
                            val sheenColors = if (isDark) {
                                listOf(
                                    Color(0x28FFFFFF),
                                    Color(0x0CFFFFFF),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0x55FFFFFF),
                                    Color(0x20FFFFFF),
                                    Color.Transparent
                                )
                            }
                            drawPath(
                                path = sheenPath,
                                brush = Brush.verticalGradient(
                                    colors = sheenColors,
                                    startY = -sphereRadius,
                                    endY = 0f
                                ),
                                blendMode = BlendMode.Screen
                            )

                            // Refraction Glint with dynamic chromatic shimmer
                            val sweepOsc = sin(t * 0.22f) * sphereRadius * 0.20f
                            val glintColors = if (isDark) {
                                listOf(
                                    Color(0x20FFFFFF),
                                    Color.Transparent
                                )
                            } else {
                                listOf(
                                    Color(0x45FFFFFF),
                                    Color(0x1567E8F9),
                                    Color.Transparent
                                )
                            }
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = glintColors,
                                    center = Offset(-sphereRadius * 0.35f + sweepOsc, -sphereRadius * 0.35f),
                                    radius = sphereRadius * 0.40f
                                ),
                                radius = sphereRadius * 0.40f,
                                center = Offset(-sphereRadius * 0.35f + sweepOsc, -sphereRadius * 0.35f),
                                blendMode = BlendMode.Screen
                            )
                        }

                        // ==========================================
                        // 3. DENSE 3D NEURAL MESH & ARBORIZATIONS
                        // ==========================================
                        // Project all 3D nodes
                        val projectedNodes = Array(nodes.size) { i ->
                            val node = nodes[i]
                            // Organic micro-oscillation (neural breathing)
                            val driftX = if (isLifecycleActive) sin(t * 0.5f + i * 1.1f) * 0.024f else 0f
                            val driftY = if (isLifecycleActive) cos(t * 0.4f + i * 1.3f) * 0.024f else 0f
                            val driftZ = if (isLifecycleActive) sin(t * 0.6f + i * 0.9f) * 0.024f else 0f

                            val dispMultiplier = if (dispersion > 0f) 1f + dispersion * 1.4f else 1f

                            val nx = (node.normX + driftX) * dispMultiplier
                            val ny = (node.normY + driftY) * dispMultiplier
                            val nz = (node.normZ + driftZ) * dispMultiplier

                            project3D(nx, ny, nz, curRotX, curRotY, baseRadius, expansionProgress)
                        }

                        // A. Draw Synaptic Connection Edges (Dense short arborizations)
                        for (edge in edges) {
                            val p1 = projectedNodes[edge.sourceIndex]
                            val p2 = projectedNodes[edge.targetIndex]

                            val midZ = (p1.depthZ + p2.depthZ) / 2f
                            val depthAlpha = ((midZ + 1f) / 2f).coerceIn(0.18f, 1f)

                            // Ramification edges blossom dynamically when expanded
                            val ramifMultiplier = if (edge.isRamification) {
                                (0.35f + 0.65f * expansionProgress)
                            } else 1.0f

                            val strokeW = if (isDark) {
                                if (edge.isRealRelation) (1.2f * p1.perspective).coerceIn(0.8f, 2.2f)
                                else (0.75f * p1.perspective).coerceIn(0.5f, 1.4f)
                            } else {
                                if (edge.isRealRelation) (1.4f * p1.perspective).coerceIn(1.0f, 2.5f)
                                else (0.90f * p1.perspective).coerceIn(0.6f, 1.6f)
                            }

                            val edgeAlpha = if (isDark) {
                                if (edge.isRealRelation) {
                                    (0.50f * breathingFactor * depthAlpha).coerceIn(0.15f, 0.85f)
                                } else {
                                    (0.22f * depthAlpha * ramifMultiplier).coerceIn(0.04f, 0.45f)
                                }
                            } else {
                                if (edge.isRealRelation) {
                                    (0.70f * breathingFactor * depthAlpha).coerceIn(0.30f, 0.95f)
                                } else {
                                    (0.38f * depthAlpha * ramifMultiplier).coerceIn(0.12f, 0.65f)
                                }
                            }

                            drawLine(
                                color = edge.color.copy(alpha = edgeAlpha),
                                start = Offset(p1.screenX, p1.screenY),
                                end = Offset(p2.screenX, p2.screenY),
                                strokeWidth = strokeW
                            )
                        }

                        // B. Draw Traveling Synapse Pulses (Active Action Potentials)
                        for (pulse in pulses) {
                            val edge = edges[pulse.edgeIndex]
                            val p1 = projectedNodes[edge.sourceIndex]
                            val p2 = projectedNodes[edge.targetIndex]

                            val progress = if (isLifecycleActive) {
                                (pulse.offsetProgress + t * pulse.speed * 0.07f) % 1.0f
                            } else pulse.offsetProgress

                            val px = p1.screenX + (p2.screenX - p1.screenX) * progress
                            val py = p1.screenY + (p2.screenY - p1.screenY) * progress
                            val pulsePos = Offset(px, py)

                            val pulsePerspective = (p1.perspective + p2.perspective) / 2f
                            val pulseAlpha = if (isDark) 0.35f else 0.65f

                            drawCircle(
                                color = pulse.color.copy(alpha = pulseAlpha),
                                radius = (if (isDark) 3.2f else 3.8f) * pulsePerspective,
                                center = pulsePos
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.95f),
                                radius = (if (isDark) 1.4f else 1.8f) * pulsePerspective,
                                center = pulsePos
                            )
                        }

                        // C. Draw 3D Nodes (Radiant Synaptic Somas & Interneurons)
                        val sortedNodeIndices = nodes.indices.sortedBy { projectedNodes[it].depthZ }
                        for (i in sortedNodeIndices) {
                            val node = nodes[i]
                            val pt = projectedNodes[i]
                            val pos = Offset(pt.screenX, pt.screenY)
                            val depthScale = pt.perspective
                            val radius = node.baseRadius * depthScale

                            val isSelected = selectedNode?.id == node.id

                            if (node.isRealData) {
                                // Pulsing aura for real entities
                                val auraMultiplier = if (isSelected) 3.4f else 2.2f
                                val auraAlpha = (if (isSelected) 0.55f else if (isDark) 0.20f else 0.38f) * breathingFactor
                                drawCircle(
                                    color = node.color.copy(alpha = auraAlpha),
                                    radius = radius * auraMultiplier,
                                    center = pos
                                )

                                // Cortical Ring badge for Agents or Project Soma
                                if (node.type == GraphNodeType.AGENT || node.type == GraphNodeType.PROJECT) {
                                    drawCircle(
                                        color = node.color.copy(alpha = if (isDark) 0.75f else 0.90f),
                                        radius = radius * 1.45f,
                                        center = pos,
                                        style = Stroke(width = if (isDark) 1.3f else 1.5f)
                                    )
                                }
                            }

                            // Core node center
                            val coreAlpha = if (node.isRealData) {
                                if (isDark) 0.95f else 1.0f
                            } else {
                                if (isDark) (0.45f + (pt.depthZ + 1f) * 0.25f).coerceIn(0.18f, 0.85f)
                                else (0.60f + (pt.depthZ + 1f) * 0.30f).coerceIn(0.28f, 0.95f)
                            }

                            drawCircle(
                                color = node.color.copy(alpha = coreAlpha),
                                radius = radius,
                                center = pos
                            )

                            // Highlight spark at center of real nodes
                            if (node.isRealData) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.90f),
                                    radius = radius * 0.38f,
                                    center = pos
                                )
                            }
                        }

                        // ==========================================
                        // 4. POLISHED CRYSTAL GLASS RIM & SPECULAR BORDER
                        // ==========================================
                        val rimStrokeWidth = if (isDark) 1.6f else 1.8f
                        val rimBrush = if (isDark) {
                            Brush.sweepGradient(
                                0.00f to Color(0x35475569),
                                0.20f to Color(0x80CBD5E1),
                                0.38f to Color(0xF5FFFFFF), // Specular silver highlight
                                0.55f to Color(0x4064748B),
                                0.78f to Color(0x80E2E8F0),
                                0.88f to Color(0xFAFFFFFF), // Specular white highlight
                                1.00f to Color(0x35475569),
                                center = sphereCenter
                            )
                        } else {
                            // Prismatic iridescent crystal glass rim
                            Brush.sweepGradient(
                                0.00f to Color(0x5006B6D4), // Turquoise crystal
                                0.18f to Color(0x70818CF8), // Ethereal electric blue
                                0.35f to Color(0xFFFFFFFF), // Pure white diamond specular highlight
                                0.52f to Color(0x60EC4899), // Magenta refraction
                                0.72f to Color(0x70A855F7), // Prismatic violet
                                0.88f to Color(0xFFFFFFFF), // Pure white diamond specular highlight
                                1.00f to Color(0x5006B6D4),
                                center = sphereCenter
                            )
                        }
                        drawArc(
                            brush = rimBrush,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(-sphereRadius, -sphereRadius),
                            size = Size(sphereRadius * 2f, sphereRadius * 2f),
                            style = Stroke(width = rimStrokeWidth)
                        )

                        // Secondary Inner Specular Arch (Meniscus Highlight)
                        val meniscusBrush = if (isDark) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x50FFFFFF),
                                    Color(0x10FFFFFF),
                                    Color.Transparent
                                ),
                                startY = -sphereRadius,
                                endY = -sphereRadius * 0.3f
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x85FFFFFF),
                                    Color(0x30FFFFFF),
                                    Color.Transparent
                                ),
                                startY = -sphereRadius,
                                endY = -sphereRadius * 0.35f
                            )
                        }
                        drawArc(
                            brush = meniscusBrush,
                            startAngle = 205f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(-sphereRadius * 0.94f, -sphereRadius * 0.94f),
                            size = Size(sphereRadius * 1.88f, sphereRadius * 1.88f),
                            style = Stroke(width = 1.3f)
                        )

                        // Diamond Specular Sparkle Glints (315° and 135°)
                        fun drawDiamondSparkle(centerOffset: Offset, sparkleSize: Float) {
                            val glintColor = Color.White.copy(alpha = (if (isDark) 0.92f else 1.0f) * breathingFactor)
                            if (!isDark) {
                                // Soft iridescent cyan aura behind sparkles in light mode
                                drawCircle(
                                    color = Color(0x4500E5FF),
                                    radius = sparkleSize * 1.3f,
                                    center = centerOffset
                                )
                            }
                            drawLine(
                                color = glintColor,
                                start = centerOffset - Offset(sparkleSize, 0f),
                                end = centerOffset + Offset(sparkleSize, 0f),
                                strokeWidth = 1.2f,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = glintColor,
                                start = centerOffset - Offset(0f, sparkleSize),
                                end = centerOffset + Offset(0f, sparkleSize),
                                strokeWidth = 1.2f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.6f,
                                center = centerOffset
                            )
                        }

                        val angle1 = 315f * (PI.toFloat() / 180f)
                        val glintPos1 = Offset(cos(angle1) * sphereRadius, sin(angle1) * sphereRadius)
                        drawDiamondSparkle(glintPos1, 5.5f)

                        val angle2 = 135f * (PI.toFloat() / 180f)
                        val glintPos2 = Offset(cos(angle2) * sphereRadius, sin(angle2) * sphereRadius)
                        drawDiamondSparkle(glintPos2, 4.5f)

                        // ==========================================
                        // 5. TOUCH RIPPLE SHOCKWAVE
                        // ==========================================
                        if (rippleAnim.value > 0f) {
                            val rip = rippleAnim.value
                            val ripRadius = rip * sphereRadius * 1.30f
                            val ripAlpha = (1f - rip).coerceIn(0f, 1f)

                            drawCircle(
                                color = Color(0xFFCBD5E1).copy(alpha = ripAlpha * 0.45f),
                                radius = ripRadius,
                                center = sphereCenter,
                                style = Stroke(width = 2.2f * (1f - rip * 0.5f))
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = ripAlpha * 0.30f),
                                radius = ripRadius * 0.95f,
                                center = sphereCenter,
                                style = Stroke(width = 1.1f)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // OVERLAY CONTROLS (GLASSMORPHISM)
        // ==========================================

        // Real Metrics Badge (Top-Start) reflecting genuine memory graph data
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .glass(Capsule(), style = GlassStyle.Regular)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Palette.emeraldAccent)
            )
            Text(
                text = "${nodes.size} Nœuds · ${edges.size} Relations",
                style = Type.caption.semibold.copy(fontSize = 11.sp),
                color = Palette.text
            )
        }

        // Action Controls (Top-Right): Recenter & Expand (40dp matching Roster buttons)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Toggle Expansion Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .glass(Capsule(), style = GlassStyle.Regular)
                    .clickable(role = Role.Button) {
                        isExpanded = !isExpanded
                    },
                contentAlignment = Alignment.Center
            ) {
                Symbol(
                    name = if (isExpanded) "arrow.down.right.and.arrow.up.left" else "arrow.up.left.and.arrow.down.right",
                    size = 18.dp,
                    tint = Palette.text
                )
            }

            // Recenter Button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .glass(Capsule(), style = GlassStyle.Regular)
                    .clickable(role = Role.Button) {
                        targetPanOffset = Offset.Zero
                        targetZoomScale = 1.0f
                        userRotX = 0.10f
                        userRotY = -0.15f
                        selectedNode = null
                        onNodeSelected?.invoke(null)
                    },
                contentAlignment = Alignment.Center
            ) {
                Symbol("scope", size = 18.dp, tint = Palette.text)
            }
        }

        // Standalone Fallback Preview Card (Only displayed when onNodeSelected is not handled externally)
        if (onNodeSelected == null) {
            AnimatedVisibility(
                visible = selectedNode != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                selectedNode?.let { node ->
                    SelectedNodePreviewCard3D(
                        node = node,
                        onOpenDetail = {
                            if (node.type == GraphNodeType.EXPERIENCE) {
                                val exp = experiences.find { it.id == node.dataRefId }
                                if (exp != null) onSelectExperience(exp)
                            } else if (node.type == GraphNodeType.KNOWLEDGE) {
                                val knw = knowledge.find { it.id == node.dataRefId }
                                if (knw != null) onSelectKnowledge(knw)
                            }
                            selectedNode = null
                        },
                        onDismiss = { selectedNode = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedNodePreviewCard3D(
    node: GraphNode3D,
    onOpenDetail: () -> Unit,
    onDismiss: () -> Unit
) {
    val typeName = when (node.type) {
        GraphNodeType.AGENT -> "Agent IA"
        GraphNodeType.PROJECT -> "Projet Principal"
        GraphNodeType.EXPERIENCE -> "Expérience"
        GraphNodeType.KNOWLEDGE -> "Connaissance Validée"
        GraphNodeType.INTERNEURON -> "Interneurone"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedRectangle(16.dp), style = GlassStyle.Card)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(node.color)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = typeName,
                    style = Type.caption.semibold.copy(fontSize = 11.sp),
                    color = node.color
                )
                Text(
                    text = "• ${node.id}",
                    style = Type.caption.copy(fontSize = 11.sp),
                    color = Palette.tertiary
                )
            }
            Text(
                text = node.label,
                style = Type.body.semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = node.subtitle,
                style = Type.caption,
                color = Palette.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (node.isRealData && (node.type == GraphNodeType.EXPERIENCE || node.type == GraphNodeType.KNOWLEDGE)) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .glass(Capsule(), style = GlassStyle.Regular)
                    .clickable(role = Role.Button, onClick = onOpenDetail)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Détails",
                    style = Type.caption.semibold,
                    color = Palette.added
                )
            }
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Symbol("xmark", size = 12.dp, tint = Palette.tertiary)
        }
    }
}
