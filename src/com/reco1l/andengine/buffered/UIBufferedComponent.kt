package com.reco1l.andengine.buffered

import android.util.Log
import com.reco1l.andengine.component.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.Shape.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.GL10


/**
 * An entity that uses a buffer to draw itself.
 */
abstract class UIBufferedComponent<T : IBuffer> : UIComponent() {

    override var radius: Float
        get() = super.radius
        set(value) {
            if (super.radius != value) {
                super.radius = value
                requestBufferUpdate()
            }
        }

    /**
     * The buffer itself.
     */
    var buffer: T? = null
        get() = bufferReference?.get() ?: field
        set(value) {
            val current = bufferReference?.get() ?: field
            if (current != value) {
                current?.finalize()
                bufferReference?.set(value) ?: run { field = value }
            }
        }

    /**
     * A mutable reference to a shared buffer. This allows multiple components to share
     * the same buffer instance. When the buffer is updated via [buffer], all components
     * that share this reference will see the change.
     *
     * To share a buffer between components, create a single [MutableReference] instance
     * and assign it to all components that should share the buffer:
     *
     * ```kotlin
     * val sharedRef = MutableReference<MyBuffer>(null)
     * component1.bufferReference = sharedRef
     * component2.bufferReference = sharedRef
     *
     * // Now when you set the buffer on one component, all components see it
     * component1.buffer = myBuffer // component2 will also use myBuffer
     * ```
     */
    var bufferReference: MutableReference<T?>? = null

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


    private var needsBufferUpdate = true


    fun setBlendFunction(source: Int, destination: Int) {
        blendInfo = BlendInfo(source, destination)
    }


    //region Buffer lifecycle

    /**
     * Determines if the current buffer can be reused instead of creating a new one.
     *
     * Override this method to implement custom buffer reuse logic based on buffer properties.
     * Return `true` if the existing buffer is compatible and can be reused, `false` otherwise.
     *
     * @param buffer The current buffer to check for reusability
     * @return `true` if the buffer can be reused, `false` if a new buffer should be created
     */
    protected open fun canReuseBuffer(buffer: T): Boolean = false

    /**
     * Called when a new buffer needs to be created.
     */
    protected abstract fun createBuffer(): T

    /**
     * Called when the buffer needs to be updated.
     */
    protected abstract fun onUpdateBuffer()


    /**
     * Requests the buffer to be updated.
     */
    fun requestBufferUpdate() {
        needsBufferUpdate = true
    }

    //endregion

    //region Draw pipeline

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    override fun onHandleInvalidations(flags: Int) {
        super.onHandleInvalidations(flags)

        // Buffer update is done after invalidations are handled so we can
        // refer the buffer in those invalidations.
        if (needsBufferUpdate) {
            needsBufferUpdate = false

            val currentBuffer = buffer
            if (currentBuffer == null || !canReuseBuffer(currentBuffer)) {
                if (currentBuffer != null) {
                    Log.d("UIBufferedComponent", "Cannot reuse buffer, creating a new one. Type: ${currentBuffer::class}")
                }
                buffer = createBuffer()
            }

            val buffer = buffer

            // If the buffer is shared and its sharing mode is dynamic, we
            // need to update it before drawing every frame, this might be
            // CPU intensive, but the memory consumption is lower.
            if (buffer != null && buffer.sharingMode != BufferSharingMode.Dynamic) {
                updateBuffer()
            }
        }
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

        // If it's a shared buffer, we might need to update it before drawing.
        if (buffer?.sharingMode == BufferSharingMode.Dynamic) {
            updateBuffer()
        }

        buffer?.beginDraw(gl)
    }

    private fun updateBuffer() {
        onUpdateBuffer()
        buffer?.invalidateOnHardware()
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