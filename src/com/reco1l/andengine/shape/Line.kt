package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.Line.*
import com.reco1l.framework.math.Vec2
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
class Line : BufferedEntity<LineVertexBuffer>(LineVertexBuffer()) {

    /**
     * The width of the line.
     */
    var lineWidth = 1f

    /**
     * The starting point of the line.
     */
    var fromPoint = Vec2.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer()
            }
        }

    /**
     * The ending point of the line.
     */
    var toPoint = Vec2.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer()
            }
        }


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        gl.glLineWidth(lineWidth)
    }


    class LineVertexBuffer : VertexBuffer(
        drawTopology = GL_LINES,
        vertexCount = 2,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            entity as Line
            val fromPoint = entity.fromPoint
            val toPoint = entity.toPoint

            putVertex(
                index = 0,
                x = fromPoint.x,
                y = fromPoint.y
            )
            putVertex(
                index = 1,
                x = toPoint.x,
                y = toPoint.y
            )
        }
    }

}
