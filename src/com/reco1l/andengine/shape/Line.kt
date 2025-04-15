package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.Line.*
import com.reco1l.framework.math.Vec2
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
class Line : BufferedEntity<LineVertexBuffer>() {

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
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }

    /**
     * The ending point of the line.
     */
    var toPoint = Vec2.Zero
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }


    override fun onCreateBuffer(gl: GL10): LineVertexBuffer {
        return LineVertexBuffer()
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.lineWidth(gl, lineWidth)
    }


    inner class LineVertexBuffer : VertexBuffer(
        drawTopology = GL_LINES,
        vertexCount = 2,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {
            putVertex(0, fromPoint.x, fromPoint.y)
            putVertex(1, toPoint.x, toPoint.y)
        }
    }

}
