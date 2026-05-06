package com.reco1l.andengine.buffered

import android.opengl.GLES20
import org.andengine.opengl.util.GLState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// GLES20 draw topology constants (mirrored for convenience)
const val GL_TRIANGLES = GLES20.GL_TRIANGLES
const val GL_TRIANGLE_STRIP = GLES20.GL_TRIANGLE_STRIP
const val GL_TRIANGLE_FAN = GLES20.GL_TRIANGLE_FAN
const val GL_LINES = GLES20.GL_LINES
const val GL_LINE_STRIP = GLES20.GL_LINE_STRIP
const val GL_POINTS = GLES20.GL_POINTS

// GLES20 buffer usage constants
const val GL_STATIC_DRAW = GLES20.GL_STATIC_DRAW
const val GL_DYNAMIC_DRAW = GLES20.GL_DYNAMIC_DRAW
const val GL_STREAM_DRAW = GLES20.GL_STREAM_DRAW

interface IBuffer {

    /**
     * Determines whether the buffer is shared between multiple entities and how it should be handled.
     */
    var sharingMode: BufferSharingMode


    /**
     * Called before drawing the buffer.
     */
    fun beginDraw(gl: GLState)

    /**
     * Called when the buffer should declare its pointers and submit its data.
     */
    fun declarePointers(gl: GLState, entity: UIBufferedComponent<*>)

    /**
     * Called when the buffer should draw itself.
     */
    fun draw(gl: GLState, entity: UIBufferedComponent<*>)

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

    protected val mFloatBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(capacity * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var mVboId = 0
    private var mDirty = true

    init {
        // Register this buffer so we can reset all VBO IDs on EGL context loss.
        sAllBuffers.add(java.lang.ref.WeakReference(this))
    }

    fun setHardwareBufferNeedsUpdate() {
        mDirty = true
    }

    protected fun bindAndUpload() {
        if (mVboId == 0) {
            val ids = IntArray(1)
            GLES20.glGenBuffers(1, ids, 0)
            mVboId = ids[0]
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId)
        if (mDirty) {
            mFloatBuffer.position(0)
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                mFloatBuffer.capacity() * 4,
                mFloatBuffer,
                bufferUsage
            )
            mDirty = false
        }
    }

    fun unloadFromActiveBufferObjectManager() {
        if (isManaged && mVboId != 0) {
            GLES20.glDeleteBuffers(1, IntArray(1) { mVboId }, 0)
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
         * Called from [EngineRenderer.onSurfaceCreated] when the EGL context has been
         * recreated (context loss recovery). Resets every live Buffer's VBO ID to 0 so
         * that [bindAndUpload] re-generates and re-uploads the buffer on next draw.
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
