package com.reco1l.andengine.buffered

import com.reco1l.andengine.component.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.Shape.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.GL10


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
    open var blendInfo = BlendInfo.Mixture

    /**
     * The depth information of the entity.
     */
    open var depthInfo = DepthInfo.None

    /**
     * The clear information of the entity.
     */
    open var clearInfo = ClearInfo.None

    /**
     * Flags that determine when the buffer should be invalidated.
     */
    open var bufferInvalidationFlags = BufferInvalidationFlag.Data or BufferInvalidationFlag.Instance


    /**
     * Marks the buffer's data as invalid to be updated in the next frame.
     */
    fun invalidateBuffer(@BufferInvalidationFlag flag: Int) {
        bufferInvalidationFlags = bufferInvalidationFlags or flag
    }


    open fun setBlendFunction(source: Int, destination: Int) {
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

    //endregion

    //region Draw pipeline

    override fun onHandleInvalidations(restoreFlags: Boolean) {

        val invalidationFlags = bufferInvalidationFlags

        if (invalidationFlags != 0) {
            if (invalidationFlags and BufferInvalidationFlag.Instance != 0) {
                buffer = onCreateBuffer()
            }
        }

        super.onHandleInvalidations(restoreFlags)

        // Buffer update is done after invalidations are handled so we can
        // refer the buffer in those invalidations.
        if (invalidationFlags != 0) {

            // If the buffer is shared and its sharing mode is dynamic, we
            // need to update it before drawing every frame, this might be
            // CPU intensive, but the memory consumption is lower.
            if (invalidationFlags and BufferInvalidationFlag.Instance != 0 || invalidationFlags and BufferInvalidationFlag.Data != 0) {
                val buffer = buffer

                if (buffer != null && buffer.sharingMode != BufferSharingMode.Dynamic) {
                    updateBuffer()
                }
            }

            if (invalidationFlags == bufferInvalidationFlags) {
                bufferInvalidationFlags = 0
            }
        }
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        val wasDepthTest = GLHelper.isEnableDepthTest()

        super.doDraw(gl, camera)
        onDeclarePointers(gl)
        onDrawBuffer(gl)

        GLHelper.setDepthTest(gl, wasDepthTest)
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

    fun finalize() {
        buffer?.finalize()
    }

}