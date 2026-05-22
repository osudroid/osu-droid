package com.reco1l.andengine.ui

import android.opengl.GLES32
import com.reco1l.andengine.buffered.Buffer
import com.reco1l.andengine.buffered.BufferSharingMode
import com.reco1l.andengine.buffered.GL_DYNAMIC_DRAW
import com.reco1l.andengine.buffered.GL_TRIANGLE_STRIP
import com.reco1l.andengine.buffered.UIBufferedComponent
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.GLState

/**
 * A rectangle that renders a smooth horizontal alpha gradient using per-vertex colors.
 *
 * The [org.andengine.opengl.shader.PositionColorShaderProgram] already supports per-vertex RGBA,  normally
 * [com.reco1l.andengine.buffered.UIBufferedComponent] disables the color attribute array and uses a constant *color.
 * This class overrides [onBindShader] to keep the array enabled so GLES can read the
 * gradient alpha values out of the interleaved VBO.
 *
 * @param fromAlpha  Alpha weight at the LEFT  edge (0–1).
 * @param toAlpha    Alpha weight at the RIGHT edge (0–1).
 */
class UIGradientBox(
    private val fromAlpha: Float = 1f,
    private val toAlpha: Float = 0f,
) : UIBufferedComponent<UIGradientBox.GradientVBO>() {

    // Buffer lifecycle

    override fun onCreateBuffer() = GradientVBO().also {
        it.sharingMode = BufferSharingMode.Dynamic // update() called every frame
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    // Shader binding

    override fun onBindShader(pGLState: GLState) {
        val shader = PositionColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(
                shader.uniformMVPMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // Texture coordinates are unused, keep them disabled.
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
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
        capacity = 4 * VERTEX_FLOATS,
        bufferUsage = GL_DYNAMIC_DRAW,
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

        private fun putVert(idx: Int, x: Float, y: Float, r: Float, g: Float, b: Float, a: Float) {
            val base = idx * VERTEX_FLOATS
            mFloatBuffer.put(base, x)
            mFloatBuffer.put(base + 1, y)
            mFloatBuffer.put(base + 2, r)
            mFloatBuffer.put(base + 3, g)
            mFloatBuffer.put(base + 4, b)
            mFloatBuffer.put(base + 5, a)
        }

        // Called once per frame before drawing (Dynamic sharing mode).
        override fun beginDraw(gl: GLState) {
            bindAndUpload()

            // Position attribute: 2 floats, stride=24, byte-offset 0
            GLES32.glVertexAttribPointer(
                ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
                2, GLES32.GL_FLOAT, false, STRIDE_BYTES, 0
            )
            GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION)

            // Color attribute: 4 floats, stride=24, byte-offset 8 (skip x,y)
            GLES32.glVertexAttribPointer(
                ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION,
                4, GLES32.GL_FLOAT, false, STRIDE_BYTES, 8
            )
            GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        }

        override fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>) { /* using beginDraw */ }

        override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
            GLES32.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        }
    }

    companion object {
        private const val VERTEX_FLOATS = 6 // x, y, r, g, b, a
        private const val STRIDE_BYTES = VERTEX_FLOATS * 4 // 24
    }
}