package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.BezelOutline.*
import com.reco1l.framework.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

open class BezelOutline(cornerRadius: Float = 0f) : BufferedEntity<BezelOutlineVBO>() {

    /**
     * The corner radius of the rectangle.
     */
    var cornerRadius = cornerRadius
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Instance)
            }
        }

    /**
     * The color of the outline top.
     */
    var topColor = ColorARGB(1f, 1f, 1f, 0.075f)

    /**
     * The color of the outline bottom.
     */
    var bottomColor = ColorARGB(0f, 0f, 0f, 0.25f)


    override fun onSizeChanged() {
        super.onSizeChanged()
        invalidateBuffer(BufferInvalidationFlag.Instance)
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.lineWidth(gl, 1f)
    }

    override fun onCreateBuffer(gl: GL10): BezelOutlineVBO {

        val radius = cornerRadius.coerceIn(0f, min(width, height) / 2f)
        val segments = if (radius > 0f) Circle.approximateSegments(radius, radius, 90f) else 0

        val buffer = buffer
        if (buffer?.radius == radius && buffer.segments == segments) {
            return buffer
        }

        return BezelOutlineVBO(radius, segments)
    }

    override fun onApplyColor(gl: GL10) {
        // Applying at buffer level
    }


    inner class BezelOutlineVBO(
        val radius: Float,
        val segments: Int
    ) : VertexBuffer(

        // Segments * Arc count + Closing point
        vertexCount = 4,

        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW,
        drawTopology = GL_LINES
    ) {
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            val width = width
            val height = height
            val halfLineWidth = 0.5f

            addLine(0, radius, halfLineWidth,width - radius, halfLineWidth)
            addLine(2, radius, height - halfLineWidth, width - radius, height - halfLineWidth)
        }

        override fun draw(gl: GL10, entity: BufferedEntity<*>) {

            val halfVertices = vertexCount / 2

            GLHelper.setColor(gl, topColor.red, topColor.green, topColor.blue, topColor.alpha)
            gl.glDrawArrays(drawTopology, 0, halfVertices)

            GLHelper.setColor(gl, bottomColor.red, bottomColor.green, bottomColor.blue, bottomColor.alpha)
            gl.glDrawArrays(drawTopology, halfVertices, halfVertices)
        }
    }
}