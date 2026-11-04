package com.reco1l.andengine.buffered

import org.andengine.graphics.gles.GlesBackend
import android.opengl.GLES32
import com.reco1l.andengine.component.*
import org.andengine.engine.camera.Camera
import org.andengine.graphics.BlendMode
import org.andengine.graphics.CompareFunction
import org.andengine.graphics.DrawContext
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesPipeline
import org.andengine.entity.shape.Shape.*


/**
 * An entity that uses a buffer to draw itself.
 *
 * Entities that cannot go through the quad batch (depth testing, clearing) draw their buffer
 * through the [MeshRenderer] ([IBuffer.drawMesh]); entities with a program of their own override
 * [onDrawBuffer] and draw the buffer's data through [MeshRenderer.drawProgram].
 *
 * @param batchesShape whether this entity is a shape that [doDraw] sends through the quad batch
 * ([onDrawBatch]) whenever [canBatchShape] allows, building its buffer only for the other draws.
 */
abstract class UIBufferedComponent<T: IBuffer>(private val batchesShape: Boolean = false) : UIComponent() {

    /**
     * The buffer itself.
     */
    open var buffer: T? = null


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

    /**
     * Clears the buffers [clearInfo] asks for. Entities whose [doDraw] skips [beginDraw] call this
     * themselves.
     */
    protected fun clearIfRequested() {
        val clearMask = clearMask()
        if (clearMask == 0) return
        // Pending quads belong before the clear; and a clear ignores the depth buffer while the
        // depth mask is off, which the mesh renderer's state restore can leave behind.
        QuadBatcher.flush()
        if (clearInfo.depthBuffer) GlesBackend.state.depthMask(true)
        GlesBackend.state.clear(clearMask)
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
     * The blend mode for draws that must happen whatever the blend factors (the mesh renderer, and
     * text, which is always batched): the one of [resolvedBlendInfo], or straight alpha for factors
     * without an engine equivalent (the UI uses none).
     */
    protected fun meshBlendMode(): BlendMode = batchBlendMode() ?: BlendMode.Alpha

    /** The depth compare function of [depthInfo] for the mesh renderer; null without depth test. */
    protected fun meshDepthTest(): CompareFunction? {
        val depth = depthInfo
        return if (depth.test) GlesPipeline.compareFunctionOf(depth.function) else null
    }

    /**
     * Draws [vertexCount] positions (x, y pairs from [offset] on) of this entity's buffer as
     * [primitive] through the [MeshRenderer], with the draw color, blend and depth settings of this
     * entity and the current model-view and projection of [gl]. Transparent entities draw as well:
     * they still write depth when [depthInfo] asks for it.
     */
    fun drawPositions(gl: DrawContext, primitive: PrimitiveType, positions: FloatArray, offset: Int, vertexCount: Int) {
        MeshRenderer.draw(
            primitive, positions, offset, vertexCount,
            gl.modelViewGLMatrix, gl.projectionGLMatrix,
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            meshBlendMode(), meshDepthTest(), depthInfo.mask
        )
    }

    /**
     * Whether this entity's current depth, clear and blend state can be drawn through the batch: no
     * depth test, no clearing and a blend function the batch can express. Shapes that fail this are
     * drawn through their buffer.
     *
     * A batched shape does not run `beginDraw` and owns no buffer of its own, so do not assign
     * shared buffers to shapes that can be batched. Invisible shapes still take the buffer path
     * when they cannot be batched: they serve as depth masks there.
     */
    protected fun canBatchShape(): Boolean {
        if (depthInfo.test) return false
        val clear = clearInfo
        if (clear.depthBuffer || clear.colorBuffer || clear.stencilBuffer) return false
        return batchBlendMode() != null
    }


    //region Buffer lifecycle

    /**
     * Called when the buffer needs to be built; null leaves the entity without a buffer, which
     * entities drawn only through the batch do by default.
     */
    protected open fun onCreateBuffer(): T? = null

    /**
     * Called when the buffer needs to be updated.
     */
    open fun onUpdateBuffer() = Unit


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
            onUpdateBuffer()
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

    private fun createBufferIfNeeded() {
        if (needsNewBuffer) {
            needsNewBuffer = false
            buffer = onCreateBuffer()
            requestBufferUpdate()
        }
    }

    override fun onHandleInvalidations(restoreFlags: Boolean) {

        createBufferIfNeeded()

        super.onHandleInvalidations(restoreFlags)

        // Size changes can request a new buffer while the invalidations are handled.
        createBufferIfNeeded()

        // Buffer update is done after invalidations are handled so we can
        // refer the buffer in those invalidations.
        if (needsBufferUpdate) {
            needsBufferUpdate = false

            val buffer = buffer

            // If the buffer is shared and its sharing mode is dynamic, we
            // need to update it before drawing every frame, this might be
            // CPU intensive, but the memory consumption is lower.
            if (buffer != null && buffer.sharingMode != BufferSharingMode.Dynamic) {
                onUpdateBuffer()
            }
        }
    }

    override fun doDraw(drawContext: DrawContext, pCamera: Camera) {
        if (batchesShape) {
            val blend = if (canBatchShape()) batchBlendMode() else null
            if (blend != null) {
                onApplyColor(drawContext)
                if (drawAlpha != 0f) {
                    onDrawBatch(drawContext, blend)
                }
                return
            }
            ensureBuffer()
        }

        super.doDraw(drawContext, pCamera)
        onDrawBuffer(drawContext)

        // Disable depth test after drawing so subsequent entities are not accidentally
        // depth-tested against values we wrote (same fix as in UITriangleMesh).
        if (depthInfo.test) {
            drawContext.disableDepthTest()
        }
    }

    override fun beginDraw(drawContext: DrawContext) {
        super.beginDraw(drawContext)
        clearIfRequested()

        // If it's a shared buffer, we might need to update it before drawing.
        if (buffer?.sharingMode == BufferSharingMode.Dynamic) {
            onUpdateBuffer()
        }

        // The mesh renderer takes depth and blend from the pipeline of the draw.
        if (!depthInfo.test) {
            drawContext.disableDepthTest()
        }
    }

    /**
     * Adds this shape to the quad batch with [blend] and the current model-view and projection of
     * [gl]; the draw color is applied and not transparent. Called instead of the buffer draw for
     * entities built with `batchesShape` whenever [canBatchShape] allows.
     */
    protected open fun onDrawBatch(gl: DrawContext, blend: BlendMode) = Unit

    /**
     * Draws the buffer, through the [MeshRenderer] by default ([IBuffer.drawMesh]). Entities with a
     * program of their own override this and draw the buffer's data through
     * [MeshRenderer.drawProgram].
     */
    protected open fun onDrawBuffer(gl: DrawContext) {
        buffer?.drawMesh(gl, this)
    }

    //endregion


    override fun reset() {
        super.reset()
        blendInfo = BlendInfo.Mixture
    }

    // Buffers own no GL objects: nothing to release, and the entity is not disposed from the finalizer.
    override fun finalize() = Unit


    private companion object {
        val DEFAULT_BLEND_INFO = BlendInfo(BLENDFUNCTION_SOURCE_DEFAULT, BLENDFUNCTION_DESTINATION_DEFAULT)
    }

}