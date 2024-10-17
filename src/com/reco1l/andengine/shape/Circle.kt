package com.reco1l.andengine.shape

import androidx.annotation.*
import androidx.annotation.IntRange
import com.reco1l.andengine.*
import com.reco1l.andengine.shape.CircleVertexBuffer.Companion.DEFAULT_CIRCLE_SEGMENTS
import com.reco1l.toolkt.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
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
        set(@IntRange(from = 1) value) {
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
     * The angle where the circle ends to draw in degrees. By default, it is 270 degrees.
     */
    var endAngle = 270f
        set(@FloatRange(0.0, 360.0) value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    /**
     * Sets the portion of the circle to be drawn starting from the start angle.
     *
     * Positive values will draw the circle clockwise and negative values will draw it counter-clockwise.
     */
    fun setPortion(value: Float) {
        endAngle = startAngle + 360f * value.coerceIn(-1f, 1f)
        updateVertexBuffer()
    }


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }

    override fun onUpdateVertexBuffer() {
        (vertexBuffer as CircleVertexBuffer).update(width, height, startAngle, endAngle)
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        (vertexBuffer as CircleVertexBuffer).draw(pGL)
    }

}


class CircleVertexBuffer(@IntRange(from = 1) val segments: Int) : VertexBuffer(

    // Explanation: Segments + 2 because the first vertex that is the center of the circle is not included in the segment
    // count and we add it twice so that the last vertex connects to the first one.
    (segments + 2) * 2,

    GL11.GL_STATIC_DRAW, true
) {


    fun update(width: Float, height: Float, startAngle: Float, endAngle: Float) {

        val buffer = floatBuffer

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

        setHardwareBufferNeedsUpdate()
    }

    fun draw(pGL: GL10) {
        pGL.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, segments + 2)
    }


    companion object {

        const val DEFAULT_CIRCLE_SEGMENTS = 16

    }

}

