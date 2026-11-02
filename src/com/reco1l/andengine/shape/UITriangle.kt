package com.reco1l.andengine.shape

import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.opengl.font.FontManager
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UITriangle.*
import org.andengine.opengl.util.*

/**
 * A rectangle shape based on [UIComponent].
 */
open class UITriangle : UIBufferedComponent<TriangleVBO>() {

    /**
     * The style of painting for the triangle.
     */
    var paintStyle = PaintStyle.Fill

    /**
     * The line width of the triangle if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f


    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    // The batch needs no buffer; it is only built when the triangle is drawn the legacy way.
    override fun onCreateBuffer(): TriangleVBO? {
        return if (canBatchShape()) null else TriangleVBO()
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

            val cx = width / 2f
            val cy = height / 2f
            val halfWidth = innerWidth / 2f
            val halfHeight = innerHeight / 2f
            val color = QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha)
            val matrix = pGLState.modelViewGLMatrix

            // Same corners as the buffer: apex on top, base at the bottom.
            val ax = cx
            val ay = cy - halfHeight
            val bx = cx - halfWidth
            val by = cy + halfHeight
            val ex = cx + halfWidth

            QuadBatcher.setProjection(pGLState.projectionGLMatrix)
            if (paintStyle == PaintStyle.Fill) {
                ShapeBatching.triangle(matrix, ax, ay, bx, by, ex, by, color, blend)
            } else {
                val w = lineWidth / FontManager.getRasterScale()
                ShapeBatching.triangleOutline(matrix, ax, ay, bx, by, ex, by, w, color, blend)
            }
            return
        }

        ensureBuffer()
        super.doDraw(pGLState, pCamera)
    }

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)
        pGLState.lineWidth(lineWidth)
    }


    class TriangleVBO : VertexBuffer(GL_TRIANGLES, 3, VERTEX_2D) {

        fun update(entity: UITriangle) {
            addTriangle(
                index = 0,
                centerX = entity.width / 2f,
                centerY = entity.height / 2f,
                width = entity.innerWidth,
                height = entity.innerHeight
            )
        }

        override fun topology(entity: UIBufferedComponent<*>): Int {
            return if ((entity as UITriangle).paintStyle == PaintStyle.Fill) GL_TRIANGLES else GL_LINE_LOOP
        }

    }
}


