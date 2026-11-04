package org.andengine.graphics

/** Measures the GPU time between [begin] and [end]. Only one query may be running at a time. */
interface GpuQuery : GpuResource {

    fun begin()

    fun end()

    /**
     * True from [end] until the result was read by [resultNanos]. Becomes false without a
     * result when the GPU context is lost.
     */
    val isPending: Boolean

    /**
     * The measured time in nanoseconds once it is available, which is usually a few frames after
     * [end]. Returns -1 while the result is not available yet ([isPending] stays true), and when
     * the GPU reported that the measurement is invalid ([isPending] is false afterwards).
     */
    fun resultNanos(): Long
}
