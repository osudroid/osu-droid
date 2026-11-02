package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import java.nio.ShortBuffer
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.GpuBuffer
import org.andengine.graphics.debug.RenderStats

/**
 * GL buffer object of a fixed size. For [BufferUsage.Static] and [BufferUsage.Dynamic], every
 * [update] is also copied into a CPU-side shadow, which is uploaded again with `glBufferData` after
 * a context loss. [BufferUsage.Stream] keeps no shadow: its content is rewritten every frame, so a
 * restore only allocates the storage. [BufferTarget.Storage] needs ES 3.1. GL thread only.
 */
class GlesBuffer(
    override val target: BufferTarget,
    val usage: BufferUsage,
    override val sizeBytes: Int,
    override val label: String
) : GpuBuffer, GlesRestorable {

    /** The GL buffer target, e.g. `GL_ARRAY_BUFFER`. */
    @JvmField
    val glTarget: Int = when (target) {
        BufferTarget.Vertex -> GLES20.GL_ARRAY_BUFFER
        BufferTarget.Index -> GLES20.GL_ELEMENT_ARRAY_BUFFER
        BufferTarget.Uniform -> GLES30.GL_UNIFORM_BUFFER
        BufferTarget.PixelUnpack -> GLES30.GL_PIXEL_UNPACK_BUFFER
        BufferTarget.PixelPack -> GLES30.GL_PIXEL_PACK_BUFFER
        BufferTarget.Storage -> GLES31.GL_SHADER_STORAGE_BUFFER
    }

    private val glUsage = GlesResources.usage(usage)

    private val keepsShadow = usage != BufferUsage.Stream

    /** The GL buffer; 0 while there is none. */
    var bufferId = 0
        private set

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Buffers

    /** Last uploaded content, created on the first [update], with cached views to avoid allocations. */
    private var shadow: ByteBuffer? = null
    private var shadowShorts: ShortBuffer? = null
    private var shadowChars: CharBuffer? = null
    private var shadowInts: IntBuffer? = null
    private var shadowFloats: FloatBuffer? = null
    private var shadowLongs: LongBuffer? = null
    private var shadowDoubles: DoubleBuffer? = null

    init {
        require(sizeBytes > 0) { "Buffer '$label' needs a positive size, got $sizeBytes" }
        if (target == BufferTarget.Storage) {
            check(GlesBackend.caps.hasGl31) {
                "Buffer '$label': storage buffers need OpenGL ES 3.1, the context is ${GlesBackend.caps.version}"
            }
        }
        create()
        GlesBackend.registry.register(this)
    }

    private fun create() {
        val ids = IntArray(1)
        GlesBackend.gl.glGenBuffers(1, ids, 0)
        bufferId = ids[0]
        bind()
        val data = shadow
        data?.position(0)
        GlesBackend.gl.glBufferData(glTarget, sizeBytes, data, glUsage)
        GlesResources.label(GLES32.GL_BUFFER, bufferId, label)
    }

    /** Binds this buffer to its target through the state cache. */
    fun bind() {
        val state = GlesBackend.state
        when (target) {
            BufferTarget.Vertex -> state.bindArrayBuffer(bufferId)
            BufferTarget.Index -> {
                // The element binding belongs to the bound VAO; do not change some VAO's indices.
                state.bindVertexArray(0)
                state.bindElementBuffer(bufferId)
            }
            BufferTarget.Uniform -> state.bindUniformBuffer(bufferId)
            BufferTarget.PixelUnpack -> state.bindPixelUnpackBuffer(bufferId)
            BufferTarget.PixelPack -> state.bindPixelPackBuffer(bufferId)
            BufferTarget.Storage -> state.bindShaderStorageBuffer(bufferId)
        }
    }

    override fun update(offsetBytes: Int, data: Buffer, byteCount: Int) {
        check(!isDisposed) { "Buffer '$label' is disposed" }
        require(offsetBytes >= 0 && byteCount >= 0 && offsetBytes + byteCount <= sizeBytes) {
            "Update of $byteCount bytes at $offsetBytes outside $sizeBytes bytes of '$label'"
        }
        if (byteCount == 0) return

        if (keepsShadow) copyToShadow(offsetBytes, data, byteCount)
        bind()
        GlesBackend.gl.glBufferSubData(glTarget, offsetBytes, byteCount, data)
        RenderStats.upload(byteCount)
    }

    private fun copyToShadow(offsetBytes: Int, data: Buffer, byteCount: Int) {
        var bytes = shadow
        if (bytes == null) {
            bytes = ByteBuffer.allocateDirect(sizeBytes).order(ByteOrder.nativeOrder())
            // Views start at the current position, so create them all while it is 0.
            shadowShorts = bytes.asShortBuffer()
            shadowChars = bytes.asCharBuffer()
            shadowInts = bytes.asIntBuffer()
            shadowFloats = bytes.asFloatBuffer()
            shadowLongs = bytes.asLongBuffer()
            shadowDoubles = bytes.asDoubleBuffer()
            shadow = bytes
        }

        val position = data.position()
        val limit = data.limit()
        when (data) {
            is ByteBuffer -> {
                data.limit(position + byteCount)
                bytes.position(offsetBytes)
                bytes.put(data)
            }
            is FloatBuffer -> {
                val view = shadowFloats!!
                data.limit(position + elements(byteCount, 4))
                view.position(elements(offsetBytes, 4))
                view.put(data)
            }
            is ShortBuffer -> {
                val view = shadowShorts!!
                data.limit(position + elements(byteCount, 2))
                view.position(elements(offsetBytes, 2))
                view.put(data)
            }
            is IntBuffer -> {
                val view = shadowInts!!
                data.limit(position + elements(byteCount, 4))
                view.position(elements(offsetBytes, 4))
                view.put(data)
            }
            is CharBuffer -> {
                val view = shadowChars!!
                data.limit(position + elements(byteCount, 2))
                view.position(elements(offsetBytes, 2))
                view.put(data)
            }
            is LongBuffer -> {
                val view = shadowLongs!!
                data.limit(position + elements(byteCount, 8))
                view.position(elements(offsetBytes, 8))
                view.put(data)
            }
            is DoubleBuffer -> {
                val view = shadowDoubles!!
                data.limit(position + elements(byteCount, 8))
                view.position(elements(offsetBytes, 8))
                view.put(data)
            }
            else -> throw IllegalArgumentException("Unsupported buffer type ${data.javaClass.name}")
        }
        data.limit(limit)
        data.position(position)
    }

    private fun elements(bytes: Int, elementSize: Int): Int {
        require(bytes % elementSize == 0) {
            "Buffer '$label': $bytes bytes is not a multiple of the element size $elementSize"
        }
        return bytes / elementSize
    }

    override fun onContextLost() {
        bufferId = 0
    }

    override fun restore(backend: GlesBackend) = create()

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        GlesBackend.registry.unregister(this)
        val id = bufferId
        bufferId = 0
        if (id != 0) {
            GlesBackend.state.onBufferDeleted(id)
            GlesBackend.gl.glDeleteBuffers(1, intArrayOf(id), 0)
        }
        shadow = null
        shadowShorts = null
        shadowChars = null
        shadowInts = null
        shadowFloats = null
        shadowLongs = null
        shadowDoubles = null
    }

    override fun toString() = "GlesBuffer($label, $target, $sizeBytes bytes)"
}
