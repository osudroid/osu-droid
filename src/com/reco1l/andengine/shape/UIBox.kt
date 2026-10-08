package com.reco1l.andengine.shape

import com.reco1l.andengine.component.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UIBox.*
import com.reco1l.andengine.shape.PaintStyle.*
import org.andengine.opengl.util.GLState
import kotlin.math.*

/**
 * A rectangle shape based on [UIComponent].
 */
open class UIBox : UIBufferedComponent<BoxVBO>() {

    /**
     * The style of painting for the box.
     */
    var paintStyle = Fill
        set(value) {
            if (field != value) {
                field = value
                requestNewBuffer()
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
                requestNewBuffer()
            }
        }


    override fun onSizeChanged() {
        super.onSizeChanged()
        requestNewBuffer()
    }

    override fun onCreateBuffer(): BoxVBO {

        val radius = cornerRadius.coerceAtMost(min(width, height) / 2f).coerceAtLeast(0f)
        val segments = if (radius > 0f) UICircle.approximateSegments(radius, radius, 90f) else 0

        val buffer = buffer
        if (buffer?.radius == radius && buffer.segments == segments && buffer.paintStyle == paintStyle) {
            return buffer
        }

        return BoxVBO(radius, segments, paintStyle)
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)

        if (paintStyle == Outline) {
            pGLState.lineWidth(lineWidth)
        }
    }


    class BoxVBO(
        val radius: Float,
        val segments: Int,
        val paintStyle: PaintStyle
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

        /**
         * Vertex offsets of the four corner arcs relative to their centers, they only depend on the radius
         * so a size change only has to move them.
         */
        private val arcOffsets = FloatArray(max(1, segments) * 4 * 2).also { offsets ->
            val segments = max(1, segments)
            var i = 0

            for (arc in 0 until 4) {
                val startAngle = Math.toRadians(arc * 90.0 - 180.0)
                val deltaAngle = Math.toRadians(90.0) / max(1, segments - 1)

                for (j in 0 until segments) {
                    val angle = startAngle + j * deltaAngle
                    offsets[i++] = radius * cos(angle).toFloat()
                    offsets[i++] = radius * sin(angle).toFloat()
                }
            }
        }

        fun update(entity: UIBox) {

            val width = entity.width
            val height = entity.height
            val segments = max(1, segments)

            var position = 0

            // Disposition: Arcs are drawn in a clockwise manner
            // [1][2]
            // [4][3]

            // Center
            if (paintStyle == Fill) {
                putVertex(position++, width / 2f, height / 2f)
            }

            var offset = 0

            for (arc in 0 until 4) {
                val centerX = if (arc == 0 || arc == 3) radius else width - radius
                val centerY = if (arc < 2) radius else height - radius

                repeat(segments) {
                    putVertex(position++, centerX + arcOffsets[offset], centerY + arcOffsets[offset + 1])
                    offset += 2
                }
            }

            // Closing point
            putVertex(position, 0f, radius)
        }
    }

}