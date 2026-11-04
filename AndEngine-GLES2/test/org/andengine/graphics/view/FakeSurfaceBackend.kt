package org.andengine.graphics.view

import android.view.Surface
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

/** Calls recorded by [FakeSurfaceBackend] and [RecordingFrameRenderer], in order, safe to read from the test thread. */
class CallLog {

    private val calls = ArrayList<String>()

    fun add(call: String) = synchronized(calls) { calls.add(call) }

    fun snapshot(): List<String> = synchronized(calls) { calls.toList() }

    /** Returns the calls recorded so far and forgets them. */
    fun take(): List<String> = synchronized(calls) { calls.toList().also { calls.clear() } }
}

class FakeSurfaceBackend(private val log: CallLog) : SurfaceBackend {

    @Volatile
    override var hasContext = false
        private set

    @Volatile
    override var hasSurface = false
        private set

    /** Results handed out by [present] before it falls back to [PresentResult.OK]. */
    val presentResults = ConcurrentLinkedQueue<PresentResult>()

    /** Results handed out by [createSurface] before it falls back to `true`. */
    val createSurfaceResults = ConcurrentLinkedQueue<Boolean>()

    /** Results handed out by [makeCurrent] before it falls back to `true`. */
    val makeCurrentResults = ConcurrentLinkedQueue<Boolean>()

    @Volatile
    var createContextFailure: RuntimeException? = null

    /** Keeps a real render thread from spinning through frames as fast as it can. */
    @Volatile
    var presentDelayMillis = 0L

    /**
     * The render thread releases the surface with its lock held, so this stalls the lock on purpose: a UI thread
     * call that did not wait for the release would return before the release is recorded.
     */
    @Volatile
    var destroySurfaceDelayMillis = 0L

    override fun createContext() {
        log.add("createContext")
        createContextFailure?.let { throw it }
        hasContext = true
    }

    override fun destroyContext() {
        log.add("destroyContext")
        hasContext = false
    }

    /** The window passed to the latest [createSurface]. */
    @Volatile
    var lastWindow: Surface? = null
        private set

    override fun createSurface(window: Surface): Boolean {
        log.add("createSurface")
        lastWindow = window
        hasSurface = createSurfaceResults.poll() ?: true
        return hasSurface
    }

    override fun destroySurface() {
        if (destroySurfaceDelayMillis > 0) {
            Thread.sleep(destroySurfaceDelayMillis)
        }
        log.add("destroySurface")
        hasSurface = false
    }

    override fun makeCurrent(): Boolean {
        log.add("makeCurrent")
        return makeCurrentResults.poll() ?: true
    }

    override fun present(): PresentResult {
        log.add("present")
        if (presentDelayMillis > 0) {
            Thread.sleep(presentDelayMillis)
        }
        return presentResults.poll() ?: PresentResult.OK
    }

    override fun setSwapInterval(interval: Int) {
        log.add("setSwapInterval($interval)")
    }

    /** The values passed to [setPresentationTime], in order. */
    val presentationTimes = CopyOnWriteArrayList<Long>()

    override fun setPresentationTime(nanos: Long) {
        log.add("setPresentationTime")
        presentationTimes.add(nanos)
    }

    override fun clearPresentationTime() {
        log.add("clearPresentationTime")
    }
}

class RecordingFrameRenderer(private val log: CallLog) : FrameRenderer {

    @Volatile
    var onSwapped: Runnable? = null

    /** The size of the last frame presented, as reported by [onSurfaceChanged]. */
    @Volatile
    var presentedSize = ""

    @Volatile
    private var size = ""

    override fun onContextCreated() {
        log.add("onContextCreated")
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        log.add("onSurfaceChanged($width,$height)")
        size = "${width}x$height"
    }

    override fun onDrawFrame() {
        log.add("onDrawFrame")
    }

    override fun onFrameSwapped() {
        log.add("onFrameSwapped")
        presentedSize = size
        onSwapped?.run()
    }
}
