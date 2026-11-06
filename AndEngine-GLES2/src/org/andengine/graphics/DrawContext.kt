/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 18:00:43 - 08.03.2010
 */
package org.andengine.graphics

import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.util.GLMatrixStack

/**
 * What drawing code gets while it draws: the model-view and projection matrix stacks (kept on the
 * CPU) and the few pieces of render state it switches itself, the scissor test, the depth test and
 * the line width. State changes go through the backend's state cache, so queued batches are drawn
 * before a change they depend on. Render thread only; nothing here allocates.
 */
class DrawContext {

    private val modelViewStack = GLMatrixStack()
    private val projectionStack = GLMatrixStack()

    private val modelView = FloatArray(GLMatrixStack.GLMATRIX_SIZE)
    private val projection = FloatArray(GLMatrixStack.GLMATRIX_SIZE)

    /** The current model-view matrix, column-major. The same array every time, overwritten on the next read. */
    val modelViewGLMatrix: FloatArray
        get() {
            modelViewStack.getMatrix(modelView)
            return modelView
        }

    /** The current projection matrix, column-major. The same array every time, overwritten on the next read. */
    val projectionGLMatrix: FloatArray
        get() {
            projectionStack.getMatrix(projection)
            return projection
        }

    fun pushModelViewGLMatrix() = modelViewStack.glPushMatrix()

    fun popModelViewGLMatrix() = modelViewStack.glPopMatrix()

    fun loadModelViewGLMatrixIdentity() = modelViewStack.glLoadIdentity()

    fun translateModelViewGLMatrixf(x: Float, y: Float, z: Float) = modelViewStack.glTranslatef(x, y, z)

    fun rotateModelViewGLMatrixf(angle: Float, x: Float, y: Float, z: Float) = modelViewStack.glRotatef(angle, x, y, z)

    fun scaleModelViewGLMatrixf(scaleX: Float, scaleY: Float, scaleZ: Float) =
        modelViewStack.glScalef(scaleX, scaleY, scaleZ)

    fun skewModelViewGLMatrixf(skewX: Float, skewY: Float) = modelViewStack.glSkewf(skewX, skewY)

    fun resetModelViewGLMatrixStack() = modelViewStack.reset()

    fun pushProjectionGLMatrix() = projectionStack.glPushMatrix()

    fun popProjectionGLMatrix() = projectionStack.glPopMatrix()

    fun loadProjectionGLMatrixIdentity() = projectionStack.glLoadIdentity()

    fun translateProjectionGLMatrixf(x: Float, y: Float, z: Float) = projectionStack.glTranslatef(x, y, z)

    fun rotateProjectionGLMatrixf(angle: Float, x: Float, y: Float, z: Float) = projectionStack.glRotatef(angle, x, y, z)

    fun orthoProjectionGLMatrixf(left: Float, right: Float, bottom: Float, top: Float, zNear: Float, zFar: Float) =
        projectionStack.glOrthof(left, right, bottom, top, zNear, zFar)

    fun resetProjectionGLMatrixStack() = projectionStack.reset()

    val isScissorTestEnabled: Boolean
        get() = GlesBackend.state.isScissorEnabled

    /** @return the previous state. */
    fun enableScissorTest(): Boolean {
        val state = GlesBackend.state
        val previous = state.isScissorEnabled
        state.setScissorEnabled(true)
        return previous
    }

    /** @return the previous state. */
    fun disableScissorTest(): Boolean {
        val state = GlesBackend.state
        val previous = state.isScissorEnabled
        state.setScissorEnabled(false)
        return previous
    }

    /** @return the previous state. */
    fun disableDepthTest(): Boolean {
        val state = GlesBackend.state
        val previous = state.isDepthTestEnabled
        state.setDepthTestEnabled(false)
        return previous
    }

    fun lineWidth(width: Float) = GlesBackend.state.lineWidth(width)

    /**
     * Clears [buffers] of the current target, after the pending batched quads are drawn.
     */
    fun clear(buffers: ClearBuffers) {
        if (buffers.isNone) return
        QuadBatcher.flush()
        GlesBackend.state.clear(buffers)
    }
}
