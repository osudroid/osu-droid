package com.reco1l.andengine.buffered

import org.andengine.graphics.DrawContext

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
    fun drawMesh(gl: DrawContext, entity: UIBufferedComponent<*>)

}

abstract class Buffer(capacity: Int) : IBuffer {

    override var sharingMode = BufferSharingMode.Off

    /**
     * The client-side data the mesh renderer reads.
     */
    @JvmField
    val data = FloatArray(capacity)

}


fun <T : IBuffer> T.asSharedStatically(): T {
    sharingMode = BufferSharingMode.Static
    return this
}

fun <T : IBuffer> T.asSharedDynamically(): T {
    sharingMode = BufferSharingMode.Dynamic
    return this
}
