package com.reco1l.andengine.shape

import androidx.annotation.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.shape.Circle.*
import com.reco1l.toolkt.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*


/**
 * A circle shape.
 *
 * @author Reco1l
 */
open class Circle : BufferedEntity<CircleVertexBuffer>() {

    /**
     * The angle where the circle starts to draw in degrees. By default, it is -90 degrees.
     */
    var startAngle = -90f
        set(@FloatRange(-360.0, 360.0) value) {
            if (field != value) {
                field = value
                invalidateBuffer()
            }
        }

    /**
     * The angle where the circle ends to draw in degrees. By default, it is 270 degrees.
     */
    var endAngle = 270f
        set(@FloatRange(-360.0, 360.0) value) {
            if (field != value) {
                field = value
                invalidateBuffer()
            }
        }

    override var invalidationFlags = RebuildBufferOnSizeChanged or InvalidateDataOnSizeChanged


    /**
     * Sets the portion of the circle to be drawn starting from the start angle.
     *
     * Positive values will draw the circle clockwise and negative values will draw it counter-clockwise.
     */
    fun setPortion(value: Float) {
        endAngle = startAngle + 360f * value.coerceIn(-1f, 1f)
        invalidateBuffer()
    }


    override fun onRebuildBuffer(gl: GL10) {
        val segments = approximateSegments(drawWidth, drawHeight)
        buffer = CircleVertexBuffer(segments)
    }


    companion object {

        fun approximateSegments(width: Float, height: Float, maximumAngle: Float = 360f): Int {
            val averageRadius = (width + height) / 4f
            val minSegmentAngle = min(5f, 360f / averageRadius.toRadians())
            return max(3, (maximumAngle / minSegmentAngle).toInt())
        }

    }

    inner class CircleVertexBuffer(private val segments: Int) : VertexBuffer(
        drawTopology = GL_TRIANGLE_FAN,
        // Explanation: Segments + 2 because the first vertex that is the center of the circle is not included in the segment
        // count and we add it twice so that the last vertex connects to the first one.
        vertexCount = segments + 2,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            putVertex(index = 0,
                x = entity.drawWidth / 2f,
                y = entity.drawHeight / 2f
            )

            val startRadians = startAngle.toRadians()
            val endRadians = endAngle.toRadians()
            val deltaAngle = (endRadians - startRadians) / segments

            // The first vertex is the center of the circle.
            for (i in 1..segments + 1) {

                val angle = startRadians + (i - 1) * deltaAngle

                putVertex(index = i,
                    x = entity.drawWidth / 2f + entity.drawWidth / 2f * cos(angle),
                    y = entity.drawHeight / 2f + entity.drawHeight / 2f * sin(angle)
                )
            }
        }

    }
}

