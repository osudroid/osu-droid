package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.UILine.*
import com.reco1l.framework.math.Vec2
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.util.GLState

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


    // The batch needs no buffer; it is only built when the line is drawn the legacy way.
    override fun onCreateBuffer(): LineVertexBuffer? {
        return if (canBatchShape()) null else LineVertexBuffer()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        // Invisible shapes still reach the buffer path: they serve as depth masks there.
        val blend = if (canBatchShape()) batchBlendMode() else null
        if (blend != null) {
            onApplyColor(pGLState)

            if (drawAlpha == 0f) {
                return
            }

            QuadBatcher.setProjection(pGLState.projectionGLMatrix)
            ShapeBatching.line(
                pGLState.modelViewGLMatrix,
                fromPoint.x, fromPoint.y, toPoint.x, toPoint.y,
                lineWidth / FontManager.getRasterScale(),
                QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha), blend
            )
            return
        }

        ensureBuffer()
        super.doDraw(pGLState, pCamera)
    }

    override fun beginDraw(pGLState: GLState) {
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
