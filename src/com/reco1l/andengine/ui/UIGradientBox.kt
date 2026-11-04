package com.reco1l.andengine.ui

import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import com.reco1l.andengine.buffered.Buffer
import com.reco1l.andengine.buffered.BufferSharingMode
import com.reco1l.andengine.buffered.UIBufferedComponent
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.shape.ShapeBatching

/**
 * A rectangle that renders a smooth horizontal alpha gradient using per-vertex colors.
 *
 * Batched as a quad with a color per corner; with depth testing or clearing, the per-vertex colors
 * of [GradientVBO] are drawn through the [MeshRenderer].
 *
 * @param fromAlpha  Alpha weight at the LEFT  edge (0–1).
 * @param toAlpha    Alpha weight at the RIGHT edge (0–1).
 */
class UIGradientBox(
    private val fromAlpha: Float = 1f,
    private val toAlpha: Float = 0f,
) : UIBufferedComponent<UIGradientBox.GradientVBO>(batchesShape = true) {

    // Buffer lifecycle

    // The batch needs no buffer; it is only built when the box cannot be batched (see canBatchShape()).
    override fun onCreateBuffer(): GradientVBO? {
        if (canBatchShape()) {
            return null
        }

        return GradientVBO().also {
            it.sharingMode = BufferSharingMode.Dynamic // update() called every frame
        }
    }

    override fun onDrawBatch(gl: DrawContext, blend: BlendMode) {
        // Same corner colors as GradientVBO: the left edge fades with fromAlpha, the right with toAlpha.
        val left = QuadBatcher.packColor(drawRed, drawGreen, drawBlue, fromAlpha * drawAlpha)
        val right = QuadBatcher.packColor(drawRed, drawGreen, drawBlue, toAlpha * drawAlpha)

        QuadBatcher.setProjection(gl.projectionGLMatrix)
        ShapeBatching.gradientRect(gl.modelViewGLMatrix, 0f, 0f, width, height, left, right, left, right, blend)
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    /**
     * Stores 4 vertices in GL_TRIANGLE_STRIP order.
     * Each vertex is 6 floats: (x, y, r, g, b, a).
     *
     * Layout (STRIDE = 24 bytes):
     *   offset  0 : X  (float)
     *   offset  4 : Y  (float)
     *   offset  8 : R  (float, always 1)
     *   offset 12 : G  (float, always 1)
     *   offset 16 : B  (float, always 1)
     *   offset 20 : A  (float, gradient weight × drawAlpha)
     */
    inner class GradientVBO : Buffer(
        capacity = 4 * VERTEX_FLOATS
    ) {

        /** Re-fills the buffer with the current entity size, color, and composed alpha. */
        fun update(entity: UIGradientBox) {
            val w = entity.width
            val h = entity.height
            val r = entity.drawRed
            val g = entity.drawGreen
            val b = entity.drawBlue
            val da = entity.drawAlpha // parent × own alpha, computed per-frame
            val fa = entity.fromAlpha * da // left-edge vertex alpha
            val ta = entity.toAlpha * da // right-edge vertex alpha

            // GL_TRIANGLE_STRIP winding: TL, BL, TR, BR
            putVert(0, 0f, 0f, r, g, b, fa)
            putVert(1, 0f, h,  r, g, b, fa)
            putVert(2, w,  0f, r, g, b, ta)
            putVert(3, w,  h,  r, g, b, ta)
        }

        override fun drawMesh(gl: DrawContext, entity: UIBufferedComponent<*>) {
            MeshRenderer.drawColored(
                PrimitiveType.TriangleStrip, data, 0, 4,
                gl.modelViewGLMatrix, gl.projectionGLMatrix,
                meshBlendMode(), meshDepthTest(), depthInfo.mask
            )
        }

        private fun putVert(idx: Int, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float) {
            val base = idx * VERTEX_FLOATS
            data[base] = x
            data[base + 1] = y
            data[base + 2] = r
            data[base + 3] = g
            data[base + 4] = b
            data[base + 5] = a
        }
    }

    companion object {
        private const val VERTEX_FLOATS = 6 // x, y, r, g, b, a
    }
}