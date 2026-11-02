package com.reco1l.andengine.buffered

import android.opengl.GLES32
import org.andengine.opengl.util.GLState

// GLES20 draw topology constants (mirrored for convenience)
const val GL_TRIANGLES = GLES32.GL_TRIANGLES
const val GL_TRIANGLE_STRIP = GLES32.GL_TRIANGLE_STRIP
const val GL_TRIANGLE_FAN = GLES32.GL_TRIANGLE_FAN
const val GL_LINES = GLES32.GL_LINES
const val GL_LINE_STRIP = GLES32.GL_LINE_STRIP
const val GL_LINE_LOOP = GLES32.GL_LINE_LOOP
const val GL_POINTS = GLES32.GL_POINTS

/**
 * The geometry of a [UIBufferedComponent]: client-side data the component draws through the mesh
 * renderer. Buffers own no GL objects.
 */
interface IBuffer {

    /**
     * Determines whether the buffer is shared between multiple entities and how it should be handled.
     */
    var sharingMode: BufferSharingMode


    /**
     * Called when the buffer should draw itself through the mesh renderer, with the color, blend
     * and depth settings of [entity] (see [UIBufferedComponent.drawPositions]). Buffers that only
     * hold attributes for another buffer draw nothing.
     */
    fun drawMesh(gl: GLState, entity: UIBufferedComponent<*>)

}

abstract class Buffer(capacity: Int) : IBuffer {

    override var sharingMode = BufferSharingMode.Off

    /**
     * The client-side data the mesh renderer reads.
     */
    @JvmField
    val data = FloatArray(capacity)

    /** The float at [index] of the client-side data. */
    fun valueAt(index: Int): Float = data[index]

}


fun <T : IBuffer> T.asSharedStatically(): T {
    sharingMode = BufferSharingMode.Static
    return this
}

fun <T : IBuffer> T.asSharedDynamically(): T {
    sharingMode = BufferSharingMode.Dynamic
    return this
}
