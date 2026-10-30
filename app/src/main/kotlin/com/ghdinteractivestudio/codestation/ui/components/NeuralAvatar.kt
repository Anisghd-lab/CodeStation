package com.ghdinteractivestudio.codestation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Native Jetpack Compose Neural Avatar for CodeStation Collective Memory.
 * Faithfully matches the neural particle grid visual reference:
 * - Circular dot lattice: left side emerald & turquoise green, right side slate grey.
 * - Continuous subtle orbital oscillation and breathing luminescence.
 * - Interactive: drag-to-rotate, double-tap particle dispersion & reconstitution, single-tap onClick.
 * - Lifecycle-aware: pauses animations when in background.
 */
@Composable
fun NeuralAvatar(
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isLifecycleActive by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleActive = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Interactive Drag rotation angle
    var dragRotationAngle by remember { mutableFloatStateOf(0f) }

    // Double-tap Radial Dispersion animatable (0f = intact lattice, 1f = dispersed)
    val dispersionAnim = remember { Animatable(0f) }

    // Continuous Subtle Orbital Oscillation (-6° to +6°)
    val infiniteTransition = rememberInfiniteTransition(label = "NeuralAvatarOrbit")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbitOscillation"
    )

    // Luminescence breathing wave
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LuminescenceWave"
    )

    // Palette Colors matching visual reference
    val vibrantGreen = Color(0xFF00E676)
    val emeraldGreen = Color(0xFF10B981)
    val turquoise = Color(0xFF2DD4BF)
    val deepGreen = Color(0xFF059669)
    val slateGrey = Color(0xFF4B5563)
    val darkGrey = Color(0xFF374151)
    val mutedGrey = Color(0xFF6B7280)

    val currentTotalAngle = if (isLifecycleActive) orbitAngle + dragRotationAngle else dragRotationAngle

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap: disperse particles radially and spring back
                        coroutineScope.launch {
                            dispersionAnim.animateTo(
                                targetValue = 1.6f,
                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                            )
                            dispersionAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.55f, stiffness = 220f)
                            )
                        }
                    },
                    onTap = {
                        onClick?.invoke()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragRotationAngle = (dragRotationAngle + dragAmount.x * 0.45f) % 360f
                }
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f
            val dispersion = dispersionAnim.value

            // Draw dark circular background subtle backing
            drawCircle(
                color = Color(0xFF0B1015),
                radius = radius,
                center = center
            )

            // Outer delicate halo ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        emeraldGreen.copy(alpha = 0.18f * pulseFactor),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                radius = radius,
                center = center
            )

            // Rotate canvas for subtle orbital motion and drag interaction
            rotate(degrees = currentTotalAngle, pivot = center) {
                // Lattice generation: 8x8 grid clipped to circular boundary
                val gridSteps = 8
                val stepSize = (radius * 1.76f) / (gridSteps - 1)
                val startX = center.x - (radius * 0.88f)
                val startY = center.y - (radius * 0.88f)
                val dotBaseRadius = (size.toPx() / 48f) * 1.55f

                for (row in 0 until gridSteps) {
                    for (col in 0 until gridSteps) {
                        val basePx = startX + col * stepSize
                        val basePy = startY + row * stepSize

                        val dx = basePx - center.x
                        val dy = basePy - center.y
                        val distFromCenter = hypot(dx, dy)

                        // Circular boundary mask with slight margin
                        if (distFromCenter <= radius * 0.90f) {
                            // Apply radial dispersion if triggered
                            val dispersionOffsetMultiplier = if (dispersion > 0f) {
                                1f + dispersion * (0.8f + (distFromCenter / radius) * 0.9f)
                            } else 1f

                            val finalPx = center.x + dx * dispersionOffsetMultiplier
                            val finalPy = center.y + dy * dispersionOffsetMultiplier
                            val finalDist = hypot(finalPx - center.x, finalPy - center.y)

                            if (finalDist <= radius * 1.25f) {
                                // Column bias determines green vs grey
                                // Left side (col < 4) = Vibrant emerald / turquoise
                                // Right side (col >= 4) = Slate / dark grey
                                val normalizedCol = col.toFloat() / (gridSteps - 1) // 0.0 (left) to 1.0 (right)

                                val dotColor = when {
                                    normalizedCol < 0.28f -> vibrantGreen
                                    normalizedCol < 0.45f -> {
                                        if ((row + col) % 3 == 0) turquoise else emeraldGreen
                                    }
                                    normalizedCol < 0.58f -> {
                                        // Transition boundary
                                        if (row % 2 == 0) deepGreen else slateGrey
                                    }
                                    normalizedCol < 0.80f -> slateGrey
                                    else -> darkGrey
                                }

                                val isGreenDot = normalizedCol < 0.52f
                                val dotAlpha = if (isGreenDot) {
                                    (0.85f * pulseFactor).coerceIn(0.6f, 1f)
                                } else {
                                    0.45f
                                }

                                // Glowing halo around brighter green dots
                                if (isGreenDot && (row + col) % 2 == 0) {
                                    drawCircle(
                                        color = dotColor.copy(alpha = 0.28f * pulseFactor),
                                        radius = dotBaseRadius * 2.2f,
                                        center = Offset(finalPx, finalPy)
                                    )
                                }

                                // Core particle dot
                                drawCircle(
                                    color = dotColor.copy(alpha = dotAlpha),
                                    radius = dotBaseRadius,
                                    center = Offset(finalPx, finalPy)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
