package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.framework.*
import com.reco1l.toolkt.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import kotlin.math.*

open class BezelOutline(cornerRadius: Float = 0f) : BufferedEntity<BezelOutline.BezelOutlineVB>() {

    /**
     * The corner radius of the rectangle.
     */
    var cornerRadius = cornerRadius
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer()
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


    override var bufferInvalidationFlags = InvalidateDataOnSizeChanged or RebuildBufferOnSizeChanged


    override fun onRebuildBuffer(gl: GL10) {
        val cornerRadius = cornerRadius.coerceIn(0f, min(width, height) / 2f)
        val segmentsPerArc = Circle.approximateSegments(cornerRadius, cornerRadius, 90f)
        buffer = BezelOutlineVB(segmentsPerArc)
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        gl.glLineWidth(1f)
    }

    override fun onApplyColor(gl: GL10) {
        // Applying at buffer level
    }


    class BezelOutlineVB(private val segmentsPerArc: Int) : VertexBuffer(
        drawTopology = GL_LINES,
        vertexCount = LINE_VERTICES * LINE_COUNT + segmentsPerArc * ARC_COUNT,
        vertexSize = VERTEX_2D,
        bufferUsage = GL11.GL_STATIC_DRAW
    ) {

        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            entity as BezelOutline

            val width = entity.width
            val height = entity.height
            val cornerRadius = entity.cornerRadius.coerceIn(0f, min(width, height) / 2f)

            var position = 0

            fun addLine(fromX: Float, fromY: Float, toX: Float, toY: Float) {
                putVertex(position++, fromX, fromY)
                putVertex(position++, toX, toY)
            }

            fun addArc(centerX: Float, centerY: Float, startAngle: Float, endAngle: Float) {

                val startRadians = startAngle.toRadians()
                val endRadians = endAngle.toRadians()

                val deltaAngle = (endRadians - startRadians) / segmentsPerArc

                for (i in 0 ..< segmentsPerArc) {

                    val angle = startRadians + i * deltaAngle

                    val x = centerX + cornerRadius * cos(angle)
                    val y = centerY + cornerRadius * sin(angle)

                    putVertex(position++, x, y)
                }
            }

            val halfLineWidth = 0.5f
            val arcCenter = cornerRadius + halfLineWidth

            // Top bezel
            val top = halfLineWidth

            addArc(
                centerX = arcCenter, centerY = arcCenter,
                startAngle = -180f,
                endAngle = -90f
            )
            addLine(
                fromX = cornerRadius + halfLineWidth,
                fromY = top,
                toX = width - cornerRadius,
                toY = top
            )

            // Bottom bezel
            val bottom = height - halfLineWidth

            addArc(
                centerX = width - arcCenter,
                centerY = height - arcCenter,
                startAngle = 0f,
                endAngle = 90f
            )
            addLine(
                fromX = width - arcCenter,
                fromY = bottom,
                toX = arcCenter,
                toY = bottom
            )
        }

        override fun draw(gl: GL10, entity: BufferedEntity<*>) {
            entity as BezelOutline

            GLHelper.setColor(gl, entity.topColor.red, entity.topColor.green, entity.topColor.blue, entity.topColor.alpha)
            gl.glDrawArrays(drawTopology, 0, vertexCount / 2)

            GLHelper.setColor(gl, entity.bottomColor.red, entity.bottomColor.green, entity.bottomColor.blue, entity.bottomColor.alpha)
            gl.glDrawArrays(drawTopology, vertexCount / 2, vertexCount / 2)
        }


        companion object {
            const val LINE_VERTICES = 2
            const val LINE_COUNT = 2
            const val ARC_COUNT = 2
        }
    }
}