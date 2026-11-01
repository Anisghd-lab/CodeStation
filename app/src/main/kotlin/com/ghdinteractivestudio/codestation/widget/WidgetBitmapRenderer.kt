package com.ghdinteractivestudio.codestation.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.ghdinteractivestudio.codestation.ui.components.avatarDots
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * High-fidelity in-memory Bitmap renderer for RemoteViews widgets.
 * Allows Android home screen widgets to display the exact same visual identity,
 * dotted character avatars, and segmented tick bars as the CodeStation application.
 */
object WidgetBitmapRenderer {

    private val colorMap = mapOf(
        "green" to 0xFF00C972.toInt(),
        "cyan" to 0xFF00BCA6.toInt(),
        "blue" to 0xFF1084FE.toInt(),
        "violet" to 0xFF9159FE.toInt(),
        "magenta" to 0xFFFF309B.toInt(),
        "yellow" to 0xFFFF9800.toInt(),
        "orange" to 0xFFFF6700.toInt(),
        "red" to 0xFFFF263C.toInt(),
        "gray" to 0xFF777777.toInt(),
        "black" to 0xFF2B2B2B.toInt()
    )

    /**
     * Renders a CodeStation Dotted Character Avatar with 3D spherical lighting into an Android Bitmap.
     */
    fun renderAvatar(
        shape: String,
        colorName: String,
        sizePx: Int,
        highlightColor: Int? = null,
        status: String = "idle"
    ): Bitmap {
        val safeSize = max(24, sizePx)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cells = 13
        val dots = avatarDots(shape, cells)
        val step = safeSize.toFloat() / cells
        val half = safeSize / 2f

        val baseColor = highlightColor ?: colorMap[colorName.lowercase()] ?: 0xFF1084FE.toInt()

        val lx = -0.5153
        val ly = 0.55
        val lz = 0.9824
        val ll = 1.2393

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((row, col) in dots) {
            val cx = (col + 0.5f) * step
            val cy = (row + 0.5f) * step

            val u = ((cx - half) / half).toDouble()
            val v = ((half - cy) / half).toDouble()
            val z = sqrt(max(0.2, 1.0 - u * u - v * v))
            val nl = sqrt(u * u + v * v + z * z)

            val dotProd = u * lx + v * ly + z * lz
            val shade = 0.35 + 0.65 * max(0.0, dotProd / (nl * ll))

            val radius = (step * 0.42f * (0.55f + 0.45f * shade.toFloat())).coerceIn(1.2f, step * 0.48f)

            // Base shaded dot
            paint.color = baseColor
            val alphaFactor = min(1.0, 0.45 + 0.55 * shade)
            paint.alpha = (alphaFactor * 255).toInt().coerceIn(60, 255)
            canvas.drawCircle(cx, cy, radius, paint)

            // Specular luminous highlight
            if (shade > 0.65) {
                paint.color = Color.WHITE
                val specAlpha = (((shade - 0.65) / 0.35) * 200).toInt().coerceIn(0, 200)
                paint.alpha = specAlpha
                canvas.drawCircle(cx, cy, radius * 0.45f, paint)
            }
        }

        // Active status ring if working
        if (status in listOf("working", "busy")) {
            val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(2f, safeSize * 0.04f)
                color = 0xFF10B981.toInt()
            }
            canvas.drawCircle(half, half, half - ringPaint.strokeWidth, ringPaint)
        }

        return bitmap
    }

    /**
     * Renders CodeStation's signature UsageTicks progress bar (segmented discrete ticks).
     */
    fun renderUsageTicks(
        percent: Double,
        tintColor: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val safeW = max(40, widthPx)
        val safeH = max(8, heightPx)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val value = percent.coerceIn(0.0, 100.0)
        val activeColor = if (value >= 90.0) 0xFFEF4444.toInt() else tintColor

        val gap = 3f
        val tickWidth = 6f
        val count = max(1, ((safeW + gap) / (tickWidth + gap)).toInt())
        val actualWidth = (safeW - gap * (count - 1)) / count
        val filled = (count * value / 100.0).roundToInt()

        for (i in 0 until count) {
            val left = i * (actualWidth + gap)
            val right = left + actualWidth
            val rect = RectF(left, 0f, right, safeH.toFloat())

            if (i < filled) {
                paint.color = activeColor
                paint.alpha = 255
            } else {
                paint.color = activeColor
                paint.alpha = 40
            }
            canvas.drawRoundRect(rect, 2f, 2f, paint)
        }
        return bitmap
    }

    /**
     * Renders a smooth continuous progress bar with subtle gradient for system metrics.
     */
    fun renderProgressBar(
        percent: Double,
        tintColor: Int,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val safeW = max(40, widthPx)
        val safeH = max(6, heightPx)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val corner = safeH / 2f

        // Background Track
        paint.color = 0x25FFFFFF
        canvas.drawRoundRect(RectF(0f, 0f, safeW.toFloat(), safeH.toFloat()), corner, corner, paint)

        // Filled Indicator
        val fillWidth = (safeW * (percent.coerceIn(0.0, 100.0) / 100.0)).toFloat()
        if (fillWidth > 1f) {
            paint.color = tintColor
            canvas.drawRoundRect(RectF(0f, 0f, fillWidth, safeH.toFloat()), corner, corner, paint)
        }

        return bitmap
    }

    /**
     * Renders a glowing server icon for system metrics widgets.
     */
    fun renderServerIcon(sizePx: Int): Bitmap {
        val safeSize = max(24, sizePx)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val pad = safeSize * 0.12f
        val w = safeSize - 2 * pad
        val h = (safeSize - 2 * pad - pad * 0.5f) / 2f
        val corner = 4f

        // Server rack units
        paint.style = Paint.Style.FILL
        paint.color = 0xFF1E293B.toInt()
        val rect1 = RectF(pad, pad, pad + w, pad + h)
        val rect2 = RectF(pad, pad + h + pad * 0.4f, pad + w, pad + 2 * h + pad * 0.4f)
        canvas.drawRoundRect(rect1, corner, corner, paint)
        canvas.drawRoundRect(rect2, corner, corner, paint)

        // Sleek border stroke
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0xFF38BDF8.toInt()
        canvas.drawRoundRect(rect1, corner, corner, paint)
        canvas.drawRoundRect(rect2, corner, corner, paint)

        // Glowing LEDs
        paint.style = Paint.Style.FILL
        paint.color = 0xFF10B981.toInt()
        canvas.drawCircle(pad + w * 0.25f, pad + h * 0.5f, 3f, paint)
        canvas.drawCircle(pad + w * 0.25f, pad + h + pad * 0.4f + h * 0.5f, 3f, paint)

        return bitmap
    }
}
