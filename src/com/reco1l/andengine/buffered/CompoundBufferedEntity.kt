package com.reco1l.andengine.buffered

import com.reco1l.toolkt.kotlin.*
import javax.microedition.khronos.opengles.*

abstract class CompoundBufferedEntity(buffers: MutableList<Buffer> = mutableListOf()) : BufferedEntity<CompoundBuffer>(CompoundBuffer(buffers)) {


    /**
     * The buffers that this entity holds.
     */
    val buffers
        get() = buffer!!.buffers


    override fun onRebuildBuffer(gl: GL10) {
        super.onRebuildBuffer(gl)
        clearBuffers()
    }


    inline fun <reified T : Buffer> getFirstOf(): Buffer {
        return buffer!!.getFirstOf<T>()
    }

    inline fun <reified T : Buffer> removeBuffers() {
        buffer!!.removeBuffers<T>()
    }


    fun addBuffer(value: Buffer) {
        buffer!!.addBuffer(value)
    }

    fun removeBuffer(value: Buffer) {
        buffer!!.removeBuffer(value)
    }


    fun clearBuffers() {
        buffers.fastForEach { it.finalize() }
        buffers.clear()
    }

}
