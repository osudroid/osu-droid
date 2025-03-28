package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.RoundedBox.*
import com.reco1l.toolkt.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import kotlin.math.*

open class RoundedBox : BufferedEntity<RoundedBoxVertexBuffer>() {

    /**
     * The corner radius of the rectangle.
     */
    var cornerRadius = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer()
            }

        }


    override var invalidationFlags = InvalidateDataOnSizeChanged or RebuildBufferOnSizeChanged


    override fun onRebuildBuffer(gl: GL10) {
        val cornerRadius = cornerRadius.coerceIn(0f, min(drawWidth, drawHeight) / 2f)
        val segmentsPerArc = Circle.approximateSegments(cornerRadius, cornerRadius, 90f)
        buffer = RoundedBoxVertexBuffer(segmentsPerArc)
    }

    class RoundedBoxVertexBuffer(private val segmentsPerArc: Int) : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = QUAD_COUNT * QUAD_VERTICES + (segmentsPerArc + 2) * ARC_COUNT,
        vertexSize = VERTEX_2D,
        bufferUsage = GL11.GL_STATIC_DRAW
    ) {

        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            entity as RoundedBox

            val width = entity.drawWidth
            val height = entity.drawHeight
            val cornerRadius = entity.cornerRadius.coerceIn(0f, min(width, height) / 2f)

            var vertIndex = 0
            var position = 0

            fun addQuad(fromX: Float, fromY: Float, toX: Float, toY: Float) {

                putVertex(position++, fromX, fromY)
                putVertex(position++, fromX, toY)
                putVertex(position++, toX, fromY)
                putVertex(position++, toX, toY)
            }

            // Quads:
            //     [ ]
            // [1] [2] [3]
            //     [ ]

            // [1]
            addQuad(
                fromX = 0f, fromY = cornerRadius,
                toX = cornerRadius, toY = height - cornerRadius
            )

            // [2]
            addQuad(
                fromX = cornerRadius, fromY = 0f,
                toX = width - cornerRadius, toY = height
            )

            // [3]
            addQuad(
                fromX = width - cornerRadius, fromY = cornerRadius,
                toX = width, toY = height - cornerRadius
            )

            // Arcs

            fun addArc(centerX: Float, centerY: Float, startAngle: Float, endAngle: Float) {

                val startRadians = startAngle.toRadians()
                val endRadians = endAngle.toRadians()

                val deltaAngle = (endRadians - startRadians) / segmentsPerArc
                putVertex(position, centerX, centerY)

                // The first vertex is the center of the circle.
                for (i in 1..segmentsPerArc + 1) {

                    val angle = startRadians + (i - 1) * deltaAngle

                    val x = centerX + cornerRadius * cos(angle)
                    val y = centerY + cornerRadius * sin(angle)

                    putVertex(position + i, x, y)
                }

                position += segmentsPerArc + 2
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


        override fun draw(gl: GL10, entity: BufferedEntity<*>) {

            var offset = 0

            // Quads
            for (i in 0 until 3) {
                gl.glDrawArrays(GL_TRIANGLE_STRIP, offset, QUAD_VERTICES)
                offset += 4
            }

            val verticesPerArc = segmentsPerArc + VERTEX_2D

            // Arcs
            for (i in 0 until 4) {
                gl.glDrawArrays(GL_TRIANGLE_FAN, offset, verticesPerArc)
                offset += verticesPerArc
            }
        }

        companion object {
            const val QUAD_VERTICES = 4
            const val QUAD_COUNT = 3
            const val ARC_COUNT = 4
        }
    }
}