package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.Box.*
import com.reco1l.andengine.shape.PaintStyle.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
open class Box : BufferedEntity<BoxVBO>() {

    /**
     * The style of painting for the box.
     */
    var paintStyle = Fill
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Instance)
            }
        }

    /**
     * The line width of the box if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f

    /**
     * The corner radius of the rectangle.
     */
    var cornerRadius = 0f
        set(value) {
            if (value < 0f) {
                throw IllegalArgumentException("Corner radius cannot be negative.")
            }

            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Instance)
            }
        }


    override fun onSizeChanged() {
        super.onSizeChanged()
        invalidateBuffer(BufferInvalidationFlag.Data)
    }

    override fun onCreateBuffer(gl: GL10): BoxVBO {

        val radius = cornerRadius.coerceIn(0f, min(width, height) / 2f)
        val segments = if (radius > 0f) Circle.approximateSegments(radius, radius, 90f) else 0

        val buffer = buffer
        if (buffer?.radius == radius && buffer.segments == segments) {
            return buffer
        }

        return BoxVBO(radius, segments, paintStyle)
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        if (paintStyle == Outline) {
            GLHelper.lineWidth(gl, lineWidth)
        }
    }


    inner class BoxVBO(
        val radius: Float,
        val segments: Int,
        paintStyle: PaintStyle
    ) : VertexBuffer(

        // Segments * Arc count + Center point + Closing point
        vertexCount = max(1, segments) * 4 + if (paintStyle == Fill) 2 else 1,

        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW,
        drawTopology = when (paintStyle) {
            Fill -> GL_TRIANGLE_FAN
            Outline -> GL_LINE_STRIP
        }
    ) {
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            val width = width
            val height = height
            val segments = max(1, segments)

            var position = 0

            // Disposition: Arcs are drawn in a clockwise manner
            // [1][2]
            // [4][3]

            // Center
            if (paintStyle == Fill) {
                putVertex(position++, width / 2f, height / 2f)
            }

            // [1]
            position = addArc(position, radius, radius, -180f, -90f, radius, radius, segments)
            // [2]
            position = addArc(position, width - radius, radius, -90f, 0f, radius, radius, segments)
            // [3]
            position = addArc(position, width - radius, height - radius, 0f, 90f, radius, radius, segments)
            // [4]
            position = addArc(position, radius, height - radius, 90f, 180f, radius, radius, segments)

            // Closing point
            putVertex(position, 0f, radius)
        }
    }

}

