package com.reco1l.andengine.buffered

import com.reco1l.andengine.*
import com.reco1l.andengine.info.*
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.shape.Shape.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.GL10


/**
 * An entity that uses a buffer to draw itself.
 */
abstract class BufferedEntity<T: IBuffer> : ExtendedEntity() {

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
    protected abstract fun onCreateBuffer(gl: GL10): T?

    /**
     * Called when the buffer needs to be updated.
     */
    protected open fun onUpdateBuffer(gl: GL10, vararg data: Any) {
        val buffer = buffer

        if (buffer != null) {
            buffer.update(gl, this, *data)

            if (buffer is Buffer) {
                buffer.setHardwareBufferNeedsUpdate()
            }
        }
    }

    //endregion

    //region Draw pipeline

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        val invalidationFlags = bufferInvalidationFlags

        if (invalidationFlags != 0) {

            if (invalidationFlags and BufferInvalidationFlag.Instance != 0) {
                buffer = onCreateBuffer(gl)
            }

            if (invalidationFlags and BufferInvalidationFlag.Instance != 0 || invalidationFlags and BufferInvalidationFlag.Data != 0) {
                onUpdateBuffer(gl)
            }

            if (invalidationFlags == bufferInvalidationFlags) {
                bufferInvalidationFlags = 0
            }
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

            if (parent is BufferedEntity<*>) {
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

    fun finalize() {
        buffer?.finalize()
    }

}