package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ghdinteractivestudio.codestation.ui.theme.Capsule
import com.ghdinteractivestudio.codestation.ui.theme.LocalDark
import com.ghdinteractivestudio.codestation.ui.theme.Motion
import com.ghdinteractivestudio.codestation.ui.theme.Palette

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
            GlassStyle.Regular -> if (dark) Color(0x38222222) else Color(0xD9FFFFFF)
            GlassStyle.Clear -> if (dark) Color(0x241C1C1E) else Color(0xB3FFFFFF)
            GlassStyle.Menu -> if (dark) Color(0x521A1A1A) else Color(0xEBFAFAFA)
            GlassStyle.Card -> if (dark) Color(0x2B1A1A1A) else Color(0xE6FAFAFA)
            GlassStyle.Accent -> if (dark) Color(0x3310B981) else Color(0x26059669)
        }
    }
    val borderCol = if (dark) {
        if (style == GlassStyle.Accent) Color(0x6610B981) else Color(0x2EFFFFFF)
    } else {
        if (style == GlassStyle.Accent) Color(0x40059669) else Color(0x18000000)
    }
    return this
        .clip(shape)
        .background(if (pressed) base.copy(alpha = 0.95f) else base)
        .border(0.5.dp, borderCol, shape)
}
