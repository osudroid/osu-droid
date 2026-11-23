package org.andengine.graphics.view

import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer

/** Asynchronously reports display vsync events. */
fun interface VsyncSource {

    interface Callback {

        /**
         * @param request The latest request this vsync answers (the one this callback was requested for, or a
         * later one if that one was also requested before this vsync).
         * @param frameTimeNanos The vsync time, in the [System.nanoTime] time base.
         */
        fun onVsync(request: Long, frameTimeNanos: Long)
    }

    /**
     * Requests a single callback for the next vsync.
     *
     * @param request Handed back to [Callback.onVsync], so the callback can tell which requests it answers.
     * Larger than the request of every earlier call.
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
    // The request a signal must answer to count. A signal of an earlier request, such as one that already timed
    // out, would otherwise end the wait for this one early, so that frame would not be paced.
    private var currentRequest = 0L

    // Only touched by the waiting thread.
    private var hasRequested = false

    private val callback = object : VsyncSource.Callback {
        override fun onVsync(request: Long, frameTimeNanos: Long) {
            synchronized(lock) {
                if (request < currentRequest) {
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
class ChoreographerVsyncSource internal constructor(private val looper: FrameLooper) : VsyncSource {

    constructor() : this(HandlerThreadFrameLooper())

    @Volatile
    private var pending: VsyncSource.Callback? = null

    // The latest request, written by the requesting thread.
    @Volatile
    private var latestRequest = 0L

    // The latest request a frame callback has been posted for. Looper thread only.
    private var postedRequest = 0L

    // Posting frame callbacks and dispatching a frame both run on the looper thread, and a frame runs every frame
    // callback posted before it. When a frame callback fires, every request up to postedRequest is answered by
    // this frame, even if the frame callback of one of them was lost.
    private val frameCallback = Choreographer.FrameCallback { frameTimeNanos ->
        pending?.onVsync(postedRequest, frameTimeNanos)
    }

    private val post = Runnable {
        // Read here rather than passed along with each post, which needs no allocation per request, and a post
        // still queued behind a newer request covers that one as well.
        // Only set this on the looper thread, otherwise a late frame callback could report a request whose own
        // frame callback has not been posted yet, and end the wait for that request early.
        postedRequest = latestRequest
        looper.postFrameCallback(frameCallback)
    }

    override fun requestNextVsync(callback: VsyncSource.Callback, request: Long) {
        pending = callback
        latestRequest = request
        looper.post(post)
    }

    override fun quit() {
        looper.quit()
    }
}

/**
 * The thread a [ChoreographerVsyncSource] posts and dispatches its frame callbacks on. Posted actions and frame
 * callbacks run on the thread, each kind in the order it was posted, and a frame runs every frame callback
 * posted before it.
 */
internal interface FrameLooper {
    /**
     * Runs [action] on the thread.
     */
    fun post(action: Runnable)

    /**
     * Posts [callback] for the next frame. Only called on the thread.
     */
    fun postFrameCallback(callback: Choreographer.FrameCallback)

    /**
     * Stops the thread. Nothing posted runs afterward.
     */
    fun quit()
}

/**
 * A [FrameLooper] on a [HandlerThread] of its own, with that thread's [Choreographer].
 */
internal class HandlerThreadFrameLooper : FrameLooper {
    private val thread = HandlerThread("ChoreographerVsync").also { it.start() }
    private val handler = Handler(thread.looper)

    override fun post(action: Runnable) {
        handler.post(action)
    }

    // Called on the thread, so this is that thread's Choreographer and the callback fires there.
    override fun postFrameCallback(callback: Choreographer.FrameCallback) {
        Choreographer.getInstance().postFrameCallback(callback)
    }

    override fun quit() {
        thread.quitSafely()
    }
}
