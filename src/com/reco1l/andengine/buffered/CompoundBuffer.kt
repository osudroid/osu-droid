package com.reco1l.andengine.buffered

import javax.microedition.khronos.opengles.GL10

/**
 * A compound buffer is a buffer that contains multiple buffers.
 */
class CompoundBuffer(vararg val buffers: Buffer) : IBuffer {

    override var sharingMode = BufferSharingMode.Off


    inline fun <reified T : Buffer> getFirstOf(): T {
        return buffers.first { it is T } as T
    }


    //region Draw pipeline

    override fun beginDraw(gl: GL10) {
        buffers.forEach { it.beginDraw(gl) }
    }

    override fun declarePointers(gl: GL10, entity: UIBufferedComponent<*>) {
        buffers.forEach { it.declarePointers(gl, entity) }
    }

    override fun draw(gl: GL10, entity: UIBufferedComponent<*>) {
        buffers.forEach { it.draw(gl, entity) }
    }

    override fun invalidateOnHardware() {
        buffers.forEach { it.invalidateOnHardware() }
    }

    //endregion

    override fun finalize() {
        super.finalize()
        buffers.forEach { it.finalize() }
    }
}