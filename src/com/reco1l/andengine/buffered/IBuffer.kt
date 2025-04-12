package com.reco1l.andengine.buffered

import org.anddev.andengine.opengl.buffer.BufferObject
import javax.microedition.khronos.opengles.GL10

interface IBuffer {

    /**
     * Called before drawing the buffer.
     */
    fun beginDraw(gl: GL10)

    /**
     * Called when the buffer should declare its pointers and submit its data.
     */
    fun declarePointers(gl: GL10, entity: BufferedEntity<*>)

    /**
     * Called when the buffer should update its data.
     */
    fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any)

    /**
     * Called when the buffer should draw itself.
     */
    fun draw(gl: GL10, entity: BufferedEntity<*>)

    /**
     * Called during disposal.
     */
    fun finalize() = Unit

}

abstract class Buffer(
    capacity: Int,
    bufferUsage: Int,
    isManaged: Boolean = true
) : BufferObject(capacity, bufferUsage, isManaged), IBuffer {


    override fun finalize() {
        if (isManaged) {
            unloadFromActiveBufferObjectManager()
        }
    }

}
