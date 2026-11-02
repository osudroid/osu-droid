package org.andengine.graphics.view

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import androidx.annotation.RequiresApi

/** Asynchronously reports display vsync events. */
fun interface VsyncSource {

    interface Callback {

        /**
         * @param frameTimeNanos The vsync time, in the [System.nanoTime] time base.
         * @param expectedPresentationNanos When the frame will be shown, in the [System.nanoTime] time base.
         */
        fun onVsync(frameTimeNanos: Long, expectedPresentationNanos: Long)
    }

    /** Requests a single callback for the next vsync. */
    fun requestNextVsync(callback: Callback)

    /** Releases the resources of the source, if it holds any. No callback is requested afterwards. */
    fun quit() {}
}

/**
 * Paces a render loop to the display's vertical sync. The callback for a frame is requested one frame
 * ahead, so waiting for vsync overlaps with processing the frame instead of following it.
 *
 * [waitForVsync] must only be called from a single thread.
 */
class VsyncWaiter(
    private val source: VsyncSource = ChoreographerVsyncSource(),
    timeoutMillis: Long = 100
) {

    private val timeoutNanos = timeoutMillis * 1_000_000L

    private val lock = Object()

    // Guarded by lock.
    private var signaled = false
    private var expectedPresentationNanos = 0L

    // Only touched by the waiting thread.
    private var hasRequested = false

    private val callback = object : VsyncSource.Callback {
        override fun onVsync(frameTimeNanos: Long, expectedPresentationNanos: Long) {
            synchronized(lock) {
                signaled = true
                this@VsyncWaiter.expectedPresentationNanos = expectedPresentationNanos
                lock.notifyAll()
            }
        }
    }

    /**
     * Waits for the callback requested by the previous call and requests the next one.
     *
     * @return When the frame will be shown, in the [System.nanoTime] time base. If no callback has been
     * requested yet or none arrives within the timeout, the current [System.nanoTime].
     */
    fun waitForVsync(): Long {
        var result = System.nanoTime()

        synchronized(lock) {
            if (hasRequested) {
                val deadline = result + timeoutNanos
                var remaining = timeoutNanos

                while (!signaled && remaining > 0) {
                    val millis = remaining / 1_000_000L
                    lock.wait(millis, (remaining - millis * 1_000_000L).toInt())
                    remaining = deadline - System.nanoTime()
                }

                result = if (signaled) expectedPresentationNanos else System.nanoTime()
            }

            signaled = false
        }

        hasRequested = true
        source.requestNextVsync(callback)
        return result
    }

    /** Releases the resources of the vsync source. */
    fun quit() {
        source.quit()
    }
}

/** A [VsyncSource] backed by [Choreographer], running on its own looper thread. */
class ChoreographerVsyncSource : VsyncSource {

    private val thread = HandlerThread("ChoreographerVsync").also { it.start() }

    private val handler = Handler(thread.looper)

    @Volatile
    private var pending: VsyncSource.Callback? = null

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        pending?.onVsync(frameTimeNanos, frameTimeNanos)
    }

    private val vsyncCallback: Choreographer.VsyncCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Api33.createVsyncCallback { pending } else null

    // Runs on the looper thread, so this is that thread's Choreographer and the callback fires there.
    private val post = Runnable {
        val choreographer = Choreographer.getInstance()
        if (vsyncCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            choreographer.postVsyncCallback(vsyncCallback)
        } else {
            choreographer.postFrameCallback(frameCallback)
        }
    }

    override fun requestNextVsync(callback: VsyncSource.Callback) {
        pending = callback
        handler.post(post)
    }

    override fun quit() {
        thread.quitSafely()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33 {

        fun createVsyncCallback(callback: () -> VsyncSource.Callback?) = Choreographer.VsyncCallback { data ->
            callback()?.onVsync(
                data.frameTimeNanos,
                data.preferredFrameTimeline.expectedPresentationTimeNanos
            )
        }
    }
}
