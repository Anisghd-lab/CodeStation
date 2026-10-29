package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.RoundedRectangle
import com.ghdinteractivestudio.codestation.ui.theme.Type
import com.ghdinteractivestudio.codestation.ui.theme.semibold

@Composable
fun Modifier.pressScale(pressed: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.PRESS_SCALE else 1f,
        animationSpec = Motion.press(),
        label = "Press"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun rememberPress(): Pair<MutableInteractionSource, Boolean> {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return source to pressed
}

@Composable
fun GlassStage(
    modifier: Modifier = Modifier,
    chrome: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize(), content = content)
        Box(Modifier.fillMaxSize(), content = chrome)
    }
}

enum class Edge { Top, Bottom }

@Composable
fun ScrollEdge(edge: Edge, height: Dp, modifier: Modifier = Modifier) {
    val ground = Palette.background
    val top = edge == Edge.Top
    val wash = listOf(
        ground.copy(alpha = 0.95f),
        ground.copy(alpha = 0.65f),
        ground.copy(alpha = 0f)
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(if (top) wash else wash.reversed()))
    )
}

enum class GlassStyle { Regular, Clear, Menu, Card, Accent }

@Composable
fun Modifier.glass(
    shape: Shape = Capsule(),
    style: GlassStyle = GlassStyle.Regular,
    tint: Color = Color.Unspecified,
    pressed: Boolean = false,
    shadow: Boolean = true
): Modifier {
    val dark = LocalDark.current
    val base = if (tint.isSpecified) {
        tint
    } else {
        when (style) {
            GlassStyle.Regular -> if (dark) Color(0x38171B24) else Color(0xD9FFFFFF)
            GlassStyle.Clear -> if (dark) Color(0x2412151C) else Color(0xB3FFFFFF)
            GlassStyle.Menu -> if (dark) Color(0xE6171B24) else Color(0xF2FFFFFF)
            GlassStyle.Card -> if (dark) Color(0xB3171B24) else Color(0xEEFFFFFF)
            GlassStyle.Accent -> if (dark) Color(0x33438DFF) else Color(0x26438DFF)
        }
    }
    val borderCol = if (dark) {
        if (style == GlassStyle.Accent) Color(0x66438DFF) else Color(0x33FFFFFF)
    } else {
        if (style == GlassStyle.Accent) Color(0x40438DFF) else Color(0x18000000)
    }
    return this
        .clip(shape)
        .background(if (pressed) base.copy(alpha = 0.96f) else base)
        .border(0.5.dp, borderCol, shape)
}

/**
 * Premium iOS-style Glassmorphism Popover Menu
 */
@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 250.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .width(width)
            .clip(RoundedRectangle(18.dp))
            .background(Palette.glassSurface)
            .border(0.5.dp, Palette.glassBorder, RoundedRectangle(18.dp))
            .padding(vertical = 4.dp),
        content = content
    )
}

/**
 * Premium iOS-style Popover Menu Item with alignment, typography and optional subtitle/indicator.
 */
@Composable
fun GlassMenuItem(
    title: String,
    subtitle: String? = null,
    icon: String,
    tint: Color = Palette.accent,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Symbol(
            name = icon,
            size = 20.dp,
            tint = if (destructive) Palette.danger else tint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Type.body.semibold,
                color = if (destructive) Palette.danger else Palette.text
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = Type.system(11f),
                    color = Palette.secondary
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Subtle hairline divider between menu items.
 */
@Composable
fun GlassMenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(horizontal = 14.dp)
            .background(Palette.glassDivider)
    )
}
