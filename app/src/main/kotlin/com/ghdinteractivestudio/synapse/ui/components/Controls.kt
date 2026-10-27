package com.ghdinteractivestudio.synapse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.synapse.ui.theme.Capsule
import com.ghdinteractivestudio.synapse.ui.theme.Motion
import com.ghdinteractivestudio.synapse.ui.theme.Palette
import com.ghdinteractivestudio.synapse.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.synapse.ui.theme.Type
import com.ghdinteractivestudio.synapse.ui.theme.bold
import com.ghdinteractivestudio.synapse.ui.theme.medium
import com.ghdinteractivestudio.synapse.ui.theme.monospaced
import com.ghdinteractivestudio.synapse.ui.theme.semibold

fun tr(text: String): String = text

/** A tappable row that dims while pressed. */
@Composable
fun Modifier.plainButton(
    enabled: Boolean = true,
    role: Role = Role.Button,
    action: () -> Unit
): Modifier {
    val (source, pressed) = rememberPress()
    return clickable(source, null, enabled = enabled, role = role, onClick = action)
        .graphicsLayer { alpha = if (pressed) 0.6f else 1f }
}

/** Plain icon button that lights up on press. */
@Composable
fun IconButton(
    title: String,
    systemImage: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    action: () -> Unit
) {
    val (source, pressed) = rememberPress()
    val label = tr(title)
    Box(
        modifier
            .size(36.dp)
            .pressScale(pressed)
            .clip(RoundedRectangle(10.dp))
            .background(Palette.text.copy(alpha = if (pressed) 0.14f else 0f))
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = action)
            .semantics { contentDescription = label }
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f },
        contentAlignment = Alignment.Center
    ) {
        Symbol(systemImage, size = 21.dp, tint = if (tint.isSpecified) tint else Palette.secondary)
    }
}

@Composable
fun PrimaryButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    action: () -> Unit
) = PrimaryButton(action, modifier, enabled) {
    Text(tr(title), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun SecondaryButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    action: () -> Unit
) = SecondaryButton(action, modifier, enabled) {
    Text(tr(title), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun PrimaryButton(
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit
) = StyledButton(Palette.accentFill, Palette.onAccent, modifier, enabled, action, label)

@Composable
fun SecondaryButton(
    action: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit
) = StyledButton(Palette.bubbleUser, Palette.text, modifier, enabled, action, label)

@Composable
private fun StyledButton(
    fill: Color,
    ink: Color,
    modifier: Modifier,
    enabled: Boolean,
    action: () -> Unit,
    label: @Composable () -> Unit
) {
    val (source, pressed) = rememberPress()
    Box(
        modifier
            .pressScale(pressed)
            .graphicsLayer { alpha = if (enabled) (if (pressed) 0.85f else 1f) else 0.4f }
            .clip(Capsule())
            .background(fill)
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = action)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalTextStyle provides Type.body.medium, LocalContentColor provides ink) {
            label()
        }
    }
}

@Composable
fun LinkButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Palette.text,
    style: TextStyle = Type.body,
    action: () -> Unit
) {
    val (source, pressed) = rememberPress()
    Text(
        tr(title),
        modifier
            .clickable(source, null, enabled = enabled, role = Role.Button, onClick = action)
            .graphicsLayer { alpha = if (!enabled) 0.4f else if (pressed) 0.5f else 1f },
        style = style,
        color = color
    )
}

@Composable
fun CodyncSwitch(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable RowScope.() -> Unit
) {
    val track by animateColorAsState(if (checked) Palette.accentFill else Palette.bubbleUser, Motion.hover(), label = "Track")
    val knob by animateDpAsState(if (checked) 14.dp else 0.dp, Motion.hover(), label = "Knob")
    val knobInk = if (checked) Palette.onAccent else Color.White
    Row(
        modifier
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        label()
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(34.dp, 20.dp)
                .clip(Capsule())
                .background(track)
                .padding(2.dp)
        ) {
            Box(
                Modifier
                    .offset(x = knob)
                    .size(16.dp)
                    .clip(Capsule())
                    .background(knobInk)
            )
        }
    }
}

@Composable
fun Spinner(
    size: Dp = 14.dp,
    modifier: Modifier = Modifier,
    color: Color = Palette.secondary
) {
    val turn by rememberInfiniteTransition(label = "Spinner").animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "Turn"
    )
    val width = with(LocalDensity.current) { maxOf(1.5.dp, size / 9).toPx() }
    Canvas(
        modifier
            .size(size)
            .rotate(turn % 360f)
            .semantics { contentDescription = "Loading" }
    ) {
        drawArc(color, 0f, 252f, false, style = Stroke(width, cap = StrokeCap.Round))
    }
}

@Composable
fun <T> SegmentedChoice(
    selection: T,
    options: List<Pair<T, String>>,
    select: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(Capsule())
            .background(Palette.bubbleUser)
            .padding(3.dp)
    ) {
        val index = options.indexOfFirst { it.first == selection }.coerceAtLeast(0)
        val segment = (maxWidth - 2.dp * (options.size - 1)) / options.size
        val thumb by animateDpAsState((segment + 2.dp) * index, Motion.morph(), label = "Thumb")
        Box(
            Modifier
                .offset(x = thumb)
                .width(segment)
                .height(36.dp)
                .clip(Capsule())
                .background(Palette.background)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for ((id, label) in options) {
                val on = id == selection
                Box(
                    Modifier
                        .width(segment)
                        .height(36.dp)
                        .clip(Capsule())
                        .selectable(on, role = Role.Tab) { select(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tr(label),
                        style = Type.subheadline.medium,
                        color = if (on) Palette.text else Palette.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun <T> TabChoice(
    selection: T,
    options: List<Pair<T, String>>,
    select: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for ((id, label) in options) {
            val on = id == selection
            val fill by animateColorAsState(if (on) Palette.bubbleAgent else Color.Transparent, Motion.morph(), label = "Tab")
            Text(
                tr(label),
                Modifier
                    .clip(Capsule())
                    .background(fill)
                    .selectable(on, role = Role.Tab) { select(id) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                style = Type.subheadline,
                color = if (on) Palette.text else Palette.secondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CardForm(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Palette.background)
            .verticalScroll(rememberScrollState())
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        CompositionLocalProvider(LocalTextStyle provides Type.body, LocalContentColor provides Palette.text) {
            content()
        }
    }
}

class CardRows internal constructor() {
    internal val rows = mutableListOf<@Composable () -> Unit>()
    fun row(content: @Composable () -> Unit) { rows += content }
}

@Composable
fun CardSection(
    title: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    accessory: @Composable RowScope.() -> Unit = {},
    rows: CardRows.() -> Unit
) {
    val list = CardRows().apply(rows).rows
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Row(
                Modifier.padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(tr(title), style = Type.subheadline.bold, color = Palette.secondary)
                Spacer(Modifier.weight(1f))
                accessory()
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedRectangle(16.dp))
                .background(Palette.surface)
        ) {
            for ((index, item) in list.withIndex()) {
                if (index > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(start = 16.dp)
                            .background(Palette.border)
                    )
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    item()
                }
            }
        }
        if (footer != null) {
            Text(tr(footer), Modifier.padding(start = 4.dp), style = Type.footnote, color = Palette.tertiary)
        }
    }
}

@Composable
fun Disclosure(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            label()
            Spacer(Modifier.weight(1f))
            Symbol(if (expanded) "chevron.up" else "chevron.down", size = 12.dp, tint = Palette.secondary)
        }
        AnimatedVisibility(expanded) {
            content()
        }
    }
}

@Composable
fun CodeBox(
    code: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(10.dp))
            .background(Palette.codeBackground)
            .padding(10.dp)
    ) {
        Text(code, style = Type.footnote.monospaced, color = Palette.text)
    }
}

@Composable
fun DiffView(
    diff: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedRectangle(10.dp))
            .background(Palette.codeBackground)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for ((k, v) in diff) {
            val color = when {
                v.startsWith("+") -> Palette.added
                v.startsWith("-") -> Palette.removed
                else -> Palette.text
            }
            Text("$k: $v", style = Type.caption.monospaced, color = color)
        }
    }
}

@Composable
fun <T> ChoicePicker(
    selection: T,
    options: List<Pair<T, String>>,
    select: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SegmentedChoice(
        selection = selection,
        options = options,
        select = select,
        modifier = modifier
    )
}
