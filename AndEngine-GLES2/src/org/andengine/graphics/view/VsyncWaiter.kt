package org.andengine.graphics.view

import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer

/** Asynchronously reports display vsync events. */
fun interface VsyncSource {

    interface Callback {

        /**
         * @param request The value passed to the [requestNextVsync] call this callback answers.
         * @param frameTimeNanos The vsync time, in the [System.nanoTime] time base.
         */
        fun onVsync(request: Long, frameTimeNanos: Long)
    }

    /**
     * Requests a single callback for the next vsync.
     *
     * @param request Handed back to [Callback.onVsync], so the callback can tell which request it answers.
     */
    fun requestNextVsync(callback: Callback, request: Long)

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
    // The request a signal must answer to count. A signal of a request that already timed out would otherwise
    // end the wait for the next one early, so that frame would not be paced.
    private var currentRequest = 0L

    // Only touched by the waiting thread.
    private var hasRequested = false

    private val callback = object : VsyncSource.Callback {
        override fun onVsync(request: Long, frameTimeNanos: Long) {
            synchronized(lock) {
                if (request != currentRequest) {
                    return
                }
                signaled = true
                lock.notifyAll()
            }
        }
    }

    /**
     * Waits for the callback requested by the previous call and requests the next one. Returns at once if no
     * callback has been requested yet, and after the timeout if none arrives.
     */
    fun waitForVsync() {
        val request: Long

        synchronized(lock) {
            if (hasRequested) {
                val deadline = System.nanoTime() + timeoutNanos
                var remaining = timeoutNanos

                while (!signaled && remaining > 0) {
                    val millis = remaining / 1_000_000L
                    lock.wait(millis, (remaining - millis * 1_000_000L).toInt())
                    remaining = deadline - System.nanoTime()
                }
            }

            signaled = false
            request = ++currentRequest
        }

        hasRequested = true
        source.requestNextVsync(callback, request)
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

    // The ids of the requests not yet answered, oldest first. The handler and the Choreographer both run
    // callbacks in the order they were posted, so the n-th vsync callback answers the n-th request. Guarded
    // by itself.
    private val requests = LongArray(REQUEST_CAPACITY)
    private var requestCount = 0L
    private var answeredCount = 0L

    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        deliver(frameTimeNanos)
    }

    // Runs on the looper thread, so this is that thread's Choreographer and the callback fires there.
    private val post = Runnable {
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun requestNextVsync(callback: VsyncSource.Callback, request: Long) {
        synchronized(requests) {
            requests[(requestCount % REQUEST_CAPACITY).toInt()] = request
            requestCount++
        }
        pending = callback
        handler.post(post)
    }

    override fun quit() {
        thread.quitSafely()
    }

    private fun deliver(frameTimeNanos: Long) {
        val request = synchronized(requests) {
            val index = answeredCount++
            // Overwritten by newer requests while the display stalled, so its id is lost; no waiter matches -1.
            if (requestCount - index > REQUEST_CAPACITY) -1L else requests[(index % REQUEST_CAPACITY).toInt()]
        }
        pending?.onVsync(request, frameTimeNanos)
    }

    private companion object {
        const val REQUEST_CAPACITY = 16
    }
}
