package com.reco1l.andengine.buffered

import org.andengine.graphics.gles.GlesBackend
import android.opengl.GLES32
import com.reco1l.andengine.component.*
import org.andengine.engine.camera.Camera
import org.andengine.graphics.BlendMode
import org.andengine.graphics.gles.GlesPipeline
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
                // Shared buffers are owned elsewhere.
                field?.let { if (it.sharingMode == BufferSharingMode.Off) it.finalize() }
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

    /**
     * The blend function this entity draws with: [blendInfo], or for [BlendInfo.Inherit] the
     * parent's (the default blend function if the parent is not a buffered component).
     */
    protected val resolvedBlendInfo: BlendInfo
        get() {
            val info = blendInfo
            if (info != BlendInfo.Inherit) return info
            val parent = parent
            val inherited = if (parent is UIBufferedComponent<*>) parent.blendInfo else DEFAULT_BLEND_INFO
            return if (inherited != BlendInfo.Inherit) inherited else DEFAULT_BLEND_INFO
        }

    /** The GL buffer bits [clearInfo] asks to clear before drawing. */
    protected fun clearMask(): Int {
        var mask = 0
        if (clearInfo.depthBuffer) mask = mask or GLES32.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) mask = mask or GLES32.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) mask = mask or GLES32.GL_STENCIL_BUFFER_BIT
        return mask
    }

    // The batch blend mode of [batchBlendSource]; recomputed only when the blend info changes.
    private var batchBlendSource: BlendInfo? = null
    private var batchBlend: BlendMode? = null

    /**
     * The quad batch blend mode of [resolvedBlendInfo], or null if its factors cannot be batched.
     */
    protected fun batchBlendMode(): BlendMode? {
        val info = resolvedBlendInfo
        if (info !== batchBlendSource) {
            batchBlendSource = info
            batchBlend = GlesPipeline.blendModeOf(info.sourceFactor, info.destinationFactor)
        }
        return batchBlend
    }

    /**
     * Whether this entity's current depth, clear and blend state can be drawn through the batch: no
     * depth test, no clearing and a blend function the batch can express. Shapes that fail this are
     * drawn through their buffer.
     *
     * A batched shape does not run `beginDraw` or `onBindShader` and owns no buffer of its own, so
     * do not assign shared VBOs to shapes that can be batched.
     */
    protected fun canBatchShape(): Boolean {
        if (depthInfo.test) return false
        val clear = clearInfo
        if (clear.depthBuffer || clear.colorBuffer || clear.stencilBuffer) return false
        return batchBlendMode() != null
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


    /**
     * Builds the buffer now if there is none, for entities that skip it while they are drawn through
     * the batch ([onCreateBuffer] returning null) and meet a draw that needs it.
     */
    protected fun ensureBuffer() {
        if (buffer != null) return
        needsNewBuffer = false
        buffer = onCreateBuffer()
        needsBufferUpdate = false
        val created = buffer
        if (created != null && created.sharingMode != BufferSharingMode.Dynamic) {
            updateBuffer()
        }
    }

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
        GlesBackend.state.enableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GlesBackend.state.enableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
        // Leave no array buffer bound after this draw. Buffer.bindAndUpload() binds through
        // GlesBackend.state, the same cache GLState forwards to, so there is no stale cache entry
        // to repair any more and this reset is redundant; it is kept until Step 3 replaces the
        // buffer handling, so the binding left behind stays the same as before.
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
        val clearMask = clearMask()
        if (clearMask != 0) {
            GlesBackend.state.clear(clearMask)
        }

        // Depth testing
        if (depthInfo.test) {
            GlesBackend.state.depthFunc(depthInfo.function)
            GlesBackend.state.depthMask(depthInfo.mask)
            pGLState.enableDepthTest()
        } else {
            pGLState.disableDepthTest()
        }

        // Blending
        val blend = resolvedBlendInfo

        pGLState.enableBlend()
        pGLState.blendFunction(blend.sourceFactor, blend.destinationFactor)

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
        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(
                shader.uniformMVPMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // Provide color as constant vertex attribute (disable per-vertex array)
        GlesBackend.state.disableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GlesBackend.state.vertexAttrib4f(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, drawRed, drawGreen, drawBlue, drawAlpha)

        // Disable texture coordinates (not needed for solid-color shapes)
        GlesBackend.state.disableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
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


    private companion object {
        val DEFAULT_BLEND_INFO = BlendInfo(BLENDFUNCTION_SOURCE_DEFAULT, BLENDFUNCTION_DESTINATION_DEFAULT)
    }

}