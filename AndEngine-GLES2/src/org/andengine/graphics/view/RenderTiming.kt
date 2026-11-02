package org.andengine.graphics.view

/**
 * CPU time the render thread spent drawing the most recent frame it presented; time blocked on the display,
 * the buffer queue or other threads is not counted.
 *
 * Written once per frame and read without locking, so it costs nothing in release builds.
 */
object RenderTiming {

    /** CPU nanoseconds the render thread spent drawing its latest presented frame; `0` before the first one. */
    @Volatile
    @JvmField
    var lastWorkNanos: Long = 0L
}
