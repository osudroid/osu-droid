package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UIWedge.*
import com.reco1l.toolkt.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

class UIWedge : UIBufferedComponent<WedgeVBO>() {

    /**
     * The shear of the wedge.
     */
    var shear = 80f
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


    private fun getWedgeAngle(): Float {
        val x1 = width
        val y1 = 0f
        val x2 = width - radius - shear
        val y2 = height

        val dx = x2 - x1
        val dy = y2 - y1
        return atan2(dy, dx).toDegrees()
    }


    inner class WedgeVBO(
        val segments: Int,
        val shear: Float,
        val radius: Float,
        val style: PaintStyle,
    ) : VertexBuffer(
        vertexCount = max(1, segments) + 3,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW,
        drawTopology = if (style == PaintStyle.Fill) GL_TRIANGLE_FAN else GL_LINE_LOOP,
    ) {
        fun update() {

            var position = 0

            putVertex(position++, 0f, 0f)
            putVertex(position++, width, 0f)

            if (radius > 0f) {
                val wedgeAngle = getWedgeAngle()
                position = addArc(position, width - radius - shear, height - radius, wedgeAngle, 180f, radius, radius, segments)
            } else {
                putVertex(position++, width - shear, height)
            }
            putVertex(position, 0f, height)
        }
    }

    override fun onCreateBuffer(): WedgeVBO {
        val wedgeAngle = getWedgeAngle()
        val segments = if (radius > 0f) UICircle.approximateSegments(radius, radius, wedgeAngle) else 0

        val buffer = buffer
        if (buffer?.segments == segments && buffer.shear == shear && buffer.radius == radius && buffer.style == paintStyle) {
            return buffer
        }

        return WedgeVBO(segments, shear, radius, paintStyle)
    }

    override fun onUpdateBuffer() {
        buffer?.update()
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestNewBuffer()
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        if (paintStyle == PaintStyle.Outline) {
            GLHelper.lineWidth(gl, lineWidth)
        }
    }
}