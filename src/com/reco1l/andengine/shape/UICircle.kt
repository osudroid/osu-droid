package com.reco1l.andengine.shape

import org.andengine.graphics.PrimitiveType
import androidx.annotation.*
import com.osudroid.math.toRadians
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.shape.UICircle.*
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching
import kotlin.math.*


/**
 * A circle shape.
 *
 * @author Reco1l
 */
open class UICircle : UIBufferedComponent<CircleVertexBuffer>(batchesShape = true) {

    /**
     * The paint style of the circle.
     */
    var paintStyle = PaintStyle.Fill
        set(value) {
            if (field != value) {
                field = value
                requestNewBuffer()
            }
        }

    /**
     * The line width if the paint style is [PaintStyle.Outline].
     */
    var lineWidth = 1f

    /**
     * The angle where the circle starts to draw in degrees. By default, it is 0 degrees.
     */
    var startAngle = 0f
        set(@FloatRange(-360.0, 360.0) value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }

    /**
     * The angle where the circle ends to draw in degrees. By default, it is 360 degrees.
     */
    var endAngle = 360f
        set(@FloatRange(-360.0, 360.0) value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }

    /**
     * Sets the portion of the circle to be drawn starting from the start angle.
     *
     * Positive values will draw the circle clockwise and negative values will draw it counter-clockwise.
     */
    fun setPortion(value: Float) {
        endAngle = startAngle + 360f * value.coerceIn(-1f, 1f)
    }


    override fun beginDraw(drawContext: DrawContext) {
        super.beginDraw(drawContext)
        drawContext.lineWidth(lineWidth)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestNewBuffer()
    }

    // Only a full circle with equal sides uses the smoothed circle and ring masks.
    private fun isFullCircle(): Boolean {
        return abs(endAngle - startAngle) >= 360f && abs(width - height) <= 1e-3f
    }

    override fun onDrawBatch(gl: DrawContext, blend: BlendMode) {
        val color = QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha)
        val batchLineWidth = ShapeBatching.lineWidthFromPixels(lineWidth)

        QuadBatcher.setProjection(gl.projectionGLMatrix)
        // Full circles use the ring band, which lies inside the radius; partial outlines follow a line
        // strip centered on the radius. An outline whose portion reaches exactly a full circle therefore
        // switches between the two looks.
        if (isFullCircle()) {
            val radius = width / 2f
            if (paintStyle == PaintStyle.Fill) {
                ShapeBatching.circle(gl.modelViewGLMatrix, radius, radius, radius, color, blend)
            } else {
                ShapeBatching.ring(gl.modelViewGLMatrix, radius, radius, radius, batchLineWidth, color, blend)
            }
        } else {
            // Same points as the buffer: radii of half the size, around the center.
            val rx = width / 2f
            val ry = height / 2f
            val segments = approximateSegments(width, height)
            if (paintStyle == PaintStyle.Fill) {
                ShapeBatching.sector(gl.modelViewGLMatrix, rx, ry, rx, ry, startAngle, endAngle, segments, color, blend)
            } else {
                ShapeBatching.arc(gl.modelViewGLMatrix, rx, ry, rx, ry, startAngle, endAngle, segments, batchLineWidth, color, blend)
            }
        }
    }

    // The batch needs no buffer; it is only built when the circle cannot be batched (see canBatchShape()).
    override fun onCreateBuffer(): CircleVertexBuffer? {

        if (canBatchShape()) {
            return null
        }

        val buffer = buffer
        val segments = approximateSegments(width, height)

        if (buffer?.segments == segments && buffer.paintStyle == paintStyle) {
            return buffer
        }

        return CircleVertexBuffer(segments, paintStyle)
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }


    class CircleVertexBuffer(val segments: Int, val paintStyle: PaintStyle) : VertexBuffer(

        // Segments + Center point
        vertexCount = segments + if (paintStyle == PaintStyle.Fill) 1 else 0,

        vertexSize = VERTEX_2D,
        drawTopology = if (paintStyle == PaintStyle.Fill) PrimitiveType.TriangleFan else PrimitiveType.LineStrip
    ) {

        fun update(entity: UICircle) {

            val halfWidth = entity.width / 2f
            val halfHeight = entity.height / 2f

            var position = 0

            if (paintStyle == PaintStyle.Fill) {
                putVertex(position++, halfWidth, halfHeight)
            }

            addArc(position, halfWidth, halfHeight, entity.startAngle, entity.endAngle, halfWidth, halfHeight, segments)
        }
    }


    companion object {

        fun approximateSegments(width: Float, height: Float, maximumAngle: Float = 360f): Int {
            val averageRadius = (width + height) / 4f
            val minSegmentAngle = min(5f, 360f / averageRadius.toRadians())
            return max(3, (maximumAngle / minSegmentAngle).toInt())
        }

    }
}

