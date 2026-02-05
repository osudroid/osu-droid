package com.reco1l.andengine.shape

import androidx.annotation.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.shape.UICircle.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*


/**
 * A circle shape.
 *
 * @author Reco1l
 */
open class UICircle : UIBufferedComponent<CircleVertexBuffer>() {

    /**
     * The paint style of the circle.
     */
    var paintStyle = PaintStyle.Fill
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
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
        requestBufferUpdate()
    }


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.lineWidth(gl, lineWidth)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    override fun createBuffer(): CircleVertexBuffer {
        val segments = calculateArcResolution(width, height)
        return CircleVertexBuffer(segments, paintStyle)
    }

    override fun generateBufferCacheKey(): String {
        val segments = calculateArcResolution(width, height)
        return "CircleVBO@$segments,$paintStyle"
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }


    class CircleVertexBuffer(val segments: Int, val paintStyle: PaintStyle) : VertexBuffer(

        // Segments + Center point
        vertexCount = segments + if (paintStyle == PaintStyle.Fill) 1 else 0,

        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW,
        drawTopology = if (paintStyle == PaintStyle.Fill) GL_TRIANGLE_FAN else GL_LINE_STRIP
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

        /**
         * Calculates the number of segments needed to render a smooth arc.
         */
        fun calculateArcResolution(width: Float, height: Float, arcAngle: Float = 360f): Int {
            val averageRadius = (width + height) / 4f

            // We use 5 degrees as the maximum angle between segments for visual smoothness.
            val anglePerSegment = min(5f, 360f / averageRadius)
            val segments = (abs(arcAngle) / anglePerSegment).toInt()
            return max(3, segments)
        }

        @Deprecated(
            message = "Use calculateArcResolution instead",
            replaceWith = ReplaceWith("calculateArcResolution(width, height, maximumAngle)")
        )
        fun approximateSegments(width: Float, height: Float, maximumAngle: Float = 360f): Int {
            return calculateArcResolution(width, height, maximumAngle)
        }

    }
}
