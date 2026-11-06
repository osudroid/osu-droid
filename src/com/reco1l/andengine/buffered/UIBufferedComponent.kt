package com.reco1l.andengine.buffered

import com.reco1l.andengine.component.*
import org.andengine.engine.camera.Camera
import org.andengine.graphics.BlendMode
import org.andengine.graphics.ClearBuffers
import org.andengine.graphics.DepthState
import org.andengine.graphics.DrawContext
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesPipeline


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
     * The blend mode of the entity; null draws with the one of the parent buffered component, or
     * [DEFAULT_BLEND] if there is none.
     */
    var blendMode: BlendMode? = DEFAULT_BLEND

    /**
     * How the entity uses the depth buffer.
     */
    var depthState = DepthState.Disabled

    /**
     * The buffers cleared before the entity draws.
     */
    var clearBuffers = ClearBuffers.None


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


    /**
     * Sets [blendMode] from GL blend factors, for code written against them; factors without an
     * engine equivalent draw with [DEFAULT_BLEND].
     */
    fun setBlendFunction(source: Int, destination: Int) {
        blendMode = GlesPipeline.blendModeOf(source, destination) ?: DEFAULT_BLEND
    }

    /**
     * The blend mode this entity draws with: [blendMode], or the parent's if it is null.
     */
    protected val resolvedBlendMode: BlendMode
        get() = blendMode ?: (parent as? UIBufferedComponent<*>)?.blendMode ?: DEFAULT_BLEND

    /**
     * Clears the buffers of [clearBuffers]. Entities whose [doDraw] skips [beginDraw] call this
     * themselves.
     */
    protected fun clearIfRequested(gl: DrawContext) {
        gl.clear(clearBuffers)
    }

    /**
     * Draws [vertexCount] positions (x, y pairs from [offset] on) of this entity's buffer as
     * [primitive] through the [MeshRenderer], with the draw color, blend and depth settings of this
     * entity and the current model-view and projection of [gl]. Transparent entities draw as well:
     * they still write depth when [depthState] asks for it.
     */
    fun drawPositions(gl: DrawContext, primitive: PrimitiveType, positions: FloatArray, offset: Int, vertexCount: Int) {
        MeshRenderer.draw(
            primitive, positions, offset, vertexCount,
            gl.modelViewGLMatrix, gl.projectionGLMatrix,
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            resolvedBlendMode, depthState.test, depthState.write
        )
    }

    /**
     * Whether this entity's current depth and clear state can be drawn through the batch: no depth
     * test and no clearing. Shapes that fail this are drawn through their buffer.
     *
     * A batched shape does not run `beginDraw` and owns no buffer of its own, so do not assign
     * shared buffers to shapes that can be batched. Invisible shapes still take the buffer path
     * when they cannot be batched: they serve as depth masks there.
     */
    protected fun canBatchShape(): Boolean = depthState.test == null && clearBuffers.isNone


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
                onUpdateBuffer()
            }
        }
    }

    override fun doDraw(drawContext: DrawContext, pCamera: Camera) {
        if (batchesShape) {
            if (canBatchShape()) {
                onApplyColor(drawContext)
                if (drawAlpha != 0f) {
                    onDrawBatch(drawContext, resolvedBlendMode)
                }
                return
            }
            ensureBuffer()
        }

        super.doDraw(drawContext, pCamera)
        onDrawBuffer(drawContext)

        // Disable depth test after drawing so subsequent entities are not accidentally
        // depth-tested against values we wrote (same fix as in UITriangleMesh).
        if (depthState.test != null) {
            drawContext.disableDepthTest()
        }
    }

    override fun beginDraw(drawContext: DrawContext) {
        super.beginDraw(drawContext)
        clearIfRequested(drawContext)

        // If it's a shared buffer, we might need to update it before drawing.
        if (buffer?.sharingMode == BufferSharingMode.Dynamic) {
            onUpdateBuffer()
        }

        // The mesh renderer takes depth and blend from the pipeline of the draw.
        if (depthState.test == null) {
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
        blendMode = DEFAULT_BLEND
    }

    // Buffers own no GL objects: nothing to release, and the entity is not disposed from the finalizer.
    override fun finalize() = Unit


    companion object {

        /** The blend mode of entities that set none: straight alpha, as legacy entities blend. */
        @JvmField
        val DEFAULT_BLEND: BlendMode = BlendMode.AlphaAllChannels
    }

}