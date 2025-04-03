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
abstract class BufferedEntity<T: IBuffer>(buffer: T? = null) : ExtendedEntity() {

    /**
     * The buffer itself.
     */
    open var buffer = buffer
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
    open var bufferInvalidationFlags: Int = RebuildBufferOnSizeChanged or InvalidateDataOnSizeChanged


    /**
     * Whether the buffer should be rebuilt. During initialization, it will be true
     * if the constructor passed [IBuffer] is `null`.
     */
    protected var shouldRebuildBuffer = buffer == null

    /**
     * Whether the buffer data should be updated.
     */
    protected var shouldUpdateBuffer = true


    /**
     * Marks the buffer's data as invalid to be updated in the next frame.
     */
    fun invalidateBuffer() {
        shouldUpdateBuffer = true
    }

    /**
     * Marks the buffer's instance as invalid to be recreated in the next frame.
     */
    fun rebuildBuffer() {
        shouldRebuildBuffer = true
    }


    open fun setBlendFunction(source: Int, destination: Int) {
        blendInfo = BlendInfo(source, destination)
    }


    override fun onSizeChanged() {
        super.onSizeChanged()

        if (bufferInvalidationFlags and InvalidateDataOnSizeChanged != 0) {
            invalidateBuffer()
        }

        if (bufferInvalidationFlags and RebuildBufferOnSizeChanged != 0) {
            rebuildBuffer()
        }
    }

    override fun onPositionChanged() {
        super.onPositionChanged()

        if (bufferInvalidationFlags and InvalidateDataOnPositionChanged != 0) {
            invalidateBuffer()
        }

        if (bufferInvalidationFlags and RebuildBufferOnPositionChanged != 0) {
            rebuildBuffer()
        }
    }


    //region Buffer lifecycle

    protected open fun onRebuildBuffer(gl: GL10) = Unit

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

        if (shouldRebuildBuffer) {
            shouldRebuildBuffer = false
            onRebuildBuffer(gl)
        }

        if (shouldUpdateBuffer) {
            shouldUpdateBuffer = false
            onUpdateBuffer(gl)
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


    @Suppress("ConstPropertyName")
    companion object {

        /**
         * The buffer's data will be invalidated when the size of the entity changes.
         */
        const val InvalidateDataOnSizeChanged = 1

        /**
         * The buffer will be rebuilded when the size of the entity changes, this
         * invalidates the buffer's data as well.
         */
        const val RebuildBufferOnSizeChanged = 1 shl 1

        /**
         * The buffer's data will be invalidated when the position of the entity changes.
         */
        const val InvalidateDataOnPositionChanged = 1 shl 2

        /**
         * The buffer will be rebuilded when the position of the entity changes, this
         * invalidates the buffer's data as well.
         */
        const val RebuildBufferOnPositionChanged = 1 shl 3

    }

}