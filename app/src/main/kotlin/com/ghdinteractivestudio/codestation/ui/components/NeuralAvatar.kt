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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Animated Memory Avatar for CodeStation Roster.
 * - Organic C-loop / embryo shape extracted directly from Screenshot_2026-10-31-10-38-51-33.JPG.
 * - Uses the EXACT same dot matrix pattern and circular points as agent avatars.
 * - Continuous perpetual rotation animation (turns perpetually around its center + 3D lighting).
 * - Emerald Green (#00E676), Turquoise Cyan (#00BCA6), Slate Grey (#4B5563) & Graphite palette.
 * - Single tap opens Memory; double tap triggers dispersion and spring reconstitution.
 */
@Composable
fun NeuralAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current

    var active by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // Continuous Frame Time for perpetual rotation (1 unit = 1 second)
    val transition = rememberInfiniteTransition(label = "MemoryAvatarPerpetualRotation")
    val elapsedSeconds by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_600_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MemoryAvatarSeconds"
    )

    // Double-tap dispersion animation
    val dispersionAnim = remember { Animatable(0f) }

    val cells = if (size < 18.dp) 7 else if (size < 28.dp) 9 else 13
    val dots = remember(cells) { avatarDots("seed", cells) }

    // Authentic Emerald & Slate Palette
    val emeraldGreen = Color(0xFF00E676)
    val turquoiseCyan = Color(0xFF00BCA6)
    val slateGrey = Color(0xFF4B5563)
    val darkGrey = Color(0xFF374151)

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        coroutineScope.launch {
                            dispersionAnim.animateTo(1.6f, tween(220, easing = FastOutSlowInEasing))
                            dispersionAnim.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 220f))
                        }
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val step = this.size.width / cells
            val half = this.size.width / 2f
            val sec = if (active) elapsedSeconds.toDouble() else 0.0
            val dispersion = dispersionAnim.value

            // Perpetual 2D rotation of the organic avatar (smooth 360° turn every ~9 seconds)
            val rotDegrees = (sec * 40.0 % 360.0).toFloat()

            // 3D spherical lighting rotating with orbital light vector
            val yaw = sec * 1.5
            val lx = sin(yaw) * 0.85
            val ly = 0.55
            val lz = cos(yaw) * 0.65 + 0.55
            val ll = sqrt(lx * lx + ly * ly + lz * lz)

            rotate(degrees = rotDegrees, pivot = Offset(half, half)) {
                for ((row, col) in dots) {
                    val baseCenterX = (col + 0.5f) * step
                    val baseCenterY = (row + 0.5f) * step

                    // Radial dispersion offset for double-tap
                    val dx = baseCenterX - half
                    val dy = baseCenterY - half
                    val dist = hypot(dx, dy)
                    val dirX = if (dist > 0.001f) dx / dist else 0f
                    val dirY = if (dist > 0.001f) dy / dist else 0f

                    val curCenterX = baseCenterX + dirX * (dispersion * half * 0.9f)
                    val curCenterY = baseCenterY + dirY * (dispersion * half * 0.9f)
                    val center = Offset(curCenterX, curCenterY)

                    // Normalized position (-1.0 to +1.0)
                    val u = ((baseCenterX - half) / half).toDouble()
                    val v = ((half - baseCenterY) / half).toDouble()
                    val z = sqrt(max(0.2, 1.0 - u * u - v * v))
                    val nl = sqrt(u * u + v * v + z * z)

                    // 3D Spherical shade rotating perpetually
                    val dotProd = u * lx + v * ly + z * lz
                    val shade = (0.30 + 0.70 * max(0.0, dotProd / (nl * ll))).coerceIn(0.2, 1.0)

                    // Diagonal gradient distribution across the shape
                    val diagPos = ((col + row).toFloat() / (cells * 2f))
                    val isLuminousRegion = diagPos < 0.60f
                    val baseColor = when {
                        diagPos < 0.30f -> emeraldGreen
                        diagPos < 0.55f -> turquoiseCyan
                        diagPos < 0.75f -> slateGrey
                        else -> darkGrey
                    }

                    val radius = (step * 0.42f * (0.55f + 0.45f * shade.toFloat())).coerceIn(1.2f, step * 0.48f)
                    val alpha = if (isLuminousRegion) {
                        (0.65f + 0.35f * shade.toFloat()).coerceIn(0.4f, 1.0f)
                    } else {
                        (0.35f + 0.25f * shade.toFloat()).coerceIn(0.2f, 0.65f)
                    }

                    // Base circular dot (identical to bot avatars)
                    drawCircle(
                        color = baseColor.copy(alpha = alpha),
                        radius = radius,
                        center = center
                    )

                    // Specular highlight glint on rotating crest
                    if (shade > 0.72) {
                        val specularAlpha = ((shade - 0.72) / 0.28 * 0.75).toFloat().coerceIn(0f, 0.75f)
                        drawCircle(
                            color = Color.White.copy(alpha = specularAlpha),
                            radius = radius * 0.45f,
                            center = center
                        )
                    }
                }
            }
        }
    }
}
