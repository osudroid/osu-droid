package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UILine.*
import com.reco1l.framework.math.Vec2
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching

/**
 * A rectangle shape based on [UIComponent].
 */
class UILine : UIBufferedComponent<LineVertexBuffer>(batchesShape = true) {

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


    // The batch needs no buffer; it is only built when the line cannot be batched (see canBatchShape()).
    override fun onCreateBuffer(): LineVertexBuffer? {
        return if (canBatchShape()) null else LineVertexBuffer()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun onDrawBatch(gl: DrawContext, blend: BlendMode) {
        QuadBatcher.setProjection(gl.projectionGLMatrix)
        ShapeBatching.line(
            gl.modelViewGLMatrix,
            fromPoint.x, fromPoint.y, toPoint.x, toPoint.y,
            ShapeBatching.lineWidthFromPixels(lineWidth),
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha), blend
        )
    }

    override fun beginDraw(pGLState: DrawContext) {
        super.beginDraw(pGLState)
        pGLState.lineWidth(lineWidth)
    }


    class LineVertexBuffer : VertexBuffer(
        drawTopology = GL_LINES,
        vertexCount = 2,
        vertexSize = VERTEX_2D
    ) {
        fun update(entity: UILine) {
            putVertex(0, entity.fromPoint.x, entity.fromPoint.y)
            putVertex(1, entity.toPoint.x, entity.toPoint.y)
        }
    }

}
