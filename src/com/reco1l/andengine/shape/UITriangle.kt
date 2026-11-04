package com.reco1l.andengine.shape

import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UITriangle.*
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext

/**
 * A rectangle shape based on [UIComponent].
 */
open class UITriangle : UIBufferedComponent<TriangleVBO>(batchesShape = true) {

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

    // The batch needs no buffer; it is only built when the triangle cannot be batched (see canBatchShape()).
    override fun onCreateBuffer(): TriangleVBO? {
        return if (canBatchShape()) null else TriangleVBO()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun onDrawBatch(gl: DrawContext, blend: BlendMode) {
        val cx = width / 2f
        val cy = height / 2f
        val halfWidth = innerWidth / 2f
        val halfHeight = innerHeight / 2f
        val color = QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha)
        val matrix = gl.modelViewGLMatrix

        // Same corners as the buffer: apex on top, base at the bottom.
        val ax = cx
        val ay = cy - halfHeight
        val bx = cx - halfWidth
        val by = cy + halfHeight
        val ex = cx + halfWidth

        QuadBatcher.setProjection(gl.projectionGLMatrix)
        if (paintStyle == PaintStyle.Fill) {
            ShapeBatching.triangle(matrix, ax, ay, bx, by, ex, by, color, blend)
        } else {
            val w = ShapeBatching.lineWidthFromPixels(lineWidth)
            ShapeBatching.triangleOutline(matrix, ax, ay, bx, by, ex, by, w, color, blend)
        }
    }

    override fun beginDraw(drawContext: DrawContext) {
        super.beginDraw(drawContext)
        drawContext.lineWidth(lineWidth)
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


