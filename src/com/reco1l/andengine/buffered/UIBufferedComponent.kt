package com.reco1l.andengine.buffered

import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.component.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.Shape.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.GL10


/**
 * An entity that uses a buffer to draw itself.
 */
abstract class UIBufferedComponent<T : IBuffer> : UIComponent() {

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
     * Whether to allow the buffer cache.
     */
    var allowBufferCache = true

    /**
     * Whether to allow the buffer to be updated dynamically.
     */
    var allowBufferDynamicUpdate = true

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
     * Indicates whether the buffer needs to be updated.
     */
    protected var needsBufferUpdate = true
        private set


    private var bufferCacheKey: String? = null


    fun setBlendFunction(source: Int, destination: Int) {
        blendInfo = BlendInfo(source, destination)
    }


    //region Buffer lifecycle

    protected abstract fun generateBufferCacheKey(): String

    /**
     * Called when the buffer needs to be built.
     */
    protected abstract fun createBuffer(): T

    /**
     * Called when the buffer needs to be updated.
     */
    protected abstract fun onUpdateBuffer()


    @Deprecated(message = "Requesting explicitly new buffer instance is no longer allowed", replaceWith = ReplaceWith("requestBufferUpdate()"))
    fun requestNewBuffer() {
        needsBufferUpdate = true
    }

    fun requestBufferUpdate() {
        needsBufferUpdate = true
    }

    //endregion

    //region Draw pipeline

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestNewBuffer()
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        // Buffer update is done after invalidations are handled so we can
        // refer the buffer in those invalidations.
        if (needsBufferUpdate) {
            needsBufferUpdate = false

            val cacheKey = generateBufferCacheKey()

            if (bufferCacheKey != cacheKey) {

                if (allowBufferCache) {
                    val newBuffer = UIEngine.current.resources.getOrStoreBuffer(cacheKey) { createBuffer() }

                    val oldBuffer = buffer
                    if (oldBuffer != null) {
                        UIEngine.current.resources.unsubscribeFromBuffer(oldBuffer, this)
                    }

                    UIEngine.current.resources.subscribeToBuffer(newBuffer, this)

                    @Suppress("UNCHECKED_CAST")
                    buffer = newBuffer as T?
                } else {
                    if (!allowBufferDynamicUpdate || buffer == null) {
                        buffer = createBuffer()
                    }
                }

                bufferCacheKey = cacheKey
            }

            onUpdateBuffer()
            buffer?.invalidateOnHardware()
        }

        super.onManagedDraw(gl, camera)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)
        onDeclarePointers(gl)
        onDrawBuffer(gl)
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        // Clearing
        var clearMask = 0

        if (clearInfo.depthBuffer) clearMask = clearMask or GL10.GL_DEPTH_BUFFER_BIT
        if (clearInfo.colorBuffer) clearMask = clearMask or GL10.GL_COLOR_BUFFER_BIT
        if (clearInfo.stencilBuffer) clearMask = clearMask or GL10.GL_STENCIL_BUFFER_BIT

        if (clearMask != 0) {
            gl.glClear(clearMask)
        }

        // Depth testing
        if (depthInfo.test) {
            gl.glDepthFunc(depthInfo.function)
            gl.glDepthMask(depthInfo.mask)

            GLHelper.enableDepthTest(gl)
        } else {
            GLHelper.disableDepthTest(gl)
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

        GLHelper.enableBlend(gl)
        GLHelper.blendFunction(gl, sourceFactor, destinationFactor)

        buffer?.beginDraw(gl)
    }


    protected open fun onDeclarePointers(gl: GL10) {
        buffer?.declarePointers(gl, this)
    }

    protected open fun onDrawBuffer(gl: GL10) {
        buffer?.draw(gl, this)
    }

    //endregion


    override fun reset() {
        super.reset()
        blendInfo = BlendInfo.Mixture
    }

    open fun finalize() {
        buffer?.finalize()
    }

}