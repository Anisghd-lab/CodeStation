package com.ghdinteractivestudio.synapse.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter

/**
 * Drawn symbols for the tab bar and navigation: overlapping speech bubbles and card stack.
 */
internal class DrawnSymbol(
    private val width: Float,
    private val height: Float,
    private val shape: () -> Path
) : Painter() {
    private var tint: ColorFilter? = null
    private val path by lazy(shape)

    override val intrinsicSize = Size.Unspecified

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        tint = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val unit = minOf(size.width / width, size.height / height)
        val dx = (size.width - width * unit) / 2
        val dy = (size.height - height * unit) / 2
        translate(dx, dy) {
            scale(unit, unit, Offset.Zero) {
                drawPath(path, Color.Black, colorFilter = tint)
            }
        }
    }
}

private fun round(left: Float, top: Float, right: Float, bottom: Float, radius: Float, bottomRadius: Float = radius) = Path().apply {
    addRoundRect(
        RoundRect(
            Rect(left, top, right, bottom),
            CornerRadius(radius),
            CornerRadius(radius),
            CornerRadius(bottomRadius),
            CornerRadius(bottomRadius)
        )
    )
}

private fun tail(vararg points: Float) = Path().apply {
    moveTo(points[0], points[1])
    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
    close()
}

private fun union(a: Path, b: Path) = Path().apply { op(a, b, PathOperation.Union) }

internal val Bubbles = DrawnSymbol(34.5f, 27.5f) {
    val left = union(round(0f, 0f, 25f, 18f, 9f), tail(3f, 15f, 0f, 23f, 10f, 18f))
    val right = union(round(9.5f, 9.5f, 34.5f, 27.5f, 9f), tail(31.5f, 24.5f, 34.5f, 32.5f, 24.5f, 27.5f))
    val cut = union(round(7.5f, 7.5f, 36.5f, 29.5f, 11f), tail(33.5f, 22.5f, 37f, 34.5f, 22.5f, 29.5f))
    Path().apply {
        op(left, cut, PathOperation.Difference)
        op(this, right, PathOperation.Union)
    }
}

internal val CardStack = DrawnSymbol(28f, 28f) {
    Path().apply {
        addRoundRect(RoundRect(Rect(4f, 8f, 24f, 26f), CornerRadius(4f)))
        addRoundRect(RoundRect(Rect(6f, 4f, 22f, 8f), CornerRadius(3f), CornerRadius(3f), CornerRadius(0f), CornerRadius(0f)))
        addRoundRect(RoundRect(Rect(8f, 1f, 20f, 4f), CornerRadius(2f), CornerRadius(2f), CornerRadius(0f), CornerRadius(0f)))
    }
}
