package org.andengine.graphics

import java.nio.Buffer

enum class BufferTarget { Vertex, Index, Uniform, PixelUnpack, PixelPack, Storage }

/**
 * How often a buffer's content changes. [Static] and [Dynamic] buffers keep a CPU copy of their
 * content so it survives a GPU context loss. [Stream] buffers are rewritten every frame and keep
 * none: after a context loss their content is undefined until the next update.
 */
enum class BufferUsage { Static, Dynamic, Stream }

interface GpuBuffer : GpuResource {
    val target: BufferTarget
    val sizeBytes: Int

    /** Writes [byteCount] bytes of [data], starting at its current position, at [offsetBytes]. */
    fun update(offsetBytes: Int, data: Buffer, byteCount: Int)

    /**
     * Gives the buffer new storage holding exactly the first [byteCount] bytes of [data] (read from
     * its current position). Draws already issued keep reading the old storage, so this never waits
     * for them, unlike an [update] of a buffer that queued draws still use. For data rewritten for
     * every draw. Afterwards the storage is exactly [byteCount] bytes, so a later [update] must stay
     * within it; stream buffers are meant to be only replaced.
     *
     * @throws IllegalStateException unless the buffer is [BufferUsage.Stream].
     */
    fun replace(data: Buffer, byteCount: Int)
}
