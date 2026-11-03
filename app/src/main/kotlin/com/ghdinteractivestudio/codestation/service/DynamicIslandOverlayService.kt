package com.ghdinteractivestudio.codestation.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ghdinteractivestudio.codestation.CodeStationApplication
import com.ghdinteractivestudio.codestation.MainActivity
import com.ghdinteractivestudio.codestation.data.local.ApprovalEntity
import com.ghdinteractivestudio.codestation.domain.model.Bot
import com.ghdinteractivestudio.codestation.domain.model.BotStatus
import com.ghdinteractivestudio.codestation.domain.model.CamcsMission
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.IslandPhase
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.backgroundColor
import com.ghdinteractivestudio.codestation.ui.components.formatIslandElapsed
import com.ghdinteractivestudio.codestation.ui.components.frostedLiquidGlass
import com.ghdinteractivestudio.codestation.ui.components.tint
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationTheme
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
import com.ghdinteractivestudio.codestation.ui.theme.medium
import com.ghdinteractivestudio.codestation.ui.theme.monospaced
import com.ghdinteractivestudio.codestation.ui.theme.monospacedDigit
import com.ghdinteractivestudio.codestation.ui.theme.semibold
import com.ghdinteractivestudio.codestation.util.OverlayPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateMapOf
import com.ghdinteractivestudio.codestation.data.remote.StreamingEvent

data class CutoutMetrics(
    val cutoutLeftPx: Int,
    val statusBarHeightDp: Float,
    val pillHeightDp: Float,
    val pillTopYPx: Int,
    val marginRightPx: Int,
    val density: Float
) {
    companion object {
        val DEFAULT = CutoutMetrics(
            cutoutLeftPx = 506,
            statusBarHeightDp = 40f,
            pillHeightDp = 15f,
            pillTopYPx = 38,
            marginRightPx = 589,
            density = 3f
        )
    }
}

/**
 * Service gérant la fenêtre flottante Dynamic Island système (TYPE_APPLICATION_OVERLAY).
 * Positionnée immédiatement à gauche de la découpe physique de la caméra frontale.
 * Machine à états complète : READY, WORKING, STREAMING, COMPLETED, NEEDS_APPROVAL, ERROR, PAUSED, IDLE.
 * Réutilise fidèlement les icônes, couleurs et animations de État -> Status Bar -> Preview.
 */
class DynamicIslandOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var currentLayoutParams: WindowManager.LayoutParams? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null
    private var delayedHideJob: Job? = null
    private var wasActive = false
    private val isOverlayExpanded = androidx.compose.runtime.mutableStateOf(false)

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, DynamicIslandOverlayService::class.java)
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DynamicIslandOverlayService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startObserving()
    }

    private fun startObserving() {
        val app = application as? CodeStationApplication ?: return
        val container = app.container

        data class IslandInputs(
            val isEnabled: Boolean,
            val bots: List<Bot>,
            val approvals: List<ApprovalEntity>,
            val camcsMission: CamcsMission?
        )

        observerJob?.cancel()
        observerJob = scope.launch {
            combine(
                container.preferencesRepository.dynamicIslandOverlayEnabledStream,
                container.botRepository.getBotsStream(),
                container.approvalDao.getAllPendingApprovalsStream(),
                container.camcsRepository.activeMissionStream
            ) { isEnabled, bots, approvals, camcsMission ->
                IslandInputs(isEnabled, bots, approvals, camcsMission)
            }.collect { (isEnabled, bots, approvals, camcsMission) ->
                val hasPermission = OverlayPermissionHelper.canDrawOverlays(this@DynamicIslandOverlayService)
                val hasWorkingBot = bots.any { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }
                val hasApproval = approvals.isNotEmpty()
                val hasActiveCamcs = camcsMission != null && camcsMission.status !in listOf("CANCELLED", "FAILED")
                val currentlyActive = hasWorkingBot || hasApproval || hasActiveCamcs

                val shouldShow = isEnabled && hasPermission && (currentlyActive || wasActive)

                if (shouldShow) {
                    delayedHideJob?.cancel()
                    showOverlay()
                } else {
                    if (wasActive && !currentlyActive) {
                        // Conserver affiché 4.2s pour permettre l'animation COMPLETED "Done"
                        delayedHideJob?.cancel()
                        delayedHideJob = scope.launch {
                            delay(4200)
                            hideOverlay()
                        }
                    } else {
                        hideOverlay()
                    }
                }
                wasActive = currentlyActive
            }
        }
    }

    private fun getCutoutMetrics(): CutoutMetrics {
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        var cutoutLeftPx = screenWidth / 2 - (12 * density).toInt()
        var statusBarHeightPx = (40 * density).toInt()
        var cameraCenterYPx = statusBarHeightPx / 2

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val metrics = windowManager?.currentWindowMetrics
                val insets = metrics?.windowInsets
                val cutout = insets?.displayCutout
                val topRect = cutout?.boundingRectTop
                if (topRect != null && !topRect.isEmpty) {
                    cutoutLeftPx = topRect.left
                    if (topRect.height() > 0) {
                        cameraCenterYPx = (topRect.top + topRect.bottom) / 2
                    }
                }
                val sbInsets = insets?.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.statusBars())
                if (sbInsets != null && sbInsets.top > 0) {
                    statusBarHeightPx = sbInsets.top
                    cameraCenterYPx = statusBarHeightPx / 2
                }
            } catch (_: Exception) {}
        }

        // Hauteur ultra-compacte et proportionnée réduite de 35-40% (14-16dp)
        val statusBarHeightDp = statusBarHeightPx / density
        val pillHeightDp = (statusBarHeightDp * 0.36f).coerceIn(14f, 16f)
        val pillHeightPx = (pillHeightDp * density).toInt()

        // Décalage vertical de 0.4 mm pour alignement fin avec les icônes de notification
        val offsetMmPx = android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_MM,
            0.4f,
            resources.displayMetrics
        ).toInt()

        // Centrage vertical avec la découpe caméra physique + ajustement de 0.4 mm
        val pillTopYPx = (cameraCenterYPx - pillHeightPx / 2 + offsetMmPx).coerceAtLeast(0)

        // Positionnement immédiatement à gauche de la découpe caméra avec petit espace régulier (5dp)
        val spacingPx = (5 * density).toInt()
        val targetRightPx = (cutoutLeftPx - spacingPx).coerceAtLeast(0)
        val marginRightPx = screenWidth - targetRightPx

        return CutoutMetrics(
            cutoutLeftPx = cutoutLeftPx,
            statusBarHeightDp = statusBarHeightDp,
            pillHeightDp = pillHeightDp,
            pillTopYPx = pillTopYPx,
            marginRightPx = marginRightPx,
            density = density
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOverlayExpansion(isOverlayExpanded.value)
    }

    fun updateOverlayExpansion(expanded: Boolean) {
        val view = overlayView ?: return
        val params = currentLayoutParams ?: return
        val metrics = getCutoutMetrics()
        val density = metrics.density
        isOverlayExpanded.value = expanded

        if (expanded) {
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = 0
            params.y = metrics.pillTopYPx
            params.width = (340 * density).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            // Positionné immédiatement à gauche de la découpe caméra physique
            params.gravity = Gravity.TOP or Gravity.END
            params.x = metrics.marginRightPx
            params.y = metrics.pillTopYPx
            params.width = WindowManager.LayoutParams.WRAP_CONTENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }

        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun showOverlay() {
        if (overlayView != null) {
            // Vue déjà attachée, Compose observe les flows en continu
            return
        }

        val app = application as? CodeStationApplication ?: return
        val container = app.container
        val metrics = getCutoutMetrics()

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = metrics.marginRightPx
            y = metrics.pillTopYPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        currentLayoutParams = layoutParams

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeViewModelStoreOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)

            var downTime = 0L
            var downX = 0f
            var downY = 0f
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_OUTSIDE -> {
                        if (isOverlayExpanded.value) {
                            updateOverlayExpansion(false)
                        }
                        true
                    }
                    MotionEvent.ACTION_DOWN -> {
                        downTime = System.currentTimeMillis()
                        downX = event.rawX
                        downY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        val elapsed = System.currentTimeMillis() - downTime
                        val dx = Math.abs(event.rawX - downX)
                        val dy = Math.abs(event.rawY - downY)
                        if (elapsed < 350 && dx < 25 * metrics.density && dy < 25 * metrics.density) {
                            if (!isOverlayExpanded.value) {
                                updateOverlayExpansion(true)
                            }
                        }
                        false
                    }
                    else -> false
                }
            }

            setContent {
                val currentMetrics = remember { getCutoutMetrics() }
                CodeStationTheme(darkTheme = true) {
                    OverlayIslandContent(
                        container = container,
                        isExpandedState = isOverlayExpanded,
                        metrics = currentMetrics,
                        onOpenBot = { botId ->
                            val intent = Intent(this@DynamicIslandOverlayService, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("codestation://chat?botId=$botId")
                                putExtra("botId", botId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        },
                        onOpenCamcs = { missionId ->
                            val intent = Intent(this@DynamicIslandOverlayService, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("codestation://camcs?missionId=$missionId")
                                putExtra("missionId", missionId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        },
                        onOpenApp = {
                            val intent = Intent(this@DynamicIslandOverlayService, MainActivity::class.java).apply {
                                action = Intent.ACTION_MAIN
                                addCategory(Intent.CATEGORY_LAUNCHER)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(intent)
                        },
                        onToggleExpand = { expanded ->
                            updateOverlayExpansion(expanded)
                        }
                    )
                }
            }
        }

        try {
            windowManager?.addView(composeView, layoutParams)
            overlayView = composeView
        } catch (_: Exception) {
            // Permission révoquée ou non disponible
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onDestroy() {
        observerJob?.cancel()
        delayedHideJob?.cancel()
        scope.cancel()
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OverlayIslandContent(
    container: com.ghdinteractivestudio.codestation.AppContainer,
    isExpandedState: androidx.compose.runtime.MutableState<Boolean>,
    metrics: CutoutMetrics,
    onOpenBot: (String) -> Unit,
    onOpenCamcs: (String) -> Unit = {},
    onOpenApp: () -> Unit = {},
    onToggleExpand: (Boolean) -> Unit = {}
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val bots by container.botRepository.getBotsStream().collectAsState(initial = emptyList())
    val approvals by container.approvalDao.getAllPendingApprovalsStream().collectAsState(initial = emptyList())
    val streamingEvent by container.eventStreamClient.streamingFlow.collectAsState(initial = null)
    val camcsMission by container.camcsRepository.activeMissionStream.collectAsState(initial = null)

    val streamingBots = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(streamingEvent) {
        streamingEvent?.let { event ->
            if (event.isStreaming) {
                streamingBots[event.botId] = true
            } else {
                streamingBots.remove(event.botId)
            }
        }
    }

    val activeBots = remember(bots) {
        bots.filter { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }
    }

    val isCamcsActive = camcsMission != null && camcsMission?.status in listOf(
        "WORKING", "RUNNING", "STREAMING", "PLANNING", "DISPATCHING",
        "STARTING", "QUEUED", "WAITING_FOR_AGENT", "WAITING_FOR_APPROVAL", "VALIDATING"
    )
    val isCamcsStreaming = camcsMission?.status == "STREAMING"
    val isCamcsNeedsApproval = camcsMission?.status == "WAITING_FOR_APPROVAL"
    val isCamcsError = camcsMission?.status in listOf("FAILED", "BLOCKED")
    val isCamcsCompleted = camcsMission?.status == "COMPLETED"
    val isCamcsPaused = camcsMission?.status == "PAUSED"

    var lastCompletedBotName by remember { mutableStateOf<String?>(null) }
    var showCompletedBanner by remember { mutableStateOf(false) }
    var previousWorkingBotIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var startedAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activeBots, camcsMission?.id, camcsMission?.status) {
        val currentIds = activeBots.map { it.id }.toSet()
        if (activeBots.isNotEmpty() || isCamcsActive) {
            if (previousWorkingBotIds.isEmpty() && startedAt == 0L) {
                val oldestBot = activeBots.map { it.lastActivityAt }.filter { it > 0 }.minOrNull()
                val camcsStart = camcsMission?.let { (it.createdAt * 1000).toLong() }
                startedAt = listOfNotNull(oldestBot, camcsStart).minOrNull() ?: System.currentTimeMillis()
            }
            showCompletedBanner = false
        } else if (previousWorkingBotIds.isNotEmpty() || isCamcsCompleted) {
            val completedBot = bots.firstOrNull { it.id in previousWorkingBotIds }
            lastCompletedBotName = completedBot?.name ?: camcsMission?.title ?: "Tâche CAMCS"
            showCompletedBanner = true
            delay(4000)
            showCompletedBanner = false
            startedAt = 0L
        }
        previousWorkingBotIds = currentIds
    }

    // Machine à états unifiée bots + CAMCS avec priorités :
    // NEEDS_APPROVAL > ERROR > STREAMING > WORKING > COMPLETED > PAUSED > IDLE / READY
    val isStreamingActive = activeBots.any { streamingBots[it.id] == true } || isCamcsStreaming
    val phase = when {
        approvals.isNotEmpty() || isCamcsNeedsApproval -> IslandPhase.NEEDS_APPROVAL
        isCamcsError -> IslandPhase.ERROR
        isStreamingActive -> IslandPhase.STREAMING
        activeBots.isNotEmpty() || isCamcsActive -> IslandPhase.WORKING
        isCamcsPaused -> IslandPhase.PAUSED
        showCompletedBanner || isCamcsCompleted -> IslandPhase.COMPLETED
        bots.any { it.status == BotStatus.IDLE } -> IslandPhase.IDLE
        else -> IslandPhase.READY
    }

    val isExpanded = isExpandedState.value

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier.wrapContentSize()
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade()) },
            label = "OverlayContent"
        ) { expanded ->
            if (expanded) {
                // EXPANDED DYNAMIC ISLAND CARD
                val cardShape = RoundedRectangle(22.dp)
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .frostedLiquidGlass(
                            shape = cardShape,
                            isUser = (phase == IslandPhase.NEEDS_APPROVAL)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // En-tête : Titre / statut général + Bouton fermer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (camcsMission != null && activeBots.isEmpty()) {
                                Symbol("bolt.fill", size = 18.dp, tint = Palette.electricBlue)
                                Column {
                                    Text(camcsMission!!.title, style = Type.body.bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Mission CAMCS (${camcsMission!!.status})", style = Type.system(10f), color = Palette.electricBlue)
                                }
                            } else if (activeBots.size == 1 && camcsMission == null) {
                                val single = activeBots.first()
                                CharacterAvatar(shape = single.avatarGlyph, color = single.avatarColorHex, size = 26.dp)
                                Column {
                                    Text(single.name, style = Type.body.bold, color = Color.White)
                                    Text(
                                        if (streamingBots[single.id] == true) "Streaming en cours..." else "Exécution CLI active",
                                        style = Type.system(10f),
                                        color = if (streamingBots[single.id] == true) Palette.electricBlue else Palette.emeraldAccent
                                    )
                                }
                            } else if (activeBots.isNotEmpty() || camcsMission != null) {
                                val count = activeBots.size + (if (camcsMission != null) 1 else 0)
                                Symbol("sparkles", size = 18.dp, tint = Palette.electricBlue)
                                Text("$count Tâches & Agents mobilisés", style = Type.body.bold, color = Color.White)
                            } else if (phase == IslandPhase.NEEDS_APPROVAL) {
                                Symbol("exclamationmark.triangle.fill", size = 18.dp, tint = Palette.warning)
                                Text("${approvals.size} Action(s) requise(s)", style = Type.body.bold, color = Color.White)
                            } else if (phase == IslandPhase.COMPLETED) {
                                Symbol("checkmark.circle.fill", size = 18.dp, tint = Palette.added)
                                Text(lastCompletedBotName?.let { "$it a terminé" } ?: "Terminé", style = Type.body.bold, color = Color.White)
                            } else {
                                Symbol("sparkles", size = 18.dp, tint = Color.White)
                                Text("CodeStation", style = Type.body.bold, color = Color.White)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color.White.copy(alpha = 0.1f), Capsule())
                                .clip(Capsule())
                                .clickable {
                                    onToggleExpand(false)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Symbol("chevron.up", size = 12.dp, tint = Color.White)
                        }
                    }

                    // Section CAMCS Mission Supervision en temps réel
                    if (camcsMission != null) {
                        val m = camcsMission!!
                        val activeAgent = m.agents.firstOrNull {
                            it.status in listOf("WORKING", "RUNNING", "STREAMING", "STARTING", "VALIDATING")
                        } ?: m.agents.firstOrNull()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.06f), RoundedRectangle(12.dp))
                                .border(0.5.dp, Palette.electricBlue.copy(alpha = 0.35f), RoundedRectangle(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Palette.electricBlue.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("CAMCS", style = Type.caption2.bold, color = Palette.electricBlue)
                                    }
                                    Text(
                                        text = m.title,
                                        style = Type.caption.bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${m.progress}%",
                                    style = Type.caption2.bold,
                                    color = Palette.emeraldAccent
                                )
                            }

                            if (activeAgent != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CharacterAvatar(shape = activeAgent.shape, color = activeAgent.color, size = 22.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${activeAgent.name} (${activeAgent.role})",
                                            style = Type.caption.bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = activeAgent.currentSubtask,
                                            style = Type.caption2,
                                            color = Color.White.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (activeAgent.pid != null && activeAgent.pid > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.White.copy(alpha = 0.1f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "PID ${activeAgent.pid}",
                                                style = Type.caption2.monospaced,
                                                color = Palette.secondary
                                            )
                                        }
                                    }
                                }

                                if (!activeAgent.lastOutput.isNullOrBlank()) {
                                    Text(
                                        text = "> ${activeAgent.lastOutput.takeLast(100)}",
                                        style = Type.caption2.monospaced,
                                        color = Color(0xFF93C5FD),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Boutons d'action pour la tâche CAMCS en cours
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (m.status !in listOf("COMPLETED", "FAILED", "CANCELLED")) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(Palette.danger.copy(alpha = 0.2f), Capsule())
                                            .clip(Capsule())
                                            .clickable {
                                                coroutineScope.launch {
                                                    container.camcsRepository.cancelMission(m.id)
                                                }
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Annuler mission", style = Type.caption2.bold, color = Palette.danger)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Palette.electricBlue.copy(alpha = 0.25f), Capsule())
                                        .clip(Capsule())
                                        .clickable {
                                            onToggleExpand(false)
                                            onOpenCamcs(m.id)
                                        }
                                        .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text("Centre CAMCS", style = Type.caption2.bold, color = Palette.electricBlue)
                                }
                            }
                        }
                    }

                    // Section Approbations (Prioritaire, ne disparaît jamais sans action utilisateur)
                    if (approvals.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            approvals.forEach { approval ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Palette.warning.copy(alpha = 0.12f), RoundedRectangle(12.dp))
                                        .border(0.5.dp, Palette.warning.copy(alpha = 0.4f), RoundedRectangle(12.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${approval.botName} demande une autorisation :",
                                        style = Type.caption.bold,
                                        color = Palette.warning
                                    )
                                    Text(
                                        text = approval.commandOrPath,
                                        style = Type.caption2.monospaced,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Palette.added, Capsule())
                                                .clip(Capsule())
                                                .clickable {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        container.approvalDao.updateStatus(approval.id, "APPROVED", false)
                                                    }
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Autoriser", style = Type.caption.bold, color = Color.White)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Palette.danger.copy(alpha = 0.8f), Capsule())
                                                .clip(Capsule())
                                                .clickable {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        container.approvalDao.updateStatus(approval.id, "REJECTED", false)
                                                    }
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Refuser", style = Type.caption.bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section Multi-agents actifs (Agrégation et consultation de chaque activité)
                    if (activeBots.size > 1) {
                        Text(
                            text = "Agents en cours (${activeBots.size})",
                            style = Type.caption.medium,
                            color = Palette.secondary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeBots.forEach { bot ->
                                val isStreaming = streamingBots[bot.id] == true
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedRectangle(10.dp))
                                        .clickable {
                                            onToggleExpand(false)
                                            onOpenBot(bot.id)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CharacterAvatar(shape = bot.avatarGlyph, color = bot.avatarColorHex, size = 20.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bot.name, style = Type.caption.bold, color = Color.White)
                                        val snippet = bot.lastMessage?.take(60) ?: "En cours d'exécution..."
                                        Text(
                                            snippet,
                                            style = Type.caption2,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(Capsule())
                                            .background(if (isStreaming) Palette.electricBlue else Palette.emeraldAccent)
                                    )
                                }
                            }
                        }
                    } else if (activeBots.size == 1 && approvals.isEmpty() && camcsMission == null) {
                        val single = activeBots.first()
                        val activityText = single.lastMessage?.takeIf { it.isNotBlank() } ?: single.description.takeIf { it.isNotBlank() } ?: "Traitement CLI en cours..."
                        Text(
                            text = activityText,
                            style = Type.caption,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Bouton principal d'action
                    val targetBotId = approvals.firstOrNull()?.botId ?: activeBots.firstOrNull()?.id ?: ""
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.electricBlue, Capsule())
                            .clip(Capsule())
                            .clickable {
                                onToggleExpand(false)
                                if (camcsMission != null) {
                                    onOpenCamcs(camcsMission!!.id)
                                } else if (targetBotId.isNotEmpty()) {
                                    onOpenBot(targetBotId)
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when {
                                camcsMission != null -> "Ouvrir Centre de Contrôle CAMCS"
                                targetBotId.isNotEmpty() -> "Accéder à l'Agent"
                                else -> "Ouvrir CodeStation"
                            },
                            style = Type.caption.bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // COLLAPSED DYNAMIC ISLAND (Pill compacte immédiatement à gauche de la caméra physique)
                // Reproduction stricte de État -> Status Bar -> Preview (StateScreen.kt:StatusBarPreview)
                val pillBg = phase.backgroundColor()
                val accentColor = phase.tint()

                Row(
                    modifier = Modifier
                        .height(metrics.pillHeightDp.dp)
                        .clip(Capsule())
                        .background(pillBg)
                        .border(
                            width = 0.5.dp,
                            color = if (phase == IslandPhase.NEEDS_APPROVAL || phase == IslandPhase.ERROR || phase == IslandPhase.COMPLETED) {
                                Color.Transparent
                            } else {
                                accentColor.copy(alpha = 0.35f)
                            },
                            shape = Capsule()
                        )
                        .combinedClickable(
                            role = Role.Button,
                            onClick = {
                                if (camcsMission != null) {
                                    onOpenCamcs(camcsMission!!.id)
                                } else {
                                    onOpenApp()
                                }
                            },
                            onLongClick = {
                                onToggleExpand(true)
                            }
                        )
                        .padding(start = 5.dp, end = 7.dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Indicateur / Icône compacte réduite (-35% à -40%) conforme à StateScreen:StatusBarPreview
                    when (phase) {
                        IslandPhase.WORKING -> {
                            Box(
                                modifier = Modifier
                                    .size(4.5.dp)
                                    .scale(pulseScale)
                                    .clip(Capsule())
                                    .background(Palette.emeraldAccent)
                            )
                        }
                        IslandPhase.STREAMING -> {
                            Box(
                                modifier = Modifier
                                    .size(4.5.dp)
                                    .scale(pulseScale)
                                    .clip(Capsule())
                                    .background(Palette.electricBlue)
                            )
                        }
                        IslandPhase.COMPLETED -> {
                            Symbol("checkmark", size = 9.dp, tint = Color.White)
                        }
                        IslandPhase.NEEDS_APPROVAL -> {
                            Symbol("exclamationmark.triangle.fill", size = 9.dp, tint = Color.White)
                        }
                        IslandPhase.ERROR -> {
                            Symbol("exclamationmark.triangle.fill", size = 9.dp, tint = Color.White)
                        }
                        IslandPhase.PAUSED -> {
                            Symbol("pause.fill", size = 8.dp, tint = Color.White.copy(alpha = 0.8f))
                        }
                        IslandPhase.IDLE, IslandPhase.READY -> {
                            Symbol("sparkles", size = 9.dp, tint = Color.White)
                        }
                    }

                    // Texte / Chronomètre compact réduit (-35% à -40%)
                    Text(
                        text = when (phase) {
                            IslandPhase.WORKING, IslandPhase.STREAMING -> {
                                val activeCount = activeBots.size + (if (isCamcsActive) 1 else 0)
                                if (activeCount > 1) {
                                    "${activeCount}act · ${formatIslandElapsed(startedAt)}"
                                } else if (isCamcsActive) {
                                    val activeAgent = camcsMission?.agents?.firstOrNull { it.status in listOf("WORKING", "RUNNING", "STREAMING") }
                                    if (activeAgent != null) {
                                        "${activeAgent.name.take(5)} · ${formatIslandElapsed(startedAt)}"
                                    } else {
                                        formatIslandElapsed(startedAt)
                                    }
                                } else {
                                    formatIslandElapsed(startedAt)
                                }
                            }
                            IslandPhase.NEEDS_APPROVAL -> {
                                if (approvals.size > 1) "${approvals.size}act" else "Needs you"
                            }
                            IslandPhase.COMPLETED -> "Done"
                            IslandPhase.ERROR -> "Failed"
                            IslandPhase.PAUSED -> "Paused"
                            IslandPhase.IDLE -> "Idle"
                            IslandPhase.READY -> "Ready"
                        },
                        style = Type.system(9f).semibold.monospacedDigit,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
