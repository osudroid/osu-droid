package com.reco1l.andengine.shape

import androidx.annotation.*
import androidx.annotation.IntRange
import com.reco1l.andengine.*
import com.reco1l.andengine.shape.CircleVertexBuffer.Companion.DEFAULT_CIRCLE_SEGMENTS
import com.reco1l.toolkt.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.*
import kotlin.math.*

/**
 * A circle shape.
 *
 * @param segments The amount of segments that make up the circle.
 *                 Higher values result in a smoother circle but may impact performance.
 * @author Reco1l
 */
class Circle(segments: Int = DEFAULT_CIRCLE_SEGMENTS) : ExtendedEntity(vertexBuffer = CircleVertexBuffer(segments)) {


    /**
     * The amount of segments that make up the circle.
     * Higher values result in a smoother circle but may impact performance.
     *
     * The minimum value is 8. By default, the value is [DEFAULT_CIRCLE_SEGMENTS].
     *
     * It is recommended to not update this value frequently, internally it will
     * recreate the vertex buffer which can lead to performance degradation if it
     * is done too often.
     */
    var segments = segments
        set(@IntRange(from = 8) value) {
            if (field != value) {
                field = value
                setVertexBuffer(CircleVertexBuffer(value))
            }
        }

    /**
     * The angle where the circle starts to draw in degrees. By default, it is -90 degrees.
     */
    var startAngle = -90f
        set(@FloatRange(0.0, 360.0) value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The angle where the circle ends to draw in degrees. By default, it is 360 degrees (a full circle).
     */
    var endAngle = 360f
        set(@FloatRange(0.0, 360.0) value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    /**
     * Sets the portion of the circle to be drawn starting from the start angle.
     */
    fun setPortion(value: Float) {
        endAngle = startAngle - 360f * value
        updateVertexBuffer()
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as CircleVertexBuffer).update(width, height, startAngle, endAngle)
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        (vertexBuffer as CircleVertexBuffer).draw(pGL)
    }

}


class CircleVertexBuffer(@IntRange(from = 8) val segments: Int) : VertexBuffer(

    // Explanation: Segments + 2 because the first vertex that is the center of the circle is not included in the segment
    // count and we add it twice so that the last vertex connects to the first one.
    (segments + 2) * 2,

    GL11.GL_STATIC_DRAW, true
) {


    fun update(width: Float, height: Float, startAngle: Float, endAngle: Float) {

        val buffer = floatBuffer

        buffer.position(0)

        val ratioX = width / 2f
        val ratioY = height / 2f

        buffer.put(0, ratioX)
        buffer.put(1, ratioY)

        val startRadians = startAngle.toRadians()
        val endRadians = endAngle.toRadians()

        val deltaAngle = (endRadians - startRadians) / segments

        for (i in 0..segments) {

            val angle = startRadians + i * deltaAngle

            buffer.put((i + 1) * 2, ratioX + ratioX * cos(angle))
            buffer.put((i + 1) * 2 + 1, ratioY + ratioY * sin(angle))
        }

        buffer.position(0)

        setHardwareBufferNeedsUpdate()
    }

    fun draw(pGL: GL10) {
        pGL.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, segments + 2)
    }


    companion object {

        const val DEFAULT_CIRCLE_SEGMENTS = 32

    }

}

