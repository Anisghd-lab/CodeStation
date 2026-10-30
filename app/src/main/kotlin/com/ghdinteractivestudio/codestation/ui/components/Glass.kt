package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
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
    val wash = if (top) {
        arrayOf(
            0.0f to ground,
            0.60f to ground,
            0.85f to ground.copy(alpha = 0.7f),
            1.0f to ground.copy(alpha = 0f)
        )
    } else {
        arrayOf(
            0.0f to ground.copy(alpha = 0f),
            0.15f to ground.copy(alpha = 0.7f),
            0.40f to ground,
            1.0f to ground
        )
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(colorStops = wash))
    )
}

enum class GlassStyle { Regular, Clear, Menu, Card, Accent }

/**
 * Frosted Liquid Glassmorphism (Inspired by Social Media UI Cards Frosted Liquid Glass Effect).
 * Features:
 * - Specular gradient border (bright rim on top/corners, soft ambient on bottom)
 * - Frosted translucent fill with subtle liquid specular reflection sheen
 * - Clean curvature and clipping without cutting shadows or content
 */
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
            GlassStyle.Regular -> if (dark) Color(0x28171B24) else Color(0xD9FFFFFF)
            GlassStyle.Clear -> if (dark) Color(0x1812151C) else Color(0xB3FFFFFF)
            GlassStyle.Menu -> if (dark) Color(0xE6171B24) else Color(0xF2FFFFFF)
            GlassStyle.Card -> if (dark) Color(0x401A202C) else Color(0xEEFFFFFF)
            GlassStyle.Accent -> if (dark) Color(0x33438DFF) else Color(0x26438DFF)
        }
    }

    val rimBrush = if (dark) {
        if (style == GlassStyle.Accent) {
            Brush.verticalGradient(
                listOf(
                    Color(0xFF6BA6FF),
                    Color(0x80438DFF),
                    Color(0x33438DFF)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (style == GlassStyle.Menu) 0.55f else 0.75f),
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.10f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.Black.copy(alpha = 0.06f),
                Color.Black.copy(alpha = 0.14f)
            )
        )
    }

    val bgBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (pressed) 0.18f else 0.10f),
                base.copy(alpha = if (pressed) 0.85f else 0.55f),
                base.copy(alpha = if (pressed) 0.95f else 0.70f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                base.copy(alpha = 0.80f),
                base.copy(alpha = 0.90f)
            )
        )
    }

    return this
        .background(bgBrush, shape)
        .border(BorderStroke(1.dp, rimBrush), shape)
        .clip(shape)
}

/**
 * Direct Frosted Liquid Glass modifier for custom shapes and tints.
 */
@Composable
fun Modifier.frostedLiquidGlass(
    shape: Shape,
    tint: Color = Color.Unspecified,
    isUser: Boolean = false,
    pressed: Boolean = false
): Modifier {
    val dark = LocalDark.current
    val rimBrush = if (dark) {
        if (isUser) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color(0x66438DFF),
                    Color.White.copy(alpha = 0.15f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.70f),
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        }
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.Black.copy(alpha = 0.06f),
                Color.Black.copy(alpha = 0.14f)
            )
        )
    }

    val bgBrush = if (dark) {
        if (isUser) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color(0x38438DFF),
                    Color(0x35242A38),
                    Color(0x401E2533)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color(0x55171B24),
                    Color(0x7511151D)
                )
            )
        }
    } else {
        if (isUser) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xCCE2E8F0),
                    Color(0xD9CBD5E1)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xCCF3F4F6),
                    Color(0xD9E5E7EB)
                )
            )
        }
    }

    return this
        .background(bgBrush, shape)
        .border(BorderStroke(1.dp, rimBrush), shape)
        .clip(shape)
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
    val menuShape = RoundedRectangle(18.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = menuShape,
        containerColor = Palette.glassSurface,
        border = BorderStroke(0.5.dp, Palette.glassBorder),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .width(width)
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

/**
 * Frosted Liquid Glass AlertDialog conforming to CodeStation theme,
 * perfectly adapting to Light and Dark modes.
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedRectangle(24.dp)
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.frostedLiquidGlass(shape = shape),
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = shape,
        containerColor = Palette.glassSurface,
        titleContentColor = Palette.text,
        textContentColor = Palette.secondary,
        tonalElevation = 0.dp
    )
}
