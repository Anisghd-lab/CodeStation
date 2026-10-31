package com.ghdinteractivestudio.codestation.ui.components

import android.animation.ValueAnimator
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Region
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ghdinteractivestudio.codestation.ui.theme.Palette
import com.ghdinteractivestudio.codestation.ui.theme.Type
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object AvatarPalette {
    val colors = linkedMapOf(
        "black" to 0xFF2B2B2B,
        "brown" to 0xFF936439,
        "red" to 0xFFFF263C,
        "orange" to 0xFFFF6700,
        "yellow" to 0xFFFF9800,
        "green" to 0xFF00C972,
        "cyan" to 0xFF00BCA6,
        "blue" to 0xFF1084FE,
        "violet" to 0xFF9159FE,
        "magenta" to 0xFFFF309B,
        "gray" to 0xFF777777
    )
    val shapes = listOf("blob", "pebble", "squircle", "tablet", "wedge", "hex", "cloud", "teardrop")

    fun tint(id: String): Color = Color(colors[id.lowercase()] ?: colors.getValue("blue"))
}

data class AvatarFace(val shape: String, val color: String, val status: String = "idle")

/**
 * Animated dotted character avatar with 3D sphere lighting, glancing and blinking eyes.
 */
@Composable
fun CharacterAvatar(
    shape: String,
    color: String,
    size: Dp = 40.dp,
    status: String = "idle",
    monochrome: Color? = null,
    highlight: Color? = null,
    modifier: Modifier = Modifier
) {
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val time = if (active && status in listOf("working", "needsInput", "needs_input") && ValueAnimator.areAnimatorsEnabled()) {
        val transition = rememberInfiniteTransition(label = "Character activity")
        val seconds by transition.animateFloat(
            initialValue = 0f,
            targetValue = 3600f,
            animationSpec = infiniteRepeatable(tween(3_600_000, easing = LinearEasing)),
            label = "Character light and eyes"
        )
        seconds.toDouble()
    } else 0.0

    val cells = if (size < 18.dp) 7 else if (size < 28.dp) 9 else 13
    val dots = remember(shape, cells) { avatarDots(shape, cells) }
    val ink = monochrome ?: Palette.text
    val tint = monochrome ?: highlight ?: AvatarPalette.tint(color)

    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val step = this.size.width / cells
        val half = this.size.width / 2
        val glance = if ((status == "working") && time > 0) (sin(time * 2 * PI / 3.2) * 1.4).roundToInt() else 0
        val eyes = eyeColumns(cells).map { it + glance }
        val eyeRows = eyeRows(cells)
        val blinking = time > 0 && (time / 4.7) % 1 < 0.035
        val yaw = if ((status == "working") && time > 0) time * 1.4 else -0.7
        val lx = sin(yaw) * .8
        val ly = .55
        val lz = cos(yaw) * .5 + .6
        val ll = sqrt(lx * lx + ly * ly + lz * lz)

        for ((row, col) in dots) {
            if (col in eyes && row in if (blinking) eyeRows.takeLast(1) else eyeRows) continue
            val center = Offset((col + .5f) * step, (row + .5f) * step)
            val u = (center.x - half) / half
            val v = (half - center.y) / half
            val z = sqrt(max(.2, 1.0 - u * u - v * v))
            val nl = sqrt(u * u + v * v + z * z)
            var shade = .3 + .7 * max(0.0, (u * lx + v * ly + z * lz) / (nl * ll))
            if (status in listOf("needsInput", "needs_input")) {
                shade *= .6 + .4 * (.5 + .5 * sin(sqrt((u * u + v * v).toDouble()) * 9 - time * 5))
            }
            val radius = (step * .42 * (.55 + .45 * shade)).toFloat()
            drawCircle(ink.copy(alpha = dotInk(cells, shade).toFloat()), radius, center)
            if (shade > .6) {
                drawCircle(tint.copy(alpha = ((shade - .6) / .4).toFloat().coerceIn(0f, 1f)), radius, center)
            }
        }
    }
}

@Composable
fun GroupAvatar(faces: List<AvatarFace>, size: Dp = 40.dp) {
    Box(Modifier.size(size).clearAndSetSemantics { }) {
        fun side(fraction: Float) = size * fraction

        @Composable
        fun place(index: Int, fraction: Float, align: Alignment) {
            val face = faces[index]
            Box(Modifier.align(align)) { CharacterAvatar(face.shape, face.color, side(fraction), face.status) }
        }

        when (faces.size) {
            0 -> Symbol("person.2", Modifier.align(Alignment.Center), size = size * 0.4f, tint = Palette.secondary)
            1 -> place(0, 1f, Alignment.Center)
            2 -> {
                place(1, .66f, Alignment.TopEnd)
                place(0, .66f, Alignment.BottomStart)
            }
            3 -> {
                place(0, .55f, Alignment.TopCenter)
                place(1, .55f, Alignment.BottomStart)
                place(2, .55f, Alignment.BottomEnd)
            }
            else -> {
                place(0, .5f, Alignment.TopStart)
                place(1, .5f, Alignment.TopEnd)
                place(2, .5f, Alignment.BottomStart)
                if (faces.size == 4) place(3, .5f, Alignment.BottomEnd)
                else Box(Modifier.size(size * .5f).align(Alignment.BottomEnd), contentAlignment = Alignment.Center) {
                    Text("+${faces.size - 3}", style = Type.system(size.value * 0.24f, FontWeight.Bold), color = Palette.secondary)
                }
            }
        }
    }
}

private fun eyeColumns(cells: Int) = when (cells) { 7 -> listOf(2, 4); 9 -> listOf(3, 6); else -> listOf(4, 8) }
private fun eyeRows(cells: Int) = when (cells) { 7 -> listOf(2, 3); 9 -> listOf(3, 4); else -> listOf(4, 5, 6) }

private fun dotInk(cells: Int, shade: Double) = if (cells < 13) .4 + .4 * shade else .2 + .4 * min(1.0, shade / .7)

fun avatarDots(shape: String, cells: Int): List<Pair<Int, Int>> {
    if (shape.lowercase() in listOf("seed", "graine")) {
        return when (cells) {
            7 -> listOf(
                0 to 2, 0 to 3, 1 to 1, 2 to 0, 3 to 0, 4 to 0, 4 to 1,
                5 to 0, 5 to 1, 5 to 2, 6 to 1, 6 to 2, 6 to 3
            )
            9 -> listOf(
                0 to 2, 0 to 3, 0 to 4, 0 to 5, 1 to 1, 1 to 2, 2 to 0, 2 to 1,
                3 to 0, 3 to 1, 4 to 0, 4 to 1, 5 to 0, 5 to 1, 6 to 0, 6 to 1, 6 to 2,
                7 to 1, 7 to 2, 7 to 3, 7 to 4, 8 to 2, 8 to 3, 8 to 4
            )
            else -> listOf(
                0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8,
                1 to 2, 1 to 3, 1 to 4, 1 to 8, 1 to 9,
                2 to 1, 2 to 2, 2 to 3,
                3 to 1, 3 to 2,
                4 to 0, 4 to 1,
                5 to 0, 5 to 1,
                6 to 0, 6 to 1,
                7 to 0, 7 to 1,
                8 to 0, 8 to 1, 8 to 2,
                9 to 1, 9 to 2, 9 to 3,
                10 to 1, 10 to 2, 10 to 3, 10 to 4, 10 to 5, 10 to 6,
                11 to 2, 11 to 3, 11 to 4, 11 to 5, 11 to 6,
                12 to 3, 12 to 4, 12 to 5, 12 to 6
            )
        }
    }
    val path = avatarPath(shape)
    val region = Region().apply { setPath(path, Region(-100, -100, 1100, 1100)) }
    return buildList {
        for (row in 0 until cells) {
            for (col in 0 until cells) {
                if (region.contains(((col + .5) * 1000 / cells).toInt(), ((row + .5) * 1000 / cells).toInt())) {
                    add(row to col)
                }
            }
        }
    }
}

private fun avatarPath(shape: String): Path = Path().apply {
    fillType = Path.FillType.WINDING
    when (shape.lowercase()) {
        "pebble" -> addOval(RectF(0f, 100f, 1000f, 900f), Path.Direction.CW)
        "squircle" -> addRoundRect(RectF(40f, 40f, 960f, 960f), 300f, 300f, Path.Direction.CW)
        "tablet" -> addRoundRect(RectF(140f, 0f, 860f, 1000f), 220f, 220f, Path.Direction.CW)
        "wedge" -> {
            moveTo(500f, 40f)
            quadTo(900f, 400f, 980f, 860f)
            quadTo(500f, 1040f, 20f, 860f)
            quadTo(100f, 400f, 500f, 40f)
            close()
        }
        "hex" -> {
            for (i in 0 until 6) {
                val angle = i * PI / 3 - PI / 2
                val x = (500 + cos(angle) * 490).toFloat()
                val y = (500 + sin(angle) * 490).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
            val stroke = Path()
            android.graphics.Paint().apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 80f
                strokeJoin = android.graphics.Paint.Join.ROUND
            }.getFillPath(this, stroke)
            op(stroke, Path.Op.UNION)
        }
        "cloud" -> {
            addOval(RectF(0f, 300f, 550f, 850f), Path.Direction.CW)
            addOval(RectF(450f, 300f, 1000f, 850f), Path.Direction.CW)
            addOval(RectF(180f, 80f, 820f, 720f), Path.Direction.CW)
            addRoundRect(RectF(100f, 500f, 900f, 850f), 150f, 150f, Path.Direction.CW)
        }
        "teardrop" -> {
            moveTo(500f, 0f)
            cubicTo(620f, 200f, 940f, 380f, 940f, 620f)
            arcTo(RectF(60f, 180f, 940f, 1060f), 0f, 180f)
            cubicTo(60f, 380f, 380f, 200f, 500f, 0f)
            close()
        }
        "seed", "graine" -> {
            val cx = 500f
            val cy = 500f
            val steps = 72
            val tiltAngle = -12.0 * (PI / 180.0)
            val cosTilt = cos(tiltAngle)
            val sinTilt = sin(tiltAngle)
            for (i in 0..steps) {
                val t = i.toDouble() / steps * 2.0 * PI
                val a = 380.0
                val b = 470.0
                val k = 0.22
                val organicWarp = 1.0 + 0.04 * sin(2.0 * t + 0.4)
                val rawX = a * sin(t) * (1.0 - k * cos(t)) * organicWarp
                val rawY = -b * cos(t)
                val rotX = cx + (rawX * cosTilt - rawY * sinTilt).toFloat()
                val rotY = cy + (rawX * sinTilt + rawY * cosTilt).toFloat()
                if (i == 0) moveTo(rotX, rotY) else lineTo(rotX, rotY)
            }
            close()
        }
        else -> {
            for (i in 0..64) {
                val angle = i / 64.0 * 2 * PI
                val radius = .46 + .035 * sin(angle * 3 + .6)
                val x = (500 + cos(angle) * 1000 * radius).toFloat()
                val y = (500 + sin(angle) * 1000 * radius).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
    }
}
