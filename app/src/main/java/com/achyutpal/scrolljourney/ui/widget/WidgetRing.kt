package com.achyutpal.scrolljourney.ui.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap

/**
 * Widgets can't draw shapes (they're RemoteViews, not a Canvas), so the progress ring is painted
 * into a small bitmap here and shown with Glance's Image. Kept small: widget bitmaps are sent to
 * the launcher process, which limits their total size.
 */
object WidgetRing {

    fun bitmap(fraction: Float, color: Color, track: Color, sizePx: Int = SIZE_PX): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val stroke = sizePx * STROKE_FRACTION
        val bounds = RectF(stroke / 2, stroke / 2, sizePx - stroke / 2, sizePx - stroke / 2)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        paint.color = track.toArgb()
        canvas.drawOval(bounds, paint)
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        if (sweep > 0f) {
            paint.color = color.toArgb()
            canvas.drawArc(bounds, -90f, sweep, false, paint) // starts at 12 o'clock, clockwise
        }
        return bitmap
    }

    private const val SIZE_PX = 160
    private const val STROKE_FRACTION = 0.12f
}
