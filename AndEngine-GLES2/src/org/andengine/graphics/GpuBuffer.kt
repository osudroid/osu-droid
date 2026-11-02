package org.andengine.graphics

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
    fun update(offsetBytes: Int, data: java.nio.Buffer, byteCount: Int)
}
