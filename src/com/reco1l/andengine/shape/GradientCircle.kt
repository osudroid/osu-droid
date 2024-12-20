package com.reco1l.andengine.shape

import androidx.annotation.IntRange
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.Colors
import com.reco1l.toolkt.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.*
import kotlin.math.*


/**
 * A circle shape.
 *
 * @author Reco1l
 */
class GradientCircle : Circle() {


    /**
     * The color at the start angle.
     */
    var startColor: ColorARGB = ColorARGB.Black
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The color at the end angle.
     */
    var endColor = ColorARGB.White
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    private var shouldRebuildVertexBuffer = true


    override fun onUpdateVertexBuffer() {

        if (shouldRebuildVertexBuffer) {
            shouldRebuildVertexBuffer = false

            val segments = approximateSegments(drawWidth, drawHeight)
            setVertexBuffer(GradientCircleVertexBuffer(segments))
        }

        (vertexBuffer as GradientCircleVertexBuffer).update(
            drawWidth,
            drawHeight,
            startAngle,
            endAngle,
            startColor,
            endColor
        )
    }

}


class GradientCircleVertexBuffer(@IntRange(from = 1) segments: Int) : CircleVertexBuffer(segments) {

    /**
     * The buffer that holds the color data.
     */
    val colorBuffer: FloatBuffer = ByteBuffer.allocateDirect((segments + 2) * 4 * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()


    fun update(
        width: Float,
        height: Float,
        startAngle: Float,
        endAngle: Float,
        startColor: ColorARGB,
        endColor: ColorARGB
    ) {
        super.update(width, height, startAngle, endAngle)

        colorBuffer.put(0, startColor.red)
        colorBuffer.put(1, startColor.green)
        colorBuffer.put(2, startColor.blue)
        colorBuffer.put(3, 1f)

        val halfSegments = segments / 2

        // The first vertex is the center of the circle.
        for (i in 1..segments + 1) {

            var j = i - 1f
            if (j > halfSegments) {
                j = halfSegments - (j - halfSegments)
            }

            val color = Colors.interpolate(j, startColor, endColor, 0f, halfSegments.toFloat())

            colorBuffer.put(i * 4 + 0, color.red)
            colorBuffer.put(i * 4 + 1, color.green)
            colorBuffer.put(i * 4 + 2, color.blue)
            colorBuffer.put(i * 4 + 3, 1f)
        }

        setHardwareBufferNeedsUpdate()
    }

    override fun draw(pGL: GL10) {
        pGL.glEnableClientState(GL10.GL_COLOR_ARRAY)
        pGL.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer)
        super.draw(pGL)
        pGL.glDisableClientState(GL10.GL_COLOR_ARRAY)
    }

}

