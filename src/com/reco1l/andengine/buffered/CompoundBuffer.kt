package com.reco1l.andengine.buffered

import com.reco1l.toolkt.kotlin.*
import javax.microedition.khronos.opengles.GL10

/**
 * A compound buffer is a buffer that contains multiple buffers.
 */
class CompoundBuffer(vararg val buffers: Buffer) : IBuffer {


    inline fun <reified T : Buffer> getFirstOf(): Buffer {
        return buffers.first { it is T }
    }


    override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {
        buffers.fastForEach { buffer ->
            buffer.update(gl, entity, *data)
            buffer.setHardwareBufferNeedsUpdate()
        }
    }

    //region Draw pipeline

    override fun beginDraw(gl: GL10) {
        buffers.fastForEach { it.beginDraw(gl) }
    }

    override fun declarePointers(gl: GL10, entity: BufferedEntity<*>) {
        buffers.fastForEach { it.declarePointers(gl, entity) }
    }

    override fun draw(gl: GL10, entity: BufferedEntity<*>) {
        buffers.fastForEach { it.draw(gl, entity) }
    }

    //endregion

    override fun finalize() {
        super.finalize()
        buffers.fastForEach { it.finalize() }
    }
}