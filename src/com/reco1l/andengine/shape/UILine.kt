package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UILine.*
import com.reco1l.framework.math.Vec2
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

/**
 * A rectangle shape based on [UIComponent].
 */
class UILine : UIBufferedComponent<LineVertexBuffer>() {

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
                requestBufferUpdate()
            }
        }

    /**
     * The ending point of the line.
     */
    var toPoint = Vec2.Zero
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }


    override fun createBuffer(): LineVertexBuffer {
        return LineVertexBuffer()
    }

    override fun canReuseBuffer(buffer: LineVertexBuffer): Boolean {
        return true
    }

    override fun generateBufferCacheKey(): String {
        return "LineVBO@$fromPoint,$toPoint"
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.lineWidth(gl, lineWidth)
    }


    class LineVertexBuffer : VertexBuffer(
        drawTopology = GL_LINES,
        vertexCount = 2,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        fun update(entity: UILine) {
            putVertex(0, entity.fromPoint.x, entity.fromPoint.y)
            putVertex(1, entity.toPoint.x, entity.toPoint.y)
        }
    }

}
