package com.reco1l.andengine.buffered

import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.Camera
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
     * Flags that determine when the buffer should be invalidated.
     */
    open var invalidationFlags: Int = RebuildBufferOnSizeChanged or InvalidateDataOnSizeChanged


    /**
     * Whether the buffer should be rebuilt. During initalization it will be true
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


    override fun onSizeChanged() {
        super.onSizeChanged()

        if (invalidationFlags and InvalidateDataOnSizeChanged != 0) {
            invalidateBuffer()
        }

        if (invalidationFlags and RebuildBufferOnSizeChanged != 0) {
            rebuildBuffer()
        }
    }

    override fun onPositionChanged() {
        super.onPositionChanged()

        if (invalidationFlags and InvalidateDataOnPositionChanged != 0) {
            invalidateBuffer()
        }

        if (invalidationFlags and RebuildBufferOnPositionChanged != 0) {
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
        buffer?.beginDraw(gl)
    }

    protected open fun onDeclarePointers(gl: GL10) {
        buffer?.declarePointers(gl, this)
    }

    protected open fun onDrawBuffer(gl: GL10) {
        buffer?.draw(gl, this)
    }

    //endregion


    fun finalize() {
        buffer?.finalize()
    }


    companion object {

        /**
         * The buffer's data will be invalidated when the size of the entity changes.
         */
        const val InvalidateDataOnSizeChanged = 2

        /**
         * The buffer will be rebuilded when the size of the entity changes, this
         * invalidates the buffer's data as well.
         */
        const val RebuildBufferOnSizeChanged = 2 shl 1

        /**
         * The buffer's data will be invalidated when the position of the entity changes.
         */
        const val InvalidateDataOnPositionChanged = 2 shl 2

        /**
         * The buffer will be rebuilded when the position of the entity changes, this
         * invalidates the buffer's data as well.
         */
        const val RebuildBufferOnPositionChanged = 2 shl 3

    }

}