package com.ghdinteractivestudio.codestation.service

import android.app.Service
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ghdinteractivestudio.codestation.ui.components.CharacterAvatar
import com.ghdinteractivestudio.codestation.ui.components.IslandPhase
import com.ghdinteractivestudio.codestation.ui.components.Symbol
import com.ghdinteractivestudio.codestation.ui.components.formatIslandElapsed
import com.ghdinteractivestudio.codestation.ui.components.frostedLiquidGlass
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.CodeStationTheme
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.bold
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

/**
 * Service gérant la fenêtre flottante Dynamic Island système (TYPE_APPLICATION_OVERLAY).
 * S'affiche au-dessus des autres applications (ex: Termux, navigateurs) uniquement lorsque :
 * 1. La permission SYSTEM_ALERT_WINDOW est accordée.
 * 2. L'option est activée dans les paramètres CodeStation.
 * 3. Une tâche ou approbation est active sur un agent.
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

        observerJob?.cancel()
        observerJob = scope.launch {
            combine(
                container.preferencesRepository.dynamicIslandOverlayEnabledStream,
                container.botRepository.getBotsStream(),
                container.approvalDao.getAllPendingApprovalsStream()
            ) { isEnabled, bots, approvals ->
                Triple(isEnabled, bots, approvals)
            }.collect { (isEnabled, bots, approvals) ->
                val hasPermission = OverlayPermissionHelper.canDrawOverlays(this@DynamicIslandOverlayService)
                val activeBot = bots.firstOrNull { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }
                val activeApproval = approvals.firstOrNull()

                val shouldShow = isEnabled && hasPermission && (activeBot != null || activeApproval != null)

                if (shouldShow) {
                    showOverlay(activeBot, activeApproval)
                } else {
                    hideOverlay()
                }
            }
        }
    }

    fun updateOverlayExpansion(expanded: Boolean) {
        val view = overlayView ?: return
        val params = currentLayoutParams ?: return
        val density = resources.displayMetrics.density
        isOverlayExpanded.value = expanded

        if (expanded) {
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = 0
            params.y = (10 * density).toInt()
            params.width = (340 * density).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            // Positionné dans la zone libérée à gauche des icônes Wi-Fi et réseau
            // (entre la découpe caméra x=574px et l'icône Wi-Fi x=773px)
            params.gravity = Gravity.TOP or Gravity.END
            params.x = (106 * density).toInt()
            params.y = (9 * density).toInt()
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

    private fun showOverlay(activeBot: Bot?, activeApproval: ApprovalEntity?) {
        if (overlayView != null) {
            // Vue déjà attachée, Compose se recompose automatiquement grâce aux flows
            return
        }

        val app = application as? CodeStationApplication ?: return
        val container = app.container
        val density = resources.displayMetrics.density

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
            x = (106 * density).toInt()
            y = (9 * density).toInt()
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
                        if (elapsed < 350 && dx < 25 * density && dy < 25 * density) {
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
                CodeStationTheme(darkTheme = true) {
                    OverlayIslandContent(
                        container = container,
                        isExpandedState = isOverlayExpanded,
                        onOpenBot = { botId ->
                            val intent = Intent(this@DynamicIslandOverlayService, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("codestation://chat?botId=$botId")
                                putExtra("botId", botId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            // Permission révoquée dynamiquement ou fenêtre non autorisée
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

@Composable
private fun OverlayIslandContent(
    container: com.ghdinteractivestudio.codestation.AppContainer,
    isExpandedState: androidx.compose.runtime.MutableState<Boolean>,
    onOpenBot: (String) -> Unit,
    onToggleExpand: (Boolean) -> Unit = {}
) {
    val bots by container.botRepository.getBotsStream().collectAsState(initial = emptyList())
    val approvals by container.approvalDao.getAllPendingApprovalsStream().collectAsState(initial = emptyList())

    val activeApproval = approvals.firstOrNull()
    val activeBot = bots.firstOrNull { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }

    val isExpanded = isExpandedState.value
    var startedAt by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var previousWorkingBotId by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(activeBot?.id, activeBot?.status) {
        if (activeBot != null && (activeBot.status == BotStatus.WORKING || activeBot.status == BotStatus.BUSY)) {
            if (previousWorkingBotId != activeBot.id) {
                startedAt = System.currentTimeMillis()
                previousWorkingBotId = activeBot.id
            }
        } else if (activeBot == null) {
            previousWorkingBotId = null
        }
    }

    val phase = when {
        activeApproval != null -> IslandPhase.NEEDS_APPROVAL
        activeBot != null -> IslandPhase.WORKING
        else -> IslandPhase.WORKING
    }

    val botId = activeApproval?.botId ?: activeBot?.id ?: ""
    val botName = activeApproval?.botName ?: activeBot?.name ?: "Agent CodeStation"

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
                // Expanded Overlay Card with Frosted Liquid Glass
                val cardShape = RoundedRectangle(22.dp)
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .frostedLiquidGlass(
                            shape = cardShape,
                            isUser = (phase == IslandPhase.NEEDS_APPROVAL)
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CharacterAvatar(shape = activeBot?.avatarGlyph ?: "diamond", color = "blue", size = 24.dp)
                            Text(botName, style = Type.body.bold, color = Color.White)
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

                    if (phase == IslandPhase.NEEDS_APPROVAL && activeApproval != null) {
                        Text(
                            text = "Autorisation requise : ${activeApproval.commandOrPath}",
                            style = Type.caption2.monospaced,
                            color = Palette.warning,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val activityText = activeBot?.lastMessage ?: activeBot?.description?.takeIf { it.isNotBlank() } ?: "Traitement en cours..."
                        Text(
                            text = activityText,
                            style = Type.caption,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Palette.electricBlue, Capsule())
                            .clip(Capsule())
                            .clickable {
                                onToggleExpand(false)
                                if (botId.isNotEmpty()) onOpenBot(botId)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Accéder à l'Agent", style = Type.caption.bold, color = Color.White)
                    }
                }
            } else {
                // Collapsed Overlay Pill dans la barre d'état avec les icônes devant l'horloge
                // Conforme au design compact "Preview / Simple task" (StateScreen.kt:764)
                val pillBg = when (phase) {
                    IslandPhase.NEEDS_APPROVAL -> Color(0xFF8A5A00)
                    IslandPhase.ERROR -> Color(0xFF8B251E)
                    else -> Color(0xFF2C2C2C)
                }
                Row(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(pillBg)
                        .clickable(role = Role.Button) {
                            onToggleExpand(true)
                        }
                        .padding(start = 7.dp, end = 8.dp, top = 2.5.dp, bottom = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.5.dp)
                ) {
                    Symbol("sparkles", size = 11.dp, tint = Color.White)
                    Text(
                        text = when (phase) {
                            IslandPhase.WORKING -> formatIslandElapsed(startedAt)
                            IslandPhase.NEEDS_APPROVAL -> "Needs you"
                            IslandPhase.COMPLETED -> "Done"
                            IslandPhase.ERROR -> "Failed"
                        },
                        style = Type.system(10.5f).semibold.monospacedDigit,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
