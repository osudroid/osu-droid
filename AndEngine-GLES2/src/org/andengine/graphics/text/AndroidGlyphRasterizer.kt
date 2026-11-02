package org.andengine.graphics.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max

/**
 * Rasterizes glyphs of [typeface] at [sizePx] with an Android [Canvas] into an alpha-only bitmap.
 * With [strokeWidthPx] > 0, layer 1 holds the outline; both layers share the bounds of the stroke so
 * they line up. Not thread-safe; the owning font serializes access.
 */
class AndroidGlyphRasterizer @JvmOverloads constructor(
    typeface: Typeface?,
    sizePx: Float,
    antiAlias: Boolean,
    private val strokeWidthPx: Float = 0f
) : GlyphRasterizer {

    private val fill = Paint().apply {
        this.typeface = typeface
        textSize = sizePx
        isAntiAlias = antiAlias
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val stroke: Paint? = if (strokeWidthPx > 0f) Paint().apply {
        this.typeface = typeface
        textSize = sizePx
        isAntiAlias = antiAlias
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    } else null

    private val strokeInset = ceil(strokeWidthPx * 0.5f).toInt()

    private val fontMetrics = fill.fontMetrics

    override val ascent: Float get() = fontMetrics.ascent

    override val descent: Float get() = fontMetrics.descent

    private val chars = CharArray(1)
    private val widths = FloatArray(1)
    private val bounds = Rect()
    private val canvas = Canvas()

    private var scratch: Bitmap? = null
    private var scratchBytes = ByteArray(0)

    private fun measureBounds(c: Char) {
        chars[0] = c
        fill.getTextBounds(chars, 0, 1, bounds)
        if (stroke != null && !bounds.isEmpty) {
            bounds.inset(-strokeInset, -strokeInset)
        }
    }

    override fun measure(c: Char, out: GlyphMetrics) {
        measureBounds(c)
        fill.getTextWidths(chars, 0, 1, widths)
        out.width = bounds.width()
        out.height = bounds.height()
        out.offsetX = bounds.left.toFloat()
        out.offsetY = bounds.top.toFloat()
        out.advance = widths[0]
    }

    override fun rasterize(c: Char, layer: Int, out: ByteBuffer) {
        val paint = when (layer) {
            0 -> fill
            1 -> stroke ?: throw IllegalArgumentException("No stroke layer without a stroke width")
            else -> throw IllegalArgumentException("Unknown glyph layer $layer")
        }
        measureBounds(c)
        val width = bounds.width()
        val height = bounds.height()
        if (width <= 0 || height <= 0) return

        var bitmap = scratch
        if (bitmap == null || bitmap.width < width || bitmap.height < height) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(
                max(width, bitmap?.width ?: 0),
                max(height, bitmap?.height ?: 0),
                Bitmap.Config.ALPHA_8
            )
            scratch = bitmap
        }
        bitmap.eraseColor(Color.TRANSPARENT)
        canvas.setBitmap(bitmap)
        canvas.drawText(chars, 0, 1, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
        canvas.setBitmap(null)

        val rowBytes = bitmap.rowBytes
        val total = rowBytes * bitmap.height
        if (scratchBytes.size < total) scratchBytes = ByteArray(total)
        bitmap.copyPixelsToBuffer(ByteBuffer.wrap(scratchBytes, 0, total))
        for (row in 0 until height) {
            out.put(scratchBytes, row * rowBytes, width)
        }
    }
}
