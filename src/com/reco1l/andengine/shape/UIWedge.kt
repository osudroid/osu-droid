package com.reco1l.andengine.shape

import com.osudroid.math.toDegrees
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UIWedge.*
import com.reco1l.andengine.theme.srem
import org.andengine.opengl.util.GLState
import kotlin.math.*

/**
 * A rectangle whose bottom-right corner is sheared inwards, optionally rounded.
 */
open class UIWedge : UIBufferedComponent<WedgeVBO>() {

    /**
     * The shear of the wedge.
     */
    var shear = 8f.srem
        set(value) {
            if (field != value) {
                field = value
                requestNewBuffer()
            }
        }

    /**
     * The style of painting for the wedge.
     */
    var paintStyle = PaintStyle.Fill
        set(value) {
            if (field != value) {
                field = value
                requestNewBuffer()
            }
        }

    /**
     * The line width of the wedge if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f

    /**
     * The corner radius of the sheared corner.
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


    private fun getWedgeAngle(): Float {
        val x1 = width
        val y1 = 0f
        val x2 = width - cornerRadius - shear
        val y2 = height

        val dx = x2 - x1
        val dy = y2 - y1
        return atan2(dy, dx).toDegrees()
    }

    private fun getArcSegments(): Int {
        return if (cornerRadius > 0f) UICircle.approximateSegments(cornerRadius, cornerRadius, getWedgeAngle()) else 0
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        requestNewBuffer()
    }

    override fun onCreateBuffer(): WedgeVBO {
        val segments = getArcSegments()

        val buffer = buffer
        if (buffer?.segments == segments && buffer.shear == shear && buffer.radius == cornerRadius && buffer.paintStyle == paintStyle) {
            return buffer
        }

        return WedgeVBO(segments, shear, cornerRadius, paintStyle)
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)

        if (paintStyle == PaintStyle.Outline) {
            pGLState.lineWidth(lineWidth)
        }
    }


    class WedgeVBO(
        val segments: Int,
        val shear: Float,
        val radius: Float,
        val paintStyle: PaintStyle,
    ) : VertexBuffer(
        vertexCount = max(1, segments) + 3,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW,
        drawTopology = if (paintStyle == PaintStyle.Fill) GL_TRIANGLE_FAN else GL_LINE_LOOP,
    ) {
        fun update(entity: UIWedge) {

            val width = entity.width
            val height = entity.height

            var position = 0

            putVertex(position++, 0f, 0f)
            putVertex(position++, width, 0f)

            if (radius > 0f) {
                val wedgeAngle = entity.getWedgeAngle()
                position = addArc(position, width - radius - shear, height - radius, wedgeAngle, 180f, radius, radius, segments)
            } else {
                putVertex(position++, width - shear, height)
            }
            putVertex(position, 0f, height)
        }
    }
}
