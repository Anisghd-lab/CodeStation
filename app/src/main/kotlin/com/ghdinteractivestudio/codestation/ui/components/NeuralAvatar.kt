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
 * Animated Memory Avatar for CodeStation Roster (Ultimate Holographic Animation).
 * - Exact organic C-loop / embryo silhouette extracted from Screenshot_2026-10-31-10-38-51-33.JPG.
 * - Circular dot matrix with living harmonic color morphing across all points:
 *   Emerald (#00E676) -> Cyan (#00E5FF) -> Electric Sky (#38BDF8) -> Violet (#A855F7) -> Magenta (#EC4899).
 * - Continuous perpetual rotation (turns smoothly around center + 3D lighting).
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

    // Continuous Frame Time for perpetual rotation & living color morph (1 unit = 1 second)
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

    // Vibrant Prismatic Spectrum Colors for Ultimate Animation
    val cEmerald = Color(0xFF00E676)
    val cCyan = Color(0xFF00E5FF)
    val cElectricBlue = Color(0xFF38BDF8)
    val cViolet = Color(0xFFA855F7)
    val cMagenta = Color(0xFFEC4899)
    val cSlateDark = Color(0xFF334155)

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

                    // Dynamic Harmonic Living Color Morphing across all points:
                    // Combines spatial coordinate wave + time progression for ultimate fluid gradient flow
                    val colorPhase = ((sec * 0.30 + (col * 0.08 + row * 0.06)) % 1.0 + 1.0).toFloat() % 1.0f

                    val morphColor = when {
                        colorPhase < 0.20f -> morphColor(cEmerald, cCyan, colorPhase / 0.20f)
                        colorPhase < 0.40f -> morphColor(cCyan, cElectricBlue, (colorPhase - 0.20f) / 0.20f)
                        colorPhase < 0.60f -> morphColor(cElectricBlue, cViolet, (colorPhase - 0.40f) / 0.20f)
                        colorPhase < 0.80f -> morphColor(cViolet, cMagenta, (colorPhase - 0.60f) / 0.20f)
                        else -> morphColor(cMagenta, cEmerald, (colorPhase - 0.80f) / 0.20f)
                    }

                    // Blend with deep slate on shadowed side for realistic 3D volumetric depth
                    val finalPointColor = morphColor(cSlateDark, morphColor, (shade.toFloat() * 1.15f).coerceIn(0.25f, 1.0f))

                    val radius = (step * 0.42f * (0.55f + 0.45f * shade.toFloat())).coerceIn(1.2f, step * 0.48f)
                    val alpha = (0.75f + 0.25f * shade.toFloat()).coerceIn(0.45f, 1.0f)

                    // Base circular dot (identical to bot avatars)
                    drawCircle(
                        color = finalPointColor.copy(alpha = alpha),
                        radius = radius,
                        center = center
                    )

                    // Specular highlight glint on rotating crest
                    if (shade > 0.70) {
                        val specularAlpha = ((shade - 0.70) / 0.30 * 0.85).toFloat().coerceIn(0f, 0.85f)
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

/**
 * Smooth linear interpolation between two Jetpack Compose Colors.
 */
private fun morphColor(c1: Color, c2: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = c1.red + (c2.red - c1.red) * f,
        green = c1.green + (c2.green - c1.green) * f,
        blue = c1.blue + (c2.blue - c1.blue) * f,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * f
    )
}
