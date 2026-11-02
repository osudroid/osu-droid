package org.andengine.graphics.gles

import org.andengine.graphics.VertexLayout

/**
 * One VAO per (vertex layout, vertex buffer, index buffer or none) combination, created on first
 * use. Entries of disposed buffers are deleted on the next lookup that misses, so a lookup that
 * hits stays a short scan without allocations. On a new context all entries are dropped and
 * created again lazily ([restore] does nothing). GL thread only.
 */
class GlesVertexArrayCache : GlesRestorable {

    private class Entry(
        @JvmField val layout: VertexLayout,
        @JvmField val vertexBuffer: GlesBuffer,
        @JvmField val indexBuffer: GlesBuffer?,
        @JvmField val vaoId: Int
    )

    private val entries = ArrayList<Entry>()
    private val scratch = IntArray(1)

    /** The number of cached vertex arrays, read by tests. */
    val size: Int get() = entries.size

    override val restorePhase: RestorePhase get() = RestorePhase.VertexArrays

    /** The VAO for the combination; the buffers must not be disposed. May bind other state. */
    fun obtain(layout: VertexLayout, vertexBuffer: GlesBuffer, indexBuffer: GlesBuffer?): Int {
        for (i in entries.indices) {
            val e = entries[i]
            if (e.vertexBuffer === vertexBuffer && e.indexBuffer === indexBuffer && e.layout == layout) return e.vaoId
        }
        purgeDisposed()
        return create(layout, vertexBuffer, indexBuffer)
    }

    private fun purgeDisposed() {
        var i = entries.size - 1
        while (i >= 0) {
            val e = entries[i]
            if (e.vertexBuffer.isDisposed || e.indexBuffer?.isDisposed == true) {
                GlesBackend.state.onVertexArrayDeleted(e.vaoId)
                scratch[0] = e.vaoId
                GlesBackend.gl.glDeleteVertexArrays(1, scratch, 0)
                entries.removeAt(i)
            }
            i--
        }
    }

    private fun create(layout: VertexLayout, vertexBuffer: GlesBuffer, indexBuffer: GlesBuffer?): Int {
        val state = GlesBackend.state
        GlesBackend.gl.glGenVertexArrays(1, scratch, 0)
        val vao = scratch[0]

        state.bindVertexArray(vao)
        // The attribute pointers capture the array buffer bound at this point.
        state.bindArrayBuffer(vertexBuffer.bufferId)
        val stride = layout.strideBytes
        val attributes = layout.attributes
        for (i in attributes.indices) {
            val a = attributes[i]
            state.vertexAttribPointer(
                a.location,
                GlesPipeline.componentCount(a.format),
                GlesPipeline.componentType(a.format),
                GlesPipeline.isNormalized(a.format),
                stride,
                a.offsetBytes
            )
            state.enableVertexAttribArray(a.location)
        }
        if (indexBuffer != null) state.bindElementBuffer(indexBuffer.bufferId)

        entries += Entry(layout, vertexBuffer, indexBuffer, vao)
        return vao
    }

    override fun onContextLost() {
        entries.clear()
    }

    override fun restore() {
        // Lazy: the next draw creates the VAOs it needs.
    }

    override fun toString() = "GlesVertexArrayCache(${entries.size} VAOs)"
}
