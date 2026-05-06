package com.reco1l.andengine.buffered

import android.opengl.GLES20
import com.reco1l.andengine.component.*
import org.andengine.engine.camera.Camera
import org.andengine.entity.shape.Shape.*
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.*


/**
 * An entity that uses a buffer to draw itself.
 */
abstract class UIBufferedComponent<T: IBuffer> : UIComponent() {

    /**
     * The buffer itself.
     */
    open var buffer: T? = null
        set(value) {
            if (field != value) {
                field?.finalize()
                field = value
            }
        }


    /**
     * The blend information of the entity.
     */
    var blendInfo = BlendInfo.Mixture

    /**
     * The depth information of the entity.
     */
    var depthInfo = DepthInfo.None

    /**
     * The clear information of the entity.
     */
    var clearInfo = ClearInfo.None


    /**
     * Indicates whether a new buffer needs to be created.
     */
    protected var needsNewBuffer = true
        private set

    /**
     * Indicates whether the buffer needs to be updated.
     */
    protected var needsBufferUpdate = true
        private set


    fun setBlendFunction(source: Int, destination: Int) {
        blendInfo = BlendInfo(source, destination)
    }


    //region Buffer lifecycle

    /**
     * Called when the buffer needs to be built.
     */
    protected abstract fun onCreateBuffer(): T?

    /**
     * Called when the buffer needs to be updated.
     */
    abstract fun onUpdateBuffer()


    fun requestNewBuffer() {
        needsNewBuffer = true
    }

    fun requestBufferUpdate() {
        needsBufferUpdate = true
    }

    //endregion

    //region Draw pipeline

    override fun onHandleInvalidations(restoreFlags: Boolean) {

        if (needsNewBuffer) {
            needsNewBuffer = false
            buffer = onCreateBuffer()
            requestBufferUpdate()
        }

        super.onHandleInvalidations(restoreFlags)

        // Buffer update is done after invalidations are handled so we can
        // refer the buffer in those invalidations.
        if (needsBufferUpdate) {
            needsBufferUpdate = false

            val buffer = buffer

            // If the buffer is shared and its sharing mode is dynamic, we
            // need to update it before drawing every frame, this might be
            // CPU intensive, but the memory consumption is lower.
            if (buffer != null && buffer.sharingMode != BufferSharingMode.Dynamic) {
                updateBuffer()
            }
        }
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        super.doDraw(pGLState, pCamera)
        onDeclarePointers(pGLState)
        onDrawBuffer(pGLState)
        // Restore vertex attribute array state so old AndEngine Sprite rendering is not broken.
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
        // Reset GL buffer binding through GLState to keep its internal cache in sync with the
        // actual GL state. The new Buffer.bindAndUpload() calls GLES20.glBindBuffer() directly,
        // bypassing GLState.mCurrentArrayBufferID. Without this reset, legacy VBO binding would
        // skip the actual glBindBuffer call (false cache hit), causing sprites to render from the
        // wrong VBO.
        pGLState.bindArrayBuffer(0)

        // Disable depth test after drawing so subsequent entities are not accidentally
        // depth-tested against values we wrote (same fix as in UITriangleMesh).
        if (depthInfo.test) {
            pGLState.disableDepthTest()
        }
    }

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)

        // Clearing
        var clearMask = 0

        if (clearInfo.depthBuffer) clearMask = clearMask or GLES20.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) clearMask = clearMask or GLES20.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) clearMask = clearMask or GLES20.GL_STENCIL_BUFFER_BIT

        if (clearMask != 0) {
            GLES20.glClear(clearMask)
        }

        // Depth testing
        if (depthInfo.test) {
            GLES20.glDepthFunc(depthInfo.function)
            GLES20.glDepthMask(depthInfo.mask)
            pGLState.enableDepthTest()
        } else {
            pGLState.disableDepthTest()
        }

        // Blending
        var sourceFactor = BLENDFUNCTION_SOURCE_DEFAULT
        var destinationFactor = BLENDFUNCTION_DESTINATION_DEFAULT

        if (blendInfo == BlendInfo.Inherit) {
            val parent = parent

            if (parent is UIBufferedComponent<*>) {
                sourceFactor = parent.blendInfo.sourceFactor
                destinationFactor = parent.blendInfo.destinationFactor
            }
        } else {
            sourceFactor = blendInfo.sourceFactor
            destinationFactor = blendInfo.destinationFactor
        }

        pGLState.enableBlend()
        pGLState.blendFunction(sourceFactor, destinationFactor)

        // If it's a shared buffer, we might need to update it before drawing.
        if (buffer?.sharingMode == BufferSharingMode.Dynamic) {
            updateBuffer()
        }

        buffer?.beginDraw(pGLState)

        onBindShader(pGLState)
    }

    /**
     * Called during [beginDraw] after the position buffer is set up.
     * Subclasses should bind the appropriate shader and upload uniforms.
     * Default implementation uses [PositionColorShaderProgram] with a constant vertex color.
     */
    protected open fun onBindShader(pGLState: GLState) {
        val shader = PositionColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        // Upload MVP matrix
        if (PositionColorShaderProgram.sUniformModelViewPositionMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(
                PositionColorShaderProgram.sUniformModelViewPositionMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // Provide color as constant vertex attribute (disable per-vertex array)
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES20.glVertexAttrib4f(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, drawRed, drawGreen, drawBlue, drawAlpha)

        // Disable texture coordinates (not needed for solid-color shapes)
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
    }

    private fun updateBuffer() {
        onUpdateBuffer()
        buffer?.invalidateOnHardware()
    }

    protected open fun onDeclarePointers(gl: GLState) {
        buffer?.declarePointers(gl, this)
    }

    protected open fun onDrawBuffer(gl: GLState) {
        buffer?.draw(gl, this)
    }

    //endregion


    override fun reset() {
        super.reset()
        blendInfo = BlendInfo.Mixture
    }

    override fun finalize() {
        buffer?.finalize()
    }

}