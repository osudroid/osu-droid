package com.reco1l.andengine.shape

import androidx.annotation.*
import androidx.annotation.IntRange
import com.reco1l.andengine.*
import com.reco1l.toolkt.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*


/**
 * A circle shape.
 *
 * @author Reco1l
 */
class Circle : ExtendedEntity() {

    /**
     * The angle where the circle starts to draw in degrees. By default, it is -90 degrees.
     */
    var startAngle = -90f
        set(@FloatRange(-360.0, 360.0) value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The angle where the circle ends to draw in degrees. By default, it is 270 degrees.
     */
    var endAngle = 270f
        set(@FloatRange(-360.0, 360.0) value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    private var shouldRebuildVertexBuffer = true


    /**
     * Sets the portion of the circle to be drawn starting from the start angle.
     *
     * Positive values will draw the circle clockwise and negative values will draw it counter-clockwise.
     */
    fun setPortion(value: Float) {
        endAngle = startAngle + 360f * value.coerceIn(-1f, 1f)
        updateVertexBuffer()
    }


    override fun setSize(newWidth: Float, newHeight: Float): Boolean {
        if (super.setSize(newWidth, newHeight)) {
            shouldRebuildVertexBuffer = true
            return true
        }
        return false
    }

    override fun onContentSizeMeasured(): Boolean {
        if (super.onContentSizeMeasured()) {
            shouldRebuildVertexBuffer = true
            return true
        }
        return false
    }


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }

    override fun onUpdateVertexBuffer() {

        if (shouldRebuildVertexBuffer) {
            shouldRebuildVertexBuffer = false

            val segments = approximateSegments(width, height, startAngle, endAngle)

            setVertexBuffer(CircleVertexBuffer(segments))
        }

        (vertexBuffer as CircleVertexBuffer).update(width, height, startAngle, endAngle)
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        (vertexBuffer as? CircleVertexBuffer)?.draw(pGL)
    }


    companion object {

        fun approximateSegments(width: Float, height: Float, startAngle: Float, endAngle: Float): Int {

            val averageRadius = (width + height) / 4f
            val angleRange = abs(endAngle - startAngle)
            val minSegmentAngle = min(10f, 360f / averageRadius.toRadians())

            return max(3, (angleRange / minSegmentAngle).toInt())
        }

    }

}


class CircleVertexBuffer(@IntRange(from = 1) val segments: Int) : VertexBuffer(

    // Explanation: Segments + 2 because the first vertex that is the center of the circle is not included in the segment
    // count and we add it twice so that the last vertex connects to the first one.
    (segments + 2) * 2,

    GL11.GL_STATIC_DRAW, false
) {


    fun update(width: Float, height: Float, startAngle: Float, endAngle: Float) {

        val buffer = floatBuffer

        val centerX = width / 2f
        val centerY = height / 2f

        buffer.put(0, centerX)
        buffer.put(1, centerY)

        val startRadians = startAngle.toRadians()
        val endRadians = endAngle.toRadians()

        val deltaAngle = (endRadians - startRadians) / segments

        // The first vertex is the center of the circle.
        for (i in 1..segments + 1) {

            val angle = startRadians + (i - 1) * deltaAngle

            val x = centerX + centerX * cos(angle)
            val y = centerY + centerY * sin(angle)

            buffer.put(i * 2 + 0, x)
            buffer.put(i * 2 + 1, y)
        }

        setHardwareBufferNeedsUpdate()
    }

    fun draw(pGL: GL10) {
        pGL.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, segments + 2)
    }

}

