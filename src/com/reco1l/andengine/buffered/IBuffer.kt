package com.reco1l.andengine.buffered

import org.anddev.andengine.opengl.buffer.BufferObject
import javax.microedition.khronos.opengles.GL10

interface IBuffer {


    /**
     * Determines whether the buffer is shared between multiple entities and how it should be handled.
     */
    var sharingMode: BufferSharingMode


    /**
     * Called before drawing the buffer.
     */
    fun beginDraw(gl: GL10)

    /**
     * Called when the buffer should declare its pointers and submit its data.
     */
    fun declarePointers(gl: GL10, entity: UIBufferedComponent<*>)

    /**
     * Called when the buffer should draw itself.
     */
    fun draw(gl: GL10, entity: UIBufferedComponent<*>)

    /**
     * Called during disposal.
     */
    fun finalize() = Unit

    /**
     * Marks the buffer as needing an update in hardware buffers.
     */
    fun invalidateOnHardware() {
        if (this is Buffer) {
            setHardwareBufferNeedsUpdate()
        }
    }

}

abstract class Buffer(
    capacity: Int,
    bufferUsage: Int,
    isManaged: Boolean = true
) : BufferObject(capacity, bufferUsage, isManaged), IBuffer {

    override var sharingMode = BufferSharingMode.Off

    override fun finalize() {
        if (isManaged) {
            unloadFromActiveBufferObjectManager()
        }
    }

}


fun <T : IBuffer> T.asSharedStatically(): T {
    sharingMode = BufferSharingMode.Static
    return this
}

fun <T : IBuffer> T.asSharedDynamically(): T {
    sharingMode = BufferSharingMode.Dynamic
    return this
}
