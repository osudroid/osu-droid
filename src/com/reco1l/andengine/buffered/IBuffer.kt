package com.reco1l.andengine.buffered

import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.debug.RenderStats
import android.opengl.GLES32
import org.andengine.opengl.util.GLState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// GLES20 draw topology constants (mirrored for convenience)
const val GL_TRIANGLES = GLES32.GL_TRIANGLES
const val GL_TRIANGLE_STRIP = GLES32.GL_TRIANGLE_STRIP
const val GL_TRIANGLE_FAN = GLES32.GL_TRIANGLE_FAN
const val GL_LINES = GLES32.GL_LINES
const val GL_LINE_STRIP = GLES32.GL_LINE_STRIP
const val GL_LINE_LOOP = GLES32.GL_LINE_LOOP
const val GL_POINTS = GLES32.GL_POINTS

// GLES20 buffer usage constants
const val GL_STATIC_DRAW = GLES32.GL_STATIC_DRAW
const val GL_DYNAMIC_DRAW = GLES32.GL_DYNAMIC_DRAW
const val GL_STREAM_DRAW = GLES32.GL_STREAM_DRAW

interface IBuffer {

    /**
     * Determines whether the buffer is shared between multiple entities and how it should be handled.
     */
    var sharingMode: BufferSharingMode


    /**
     * Called before drawing the buffer the legacy way, for entities with a shader of their own
     * ([UIBufferedComponent.drawsWithOwnShader]).
     */
    fun beginDraw(gl: GLState) = Unit

    /**
     * Called when the buffer should declare its pointers and submit its data (legacy way).
     */
    fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>) = Unit

    /**
     * Called when the buffer should draw itself the legacy way, with the shader the entity bound.
     */
    fun draw(gl: GLState, entity: UIBufferedComponent<*>) = Unit

    /**
     * Called when the buffer should draw itself through the mesh renderer, with the color, blend
     * and depth settings of [entity] (see [UIBufferedComponent.drawPositions]). Buffers that only
     * hold attributes for another buffer, or are only used the legacy way, draw nothing.
     */
    fun drawMesh(gl: GLState, entity: UIBufferedComponent<*>)

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
    val bufferUsage: Int,
    private val isManaged: Boolean = true
) : IBuffer {

    override var sharingMode = BufferSharingMode.Off

    /**
     * The client-side data: what the mesh renderer reads and what a legacy upload sends to the GL
     * buffer.
     */
    @JvmField
    val data = FloatArray(capacity)

    // Direct copy of [data] for the legacy upload; only made once the buffer is drawn the legacy way.
    private var uploadBuffer: FloatBuffer? = null

    private var mVboId = 0
    private var mDirty = true

    init {
        // Register this buffer so we can reset all VBO IDs on EGL context loss.
        sAllBuffers.add(java.lang.ref.WeakReference(this))
    }

    /** The float at [index] of the client-side data. */
    fun valueAt(index: Int): Float = data[index]

    fun setHardwareBufferNeedsUpdate() {
        mDirty = true
    }

    protected fun bindAndUpload() {
        if (mVboId == 0) {
            val ids = IntArray(1)
            GLES32.glGenBuffers(1, ids, 0)
            mVboId = ids[0]
        }
        GlesBackend.state.bindArrayBuffer(mVboId)
        if (mDirty) {
            val upload = uploadBuffer ?: ByteBuffer
                .allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .also { uploadBuffer = it }
            upload.position(0)
            upload.put(data, 0, data.size)
            upload.position(0)
            GLES32.glBufferData(
                GLES32.GL_ARRAY_BUFFER,
                data.size * 4,
                upload,
                bufferUsage
            )
            mDirty = false
            RenderStats.upload(data.size * 4)
        }
    }

    fun unloadFromActiveBufferObjectManager() {
        if (isManaged && mVboId != 0) {
            GLES32.glDeleteBuffers(1, IntArray(1) { mVboId }, 0)
            mVboId = 0
        }
    }

    override fun finalize() {
        unloadFromActiveBufferObjectManager()
    }

    override fun invalidateOnHardware() {
        mDirty = true
    }

    companion object {
        // Weak references so finalized Buffers don't prevent GC.
        private val sAllBuffers = java.util.concurrent.CopyOnWriteArrayList<java.lang.ref.WeakReference<Buffer>>()

        /**
         * Called by the GLES resource registry (registered in `MainActivity` via
         * [org.andengine.graphics.gles.GlesLegacyRestorers]) on every new EGL context. Resets every
         * live Buffer's VBO ID to 0 so that [bindAndUpload] re-generates and re-uploads the
         * buffer on next draw.
         */
        @JvmStatic
        fun onContextLost() {
            val iter = sAllBuffers.iterator()
            val dead = mutableListOf<java.lang.ref.WeakReference<Buffer>>()
            for (ref in iter) {
                val buf = ref.get()
                if (buf == null) {
                    dead.add(ref)
                } else {
                    buf.mVboId = 0
                    buf.mDirty = true
                }
            }
            sAllBuffers.removeAll(dead)
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
