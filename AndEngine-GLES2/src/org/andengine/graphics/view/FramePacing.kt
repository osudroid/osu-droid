package org.andengine.graphics.view

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Frame pacing settings shared between the owner of a render thread and the thread itself.
 * Every field may be written from any thread; the render thread picks the changes up between frames.
 */
class FramePacing {

    /** Whether buffer swaps are synchronised to the display (swap interval 1) or not (swap interval 0). */
    @Volatile
    var vsyncEnabled: Boolean = true
        set(value) {
            field = value
            swapIntervalChanged.set(true)
        }

    /** Whether frames carry an explicit presentation time. */
    @Volatile
    var presentationTimeEnabled: Boolean = false

    /** Whether the render loop is paced by display vsync callbacks instead of by the buffer swap. */
    @Volatile
    var choreographerPacingEnabled: Boolean = false

    // A fresh surface starts with the driver's default interval, so the first frame has to apply ours.
    private val swapIntervalChanged = AtomicBoolean(true)

    /**
     * Returns the swap interval to apply (`1` or `0`) if it has to be applied since the last call,
     * or `-1` if nothing changed.
     */
    fun takeSwapIntervalChange(): Int {
        if (!swapIntervalChanged.getAndSet(false)) {
            return -1
        }
        return if (vsyncEnabled) 1 else 0
    }

    /** Marks the swap interval as needing to be applied again, since a new surface does not inherit it. */
    fun markSurfaceRecreated() {
        swapIntervalChanged.set(true)
    }
}
