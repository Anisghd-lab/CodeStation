package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.domain.model.QuotedReply
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type

private enum class TrailingState { Call, Send, Stop }

@Composable
fun MessageComposer(
    botName: String,
    isWorking: Boolean = false,
    replyTo: QuotedReply? = null,
    onCancelReply: () -> Unit = {},
    onSend: (String) -> Unit,
    onStop: () -> Unit = {},
    onAttach: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textState = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }

    val rawText = textState.text.toString()
    val canSend = rawText.isNotBlank()
    val trailing = when {
        isWorking && !canSend -> TrailingState.Stop
        canSend -> TrailingState.Send
        else -> TrailingState.Call
    }

    val placeholder = when {
        replyTo != null -> tr("Reply…")
        isWorking -> tr("Queue a message for {name}").replace("{name}", botName)
        else -> tr("Ask {name}").replace("{name}", botName)
    }

    fun submit() {
        if (canSend) {
            val toSend = rawText.trim()
            textState.edit { replace(0, length, "") }
            onSend(toSend)
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Glass + button for attachments
        Box(
            Modifier
                .size(54.dp)
                .glass(Capsule())
                .clickable(role = Role.Button, onClick = onAttach)
                .semantics { contentDescription = "Add files" },
            contentAlignment = Alignment.Center
        ) {
            Symbol("plus", size = 26.dp, tint = Palette.text)
        }

        // Expandable input container
        Column(
            Modifier
                .weight(1f)
                .glass(RoundedRectangle(24.dp))
                .animateContentSize(Motion.layout())
        ) {
            if (replyTo != null) {
                Row(
                    Modifier.padding(start = 18.dp, end = 6.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            replyTo.authorName,
                            style = Type.caption2,
                            color = Palette.secondary
                        )
                        Text(
                            replyTo.textExcerpt,
                            style = Type.footnote,
                            color = Palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton("Cancel reply", "xmark", action = onCancelReply)
                }
            }

            Row(
                Modifier.padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sendKey = Modifier.onPreviewKeyEvent { e ->
                    val ctrlEnter = e.type == KeyEventType.KeyDown && e.key == Key.Enter && (e.isCtrlPressed || e.isMetaPressed)
                    if (ctrlEnter && canSend) {
                        submit()
                        true
                    } else false
                }

                BasicTextField(
                    state = textState,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .then(sendKey)
                        .padding(vertical = 10.dp),
                    textStyle = Type.body.copy(color = Palette.text),
                    cursorBrush = SolidColor(Palette.accent),
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 8),
                    decorator = { input ->
                        Box {
                            if (rawText.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = Type.body,
                                    color = Palette.tertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            input()
                        }
                    }
                )

                TrailingButton(
                    state = trailing,
                    canSend = canSend,
                    modifier = Modifier.padding(bottom = 4.dp),
                    onClick = {
                        when (trailing) {
                            TrailingState.Send -> submit()
                            TrailingState.Stop -> onStop()
                            TrailingState.Call -> { /* Voice call toggle */ }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TrailingButton(
    state: TrailingState,
    canSend: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val enabled = state != TrailingState.Send || canSend
    val fill by animateColorAsState(
        targetValue = when {
            state == TrailingState.Send && !canSend -> Palette.accentDim
            else -> Palette.accentFill
        },
        animationSpec = Motion.morph(),
        label = "Fill"
    )
    val ink by animateColorAsState(
        targetValue = when {
            state == TrailingState.Send && !canSend -> Palette.tertiary
            else -> Palette.onAccent
        },
        animationSpec = Motion.morph(),
        label = "Ink"
    )
    val width by animateDpAsState(
        targetValue = if (state == TrailingState.Call) 46.dp else 34.dp,
        animationSpec = Motion.morph(),
        label = "Width"
    )
    val (source, pressed) = rememberPress()

    Box(
        modifier
            .pressScale(pressed)
            .size(width, 34.dp)
            .clip(Capsule())
            .background(fill)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = when (state) { TrailingState.Stop -> "Stop"; TrailingState.Call -> "Call"; TrailingState.Send -> "Send" } },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(Motion.morph()) + scaleIn(Motion.morph(), 0.6f)) togetherWith
                    (fadeOut(Motion.morph()) + scaleOut(Motion.morph(), 0.6f))
            },
            label = "Symbol"
        ) { shown ->
            when (shown) {
                TrailingState.Stop -> Symbol("stop.fill", size = 16.dp, tint = ink)
                TrailingState.Call -> Symbol("waveform", size = 20.dp, tint = ink)
                TrailingState.Send -> Symbol("arrow.up", size = 19.dp, tint = ink)
            }
        }
    }
}
