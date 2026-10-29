package com.ghdinteractivestudio.codestation.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null

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

    private fun showOverlay(activeBot: Bot?, activeApproval: ApprovalEntity?) {
        if (overlayView != null) {
            // Vue déjà attachée, Compose se recompose automatiquement grâce aux flows
            return
        }

        val app = application as? CodeStationApplication ?: return
        val container = app.container

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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 12
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandOverlayService)
            setViewTreeViewModelStoreOwner(this@DynamicIslandOverlayService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandOverlayService)

            setContent {
                CodeStationTheme(darkTheme = true) {
                    OverlayIslandContent(
                        container = container,
                        onOpenBot = { botId ->
                            val intent = Intent(this@DynamicIslandOverlayService, MainActivity::class.java).apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("codestation://chat?botId=$botId")
                                putExtra("botId", botId)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
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
    onOpenBot: (String) -> Unit
) {
    val bots by container.botRepository.getBotsStream().collectAsState(initial = emptyList())
    val approvals by container.approvalDao.getAllPendingApprovalsStream().collectAsState(initial = emptyList())

    val activeApproval = approvals.firstOrNull()
    val activeBot = bots.firstOrNull { it.status == BotStatus.WORKING || it.status == BotStatus.BUSY }

    var isExpanded by remember { mutableStateOf(false) }

    val phase = when {
        activeApproval != null -> IslandPhase.NEEDS_APPROVAL
        activeBot != null -> IslandPhase.WORKING
        else -> IslandPhase.WORKING
    }

    val botId = activeApproval?.botId ?: activeBot?.id ?: ""
    val botName = activeApproval?.botName ?: activeBot?.name ?: "Agent CodeStation"

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 10.dp, start = 8.dp, end = 8.dp)
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = { fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade()) },
            label = "OverlayContent"
        ) { expanded ->
            if (expanded) {
                // Expanded Overlay Card
                val cardShape = RoundedRectangle(22.dp)
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .background(Color(0xF50B0D12), cardShape)
                        .border(0.5.dp, Palette.glassBorder, cardShape)
                        .clip(cardShape)
                        .padding(12.dp),
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
                                .size(24.dp)
                                .background(Color.White.copy(alpha = 0.1f), Capsule())
                                .clip(Capsule())
                                .clickable { isExpanded = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Symbol("chevron.up", size = 11.dp, tint = Color.White)
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
                        val activityText = activeBot?.lastMessage ?: activeBot?.description?.takeIf { it.isNotBlank() } ?: "Traitement en arrière-plan..."
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
                                isExpanded = false
                                if (botId.isNotEmpty()) onOpenBot(botId)
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Accéder à l'Agent", style = Type.caption.bold, color = Color.White)
                    }
                }
            } else {
                // Collapsed Overlay Pill
                val pillShape = Capsule()
                Row(
                    modifier = Modifier
                        .background(Color(0xF00B0D12), pillShape)
                        .border(0.5.dp, Palette.glassBorder, pillShape)
                        .clip(pillShape)
                        .clickable { isExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotColor = if (phase == IslandPhase.NEEDS_APPROVAL) Palette.warning else Palette.emeraldAccent
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(Capsule())
                            .background(dotColor)
                    )
                    Text(
                        text = if (phase == IslandPhase.NEEDS_APPROVAL) "Action requise" else botName,
                        style = Type.caption.semibold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
