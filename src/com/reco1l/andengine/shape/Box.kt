package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import com.reco1l.toolkt.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.RectangleVertexBuffer
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import kotlin.math.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
open class Box : ExtendedEntity(vertexBuffer = BoxVertexBuffer()) {


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as BoxVertexBuffer).update(width, height)
    }

    override fun drawVertices(gl: GL10, camera: Camera) {
        gl.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }


    class BoxVertexBuffer : VertexBuffer(4 * 2, GL11.GL_STATIC_DRAW, false) {

        fun update(width: Float, height: Float) {
            floatBuffer.apply {
                put(0, 0f)
                put(1, 0f)

                put(2, 0f)
                put(3, height)

                put(4, width)
                put(5, 0f)

                put(6, width)
                put(7, height)
            }
        }

    }

}


open class RoundedBox(segmentsPerArc: Int = 10) : ExtendedEntity(RoundedBoxVertexBuffer(segmentsPerArc)) {

    /**
     * The corner radius of the rectangle.
     */
    var cornerRadius = 0f
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    private var shouldRebuildVertexBuffer = true


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

        val cornerRadius = cornerRadius.coerceIn(0f, min(width, height) / 2f)

        if (shouldRebuildVertexBuffer) {
            shouldRebuildVertexBuffer = false

            // In this case for all 4 arcs the angle range is 90°.
            val segmentsPerArc = Circle.approximateSegments(cornerRadius, cornerRadius, 0f, 90f)

            setVertexBuffer(RoundedBoxVertexBuffer(segmentsPerArc))
        }

        (vertexBuffer as RoundedBoxVertexBuffer).update(width, height, cornerRadius)
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        (vertexBuffer as RoundedBoxVertexBuffer).draw(pGL)
    }


    class RoundedBoxVertexBuffer(private val segmentsPerArc: Int) : VertexBuffer(

        (5 /*Quads*/ * 4 + (segmentsPerArc + 2) /*Arcs*/ * 4) * 2,

        GL11.GL_STATIC_DRAW, false
    ) {

        fun update(width: Float, height: Float, cornerRadius: Float) {

            val buffer = floatBuffer

            var index = 0

            fun addQuad(fromX: Float, fromY: Float, toX: Float, toY: Float) {

                buffer.put(index++, fromX)
                buffer.put(index++, fromY)

                buffer.put(index++, fromX)
                buffer.put(index++, toY)

                buffer.put(index++, toX)
                buffer.put(index++, fromY)

                buffer.put(index++, toX)
                buffer.put(index++, toY)
            }

            // Quads:
            //     [1]
            // [4] [5] [2]
            //     [3]

            // [1]
            addQuad(
                fromX = cornerRadius, fromY = 0f,
                toX = width - cornerRadius, toY = cornerRadius
            )

            // [2]
            addQuad(
                fromX = width - cornerRadius, fromY = cornerRadius,
                toX = width, toY = height - cornerRadius
            )

            // [3]
            addQuad(
                fromX = cornerRadius, fromY = height - cornerRadius,
                toX = width - cornerRadius, toY = height
            )

            // [4]
            addQuad(
                fromX = 0f, fromY = cornerRadius,
                toX = cornerRadius, toY = height - cornerRadius
            )

            // [5]
            addQuad(
                fromX = cornerRadius, fromY = cornerRadius,
                toX = width - cornerRadius, toY = height - cornerRadius
            )


            // Arcs

            fun addArc(centerX: Float, centerY: Float, startAngle: Float, endAngle: Float) {

                val startRadians = startAngle.toRadians()
                val endRadians = endAngle.toRadians()

                val deltaAngle = (endRadians - startRadians) / segmentsPerArc

                buffer.put(index + 0, centerX)
                buffer.put(index + 1, centerY)

                // The first vertex is the center of the circle.
                for (i in 1..segmentsPerArc + 1) {

                    val angle = startRadians + (i - 1) * deltaAngle

                    val x = centerX + cornerRadius * cos(angle)
                    val y = centerY + cornerRadius * sin(angle)

                    buffer.put(index + i * 2 + 0, x)
                    buffer.put(index + i * 2 + 1, y)
                }

                index += (segmentsPerArc + 2) * 2
            }

            // Top left corner (180° to 270°)
            addArc(cornerRadius, cornerRadius, -180f, -90f)

            // Top right corner (270° to 360°)
            addArc(width - cornerRadius, cornerRadius, -90f, 0f)

            // Bottom right corner (0° to 90°)
            addArc(width - cornerRadius, height - cornerRadius, 0f, 90f)

            // Bottom left corner (90° to 180°)
            addArc(cornerRadius, height - cornerRadius, 90f, 180f)
        }


        fun draw(gl: GL10) {

            var offset = 0

            // Quads
            for (i in 0 until 5) {
                gl.glDrawArrays(GL_TRIANGLE_STRIP, offset, 4)
                offset += 4
            }

            val verticesPerArc = segmentsPerArc + 2

            // Arcs
            for (i in 0 until 4) {
                gl.glDrawArrays(GL_TRIANGLE_FAN, offset, verticesPerArc)
                offset += verticesPerArc
            }
        }

    }

}