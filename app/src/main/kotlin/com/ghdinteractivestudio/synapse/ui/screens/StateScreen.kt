package com.ghdinteractivestudio.synapse.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghdinteractivestudio.synapse.ui.components.CharacterAvatar
import com.ghdinteractivestudio.synapse.ui.components.ChoicePicker
import com.ghdinteractivestudio.synapse.ui.components.CodyncSwitch
import com.ghdinteractivestudio.synapse.ui.components.IconButton
import com.ghdinteractivestudio.synapse.ui.components.LinkButton
import com.ghdinteractivestudio.synapse.ui.components.PrimaryButton
import com.ghdinteractivestudio.synapse.ui.components.ScreenHeader
import com.ghdinteractivestudio.synapse.ui.components.SegmentedChoice
import com.ghdinteractivestudio.synapse.ui.components.Symbol
import com.ghdinteractivestudio.synapse.ui.components.ThinkingOrb
import com.ghdinteractivestudio.synapse.ui.components.ThinkingOrbState
import com.ghdinteractivestudio.synapse.ui.components.plainButton
import com.ghdinteractivestudio.synapse.ui.theme.Capsule
import com.ghdinteractivestudio.synapse.ui.theme.Motion
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.medium
import com.ghdinteractivestudio.synapse.ui.theme.monospacedDigit
import com.ghdinteractivestudio.synapse.ui.theme.semibold
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class Surface { Widget, LiveUpdate, StatusBar }

enum class TaskPhase(val title: String) {
    Sending("Envoi"),
    Queued("En attente de l'ordinateur"),
    Working("Au travail"),
    NeedsInput("Action requise"),
    Completed("Terminé"),
    Failed("Erreur"),
    Stale("Mise à jour retardée");

    val live get() = this == Working || this == NeedsInput
    val mood get() = when (this) {
        Working -> "working"
        NeedsInput -> "needsInput"
        else -> "idle"
    }
}

@Composable
fun TaskPhase.tint(): Color = when (this) {
    TaskPhase.NeedsInput -> Palette.warning
    TaskPhase.Failed -> Palette.danger
    TaskPhase.Completed -> Palette.added
    TaskPhase.Working -> Palette.text
    else -> Palette.secondary
}

@Composable
fun StateScreen(
    onUsageLimitsClick: () -> Unit = {},
    bottomInset: Dp = 80.dp,
    active: Boolean = true,
    isHostConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    var surface by rememberSaveable { mutableStateOf(Surface.Widget) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.background)
            .statusBarsPadding()
    ) {
        ScreenHeader(
            title = { Text("State", style = Type.body.semibold, color = Palette.text) },
            trailing = { IconButton("Usage limits", "chart.bar", action = onUsageLimitsClick) }
        )

        SegmentedChoice(
            selection = surface,
            options = listOf(
                Surface.Widget to "Widget",
                Surface.LiveUpdate to "Live update",
                Surface.StatusBar to "Status bar"
            ),
            select = { surface = it },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        )

        AnimatedContent(
            targetState = surface,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade()) },
            label = "SurfaceContent"
        ) { shown ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = bottomInset),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (shown) {
                    Surface.Widget -> WidgetGallery(
                        isHostConnected = isHostConnected,
                        onNavigateToLiveUpdates = { surface = Surface.LiveUpdate }
                    )
                    Surface.LiveUpdate -> LiveUpdateGallery(chip = false)
                    Surface.StatusBar -> LiveUpdateGallery(chip = true)
                }
            }
        }
    }
}

@Composable
internal fun <T> rememberOnResume(read: () -> T): T {
    val owner = LocalLifecycleOwner.current
    var value by remember { mutableStateOf(read()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) value = read()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return value
}

internal fun notificationsAllowed(context: Context): Boolean =
    context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true

internal fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

@Composable
private fun WidgetGallery(
    isHostConnected: Boolean,
    onNavigateToLiveUpdates: () -> Unit
) {
    val context = LocalContext.current
    var asked by remember { mutableIntStateOf(0) }
    val allowed = rememberOnResume { notificationsAllowed(context) } || (asked > 0 && notificationsAllowed(context))
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        asked++
    }

    var kind by rememberSaveable { mutableStateOf("usage") }
    var provider by rememberSaveable { mutableStateOf("claude") }
    var sizeChoice by rememberSaveable { mutableStateOf("medium") }
    var originalIcons by rememberSaveable { mutableStateOf(false) }

    // Top Header Banner
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(22.dp))
            .background(Palette.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(RoundedRectangle(16.dp))
                .background(Palette.bubbleUser),
            contentAlignment = Alignment.Center
        ) {
            CharacterAvatar("hex", "gray", 42.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Codync, at a glance.", style = Type.title3.semibold, color = Palette.text)
            Text("Your bots and usage, on every surface.", style = Type.footnote, color = Palette.secondary)
        }
    }

    // Get set up Checklist
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SectionLabel("Get set up")
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedRectangle(18.dp))
                .background(Palette.surface)
        ) {
            SetupRow("Connect a computer", isHostConnected)
            Box(Modifier.fillMaxWidth().height(0.5.dp).padding(start = 42.dp).background(Palette.border))
            SetupRow("Add a Codync widget", false)
            Box(Modifier.fillMaxWidth().height(0.5.dp).padding(start = 42.dp).background(Palette.border))
            SetupRow(
                title = if (!allowed && asked > 0) "Turn on notifications in Settings" else "Turn on notifications",
                complete = allowed,
                action = if (allowed) null else {
                    {
                        if (Build.VERSION.SDK_INT >= 33 && asked == 0 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openNotificationSettings(context)
                        }
                    }
                }
            )
        }
        Text("Steps check themselves as you finish them.", style = Type.caption2, color = Palette.tertiary)
    }

    // Choose your widget section
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionLabel("Choose your widget") }
            Text("Sample data", style = Type.caption2, color = Palette.secondary)
        }

        ChoicePicker(
            selection = kind,
            options = listOf(
                "usage" to "Utilisation du fournisseur",
                "limits" to "Limites d'utilisation",
                "bots" to "Bots",
                "team" to "Équipe"
            ),
            select = { kind = it }
        )

        if (kind == "usage") {
            SegmentedChoice(
                selection = provider,
                options = listOf(
                    "claude" to "Claude",
                    "codex" to "Codex",
                    "agy" to "AGY"
                ),
                select = { provider = it }
            )
        }

        val sizes = listOf("small" to "Petit", "medium" to "Moyen", "large" to "Grand")
            .take(if (kind == "limits") 2 else 3)
        val currentSize = sizeChoice.takeIf { s -> sizes.any { it.first == s } } ?: "medium"
        if (kind != "team") {
            ChoicePicker(selection = currentSize, options = sizes, select = { sizeChoice = it })
        }

        // Widget Preview Container
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedRectangle(26.dp))
                .background(Palette.bubbleAgent)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isSmall = currentSize == "small" || kind == "team"
            Box(
                Modifier
                    .then(if (isSmall) Modifier.width(158.dp) else Modifier.fillMaxWidth())
                    .height(if (currentSize == "large" && kind != "team") 338.dp else 158.dp)
                    .clip(RoundedRectangle(22.dp))
                    .background(Palette.surface)
                    .padding(14.dp)
            ) {
                when (kind) {
                    "usage" -> ProviderWidgetPreview(
                        providerId = provider,
                        providerName = when (provider) {
                            "claude" -> "Claude"
                            "codex" -> "Codex"
                            else -> "AGY"
                        },
                        isMedium = currentSize != "small"
                    )
                    else -> BotsWidgetPreview(isSmall = isSmall)
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (currentSize) {
                        "small" -> "Small widget"
                        "large" -> "Large widget"
                        else -> "Medium widget"
                    },
                    style = Type.subheadline.medium,
                    color = Palette.text
                )
                Text(
                    text = when (kind) {
                        "usage" -> "Small shows the tightest limit. Medium shows two windows. Large adds a summary and up to four limits."
                        "limits" -> "Every provider's limits in one widget. The tightest leads; small shows two more and medium up to four."
                        "team" -> "Four bots as characters with a dot for each one at work; the bots that need you come first."
                        else -> "See who needs you and who's still working. Large shows up to six bots."
                    },
                    style = Type.caption,
                    color = Palette.secondary
                )
            }
        }
    }

    // Choose the look
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Choose the look")
        SegmentedChoice(
            selection = originalIcons,
            options = listOf(false to "Personnage", true to "Icone originale"),
            select = { originalIcons = it }
        )
        Text("Applies to Usage in the app and in widgets.", style = Type.caption, color = Palette.secondary)
    }

    // Keep tasks in view
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Keep tasks in view")
        Row(
            Modifier.plainButton(action = onNavigateToLiveUpdates),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Symbol("lock.rectangle", size = 18.dp, tint = Palette.accent)
            Text("Live updates on your Lock Screen", style = Type.subheadline, color = Palette.accent)
        }
    }

    // Add a widget
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Add a widget")
        PrimaryButton("Ajouter à l'écran d'accueil") { }

        // Home screen mini preview
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedRectangle(20.dp))
                .background(Palette.surface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("9:41\nSat, Oct 17", style = Type.caption.medium, color = Palette.text)
                Text("Bots", style = Type.caption.semibold, color = Palette.secondary)
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CharacterAvatar("blob", "blue", 24.dp)
                Text("Dével.. au travail", style = Type.caption, color = Palette.text)
            }
            Text("It's on your Home Screen. Touch and hold it to move it.", style = Type.caption2, color = Palette.tertiary)
        }
    }
}

@Composable
private fun ProviderWidgetPreview(
    providerId: String,
    providerName: String,
    isMedium: Boolean
) {
    val tint = when (providerId) {
        "claude" -> Color(0xFFD97757)
        "codex" -> Color(0xFF70ACDC)
        else -> Palette.accent
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            CharacterAvatar(
                shape = if (providerId == "codex") "hex" else "blob",
                color = "gray",
                size = 22.dp,
                highlight = tint
            )
            Text(
                "$providerName Utilisation",
                Modifier.weight(1f),
                style = Type.system(13f).semibold,
                color = Palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Limit 1: Session
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Session", Modifier.weight(1f), style = Type.system(11f).medium, color = Palette.secondary)
                Text("16%", style = Type.system(13f).semibold.monospacedDigit, color = Palette.text)
            }
            UsageTicks(percent = 16.0, tint = tint, height = 12.dp)
            Text(
                "réinitialisation dans 3h 20m",
                Modifier.fillMaxWidth(),
                style = Type.system(9f),
                color = Palette.secondary,
                textAlign = TextAlign.End
            )
        }

        if (isMedium) {
            // Limit 2: Hebdomadaire
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hebdomadaire", Modifier.weight(1f), style = Type.system(11f).medium, color = Palette.secondary)
                    Text("59%", style = Type.system(13f).semibold.monospacedDigit, color = Palette.text)
                }
                UsageTicks(percent = 59.0, tint = tint, height = 12.dp)
                Text(
                    "réinitialisation dans 2d 6h",
                    Modifier.fillMaxWidth(),
                    style = Type.system(9f),
                    color = Palette.secondary,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun BotsWidgetPreview(isSmall: Boolean) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Symbol("bubble.left.and.bubble.right.fill", size = 16.dp, tint = Palette.secondary)
            Text("Bots", style = Type.subheadline.semibold, color = Palette.text)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterAvatar("blob", "green", 34.dp, "working")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Lead Agent", style = Type.caption.semibold, color = Palette.text)
                Text("Au travail…", style = Type.caption2, color = Palette.secondary)
            }
        }
    }
}

@Composable
fun UsageTicks(percent: Double, tint: Color, height: Dp) {
    val value = if (percent.isFinite()) percent.coerceIn(0.0, 100.0) else 0.0
    val color = if (value >= 90) Palette.danger else tint
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val gap = 1.5.dp.toPx()
        val count = maxOf(1, (size.width / 4.dp.toPx()).toInt())
        val filled = (count * value / 100).roundToInt()
        val width = (size.width - gap * (count - 1)) / count
        for (i in 0 until count) {
            drawRoundRect(
                if (i < filled) color else color.copy(alpha = 0.16f),
                Offset(i * (width + gap), 0f),
                Size(width, size.height),
                CornerRadius(1.dp.toPx())
            )
        }
    }
}

@Composable
private fun LiveUpdateGallery(chip: Boolean) {
    val context = LocalContext.current
    val notifications = rememberOnResume { notificationsAllowed(context) }
    var taskEnabled by rememberSaveable { mutableStateOf(true) }
    var phase by rememberSaveable { mutableStateOf(TaskPhase.Working) }
    val started = remember { System.currentTimeMillis() - 157_000 }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(20.dp))
            .background(Palette.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CodyncSwitch(
            checked = taskEnabled,
            onChange = { taskEnabled = it }
        ) {
            Text(
                if (chip) "Show in the status bar" else "Show Live updates",
                style = Type.subheadline.medium,
                color = Palette.text
            )
        }

        Text(
            if (chip) "The status bar chip shows the same Live update as the Lock Screen, so this switch turns off both."
            else "Starts when you send a task from this phone. Follow its progress, then open the conversation when your bot needs you.",
            style = Type.footnote,
            color = Palette.secondary
        )

        if (!notifications) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Notifications are turned off in Android settings.", style = Type.footnote, color = Palette.secondary)
                LinkButton("Ouvrir les paramètres Android", style = Type.footnote.medium, color = Palette.accent) {
                    openNotificationSettings(context)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Preview", Modifier.weight(1f), style = Type.subheadline.medium, color = Palette.text)
            Text("Sample task", style = Type.caption, color = Palette.secondary)
        }

        ChoicePicker(
            selection = phase,
            options = listOf(
                TaskPhase.Working to "Au travail",
                TaskPhase.NeedsInput to "Action requise",
                TaskPhase.Completed to "Terminé",
                TaskPhase.Failed to "Erreur"
            ),
            select = { phase = it }
        )

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 90.dp)
                .clip(RoundedRectangle(28.dp))
                .background(Palette.bubbleAgent)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (chip) StatusBarPreview(phase, started) else LiveUpdatePreview(phase, started)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (chip) {
            Explanation("Chip", "The bot and its running time beside the clock, in the state's color. It becomes a check or a warning when the task ends.")
            Explanation("Tap", "Tap the chip to see the task's Live update; tap that to open the conversation.")
        } else {
            Explanation(
                "Lock Screen and notifications",
                "The bot in its mood, its current step in the state's color, the running time and progress. When the bot needs you, Review opens the request."
            )
        }
    }

    Text(
        "Android chooses how Live updates appear based on your device and other tasks. These previews don't start one. Approvals are handled inside Codync.",
        style = Type.footnote,
        color = Palette.secondary
    )
}

@Composable
private fun LiveUpdatePreview(phase: TaskPhase, started: Long) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(22.dp))
            .background(Palette.surface)
            .padding(14.dp)
            .animateContentSize(Motion.layoutSize),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(18.dp).clip(Capsule()).background(Palette.accentFill), contentAlignment = Alignment.Center) {
                Symbol("sparkles", size = 11.dp, tint = Palette.onAccent)
            }
            Text("Codync · now", style = Type.caption.medium, color = Palette.secondary)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CharacterAvatar("blob", "green", 40.dp, phase.mood)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Relecteur", style = Type.system(14f).semibold, color = Palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val caption = if (phase.live) {
                        if (phase == TaskPhase.NeedsInput) "Review the proposed changes." else "Running the test suite."
                    } else phase.title
                    Text(
                        caption,
                        Modifier.weight(1f, fill = false),
                        style = Type.system(12f),
                        color = if (phase == TaskPhase.Working) Palette.secondary else phase.tint(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (phase.live) {
                        Text("·", style = Type.system(12f), color = Palette.tertiary)
                        Text(formatElapsed(started), style = Type.system(12f).monospacedDigit, color = Palette.secondary)
                    }
                }
            }
            PhaseMark(phase, 20.dp)
        }

        ProgressLine(phase)

        AnimatedVisibility(
            visible = phase == TaskPhase.NeedsInput,
            enter = fadeIn(Motion.fade()) + slideInVertically { it / 2 },
            exit = fadeOut(Motion.fade())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(Capsule())
                    .background(phase.tint()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Symbol("hand.raised.fill", size = 15.dp, tint = Palette.onAccent)
                Spacer(Modifier.width(6.dp))
                Text("Review", style = Type.system(13f).semibold, color = Palette.onAccent)
            }
        }
    }
}

@Composable
private fun ProgressLine(phase: TaskPhase) {
    val tint = phase.tint()
    val filled = when (phase) {
        TaskPhase.Sending, TaskPhase.Queued -> 1
        TaskPhase.Working, TaskPhase.Stale -> 2
        TaskPhase.NeedsInput -> 3
        else -> 4
    }
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val gap = 3.dp.toPx()
        val width = (size.width - gap * 3) / 4
        for (i in 0 until 4) {
            drawRoundRect(
                if (i < filled) tint else tint.copy(alpha = 0.16f),
                Offset(i * (width + gap), 0f),
                Size(width, size.height),
                CornerRadius(size.height / 2)
            )
        }
    }
}

@Composable
private fun StatusBarPreview(phase: TaskPhase, started: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(18.dp))
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41", style = Type.system(14f).semibold.monospacedDigit, color = Color.White)
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(Capsule())
                .background(if (phase == TaskPhase.NeedsInput || phase == TaskPhase.Failed) phase.tint() else Color(0xFF2C2C2C))
                .padding(start = 7.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                .animateContentSize(Motion.layoutSize),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Symbol("sparkles", size = 13.dp, tint = Color.White)
            Text(
                when {
                    phase.live && phase != TaskPhase.NeedsInput -> formatElapsed(started)
                    phase == TaskPhase.NeedsInput -> "Needs you"
                    phase == TaskPhase.Completed -> "Done"
                    phase == TaskPhase.Failed -> "Failed"
                    else -> phase.title
                },
                style = Type.system(12f).semibold.monospacedDigit,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Symbol("wifi", size = 15.dp, tint = Color.White)
            Symbol("battery.75percent", size = 18.dp, tint = Color.White)
        }
    }
}

@Composable
private fun formatElapsed(started: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(started) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val seconds = (now - started) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun PhaseMark(phase: TaskPhase, size: Dp) {
    when (phase) {
        TaskPhase.Completed -> Symbol("checkmark", size = size, tint = phase.tint())
        TaskPhase.Failed -> Symbol("exclamationmark.triangle.fill", size = size, tint = phase.tint())
        TaskPhase.Queued -> Symbol("desktopcomputer", size = size, tint = phase.tint())
        else -> ThinkingOrb(
            state = when (phase) {
                TaskPhase.Working -> ThinkingOrbState.Working
                TaskPhase.NeedsInput -> ThinkingOrbState.Listening
                else -> ThinkingOrbState.Connecting
            },
            size = size,
            color = phase.tint()
        )
    }
}

@Composable
private fun SectionLabel(title: String) =
    Text(title, Modifier.semantics { heading() }, style = Type.caption.medium, color = Palette.secondary)

@Composable
private fun SetupRow(title: String, complete: Boolean, action: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (action != null) Modifier.clickable(role = Role.Button, onClick = action) else Modifier)
            .semantics(mergeDescendants = true) {
                stateDescription = if (complete) "Complete" else "Not complete"
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Symbol(
            if (complete) "checkmark.circle.fill" else "circle",
            size = 18.dp,
            tint = if (complete) Palette.added else Palette.tertiary
        )
        Text(
            title,
            Modifier.weight(1f),
            style = Type.footnote,
            color = if (complete) Palette.secondary else Palette.text
        )
    }
}

@Composable
private fun Explanation(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = Type.subheadline.medium, color = Palette.text)
        Text(detail, style = Type.footnote, color = Palette.secondary)
    }
}
