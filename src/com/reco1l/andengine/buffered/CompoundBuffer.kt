package com.reco1l.andengine.buffered

import org.andengine.opengl.util.GLState

/**
 * A compound buffer is a buffer that contains multiple buffers.
 */
class CompoundBuffer(vararg val buffers: Buffer) : IBuffer {

    override var sharingMode = BufferSharingMode.Off


    inline fun <reified T : Buffer> getFirstOf(): T {
        return buffers.first { it is T } as T
    }


    //region Draw pipeline

    override fun beginDraw(gl: GLState) {
        buffers.forEach { it.beginDraw(gl) }
    }

    override fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>) {
        buffers.forEach { it.declarePointers(gl, entity) }
    }

    override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
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