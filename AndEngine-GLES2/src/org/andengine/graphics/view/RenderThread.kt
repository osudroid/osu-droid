package org.andengine.graphics.view

import android.os.Debug
import android.os.Process
import android.view.Surface
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.andengine.graphics.debug.RenderStats
import org.andengine.util.debug.Debug as EngineDebug

/**
 * Renders frames continuously on its own thread and drives the lifecycle of the backend's context and surface.
 *
 * The window callbacks and [onPause]/[onResume] are called from the UI thread. The context is kept across
 * pauses, only the surface is released. [FrameRenderer.onContextCreated] is called for every new context and
 * [FrameRenderer.onSurfaceChanged] after every new surface and every size change.
 *
 * Exceptions thrown by the backend or the renderer are not caught: they end the thread and reach its
 * uncaught exception handler.
 *
 * @param vsyncWaiterFactory Creates the waiter used while [FramePacing.choreographerPacingEnabled] is set,
 * on the first frame that needs it.
 */
class RenderThread(
    private val backend: SurfaceBackend,
    private val renderer: FrameRenderer,
    private val pacing: FramePacing,
    private val vsyncWaiterFactory: () -> VsyncWaiter = { VsyncWaiter() }
) : Thread(THREAD_NAME) {

    /** Read once when the thread starts; set it before [start]. */
    @Volatile
    var frameWorkListener: FrameWorkListener? = null

    // The listener read at the start, render thread only: the one told about the start also gets every frame and the end.
    private var workListener: FrameWorkListener? = null

    private val lock = ReentrantLock()

    // Signalled on every state change, for the render thread as well as for UI threads awaiting an acknowledgement.
    private val stateChanged = lock.newCondition()

    // Requests, guarded by lock.
    private var window: Surface? = null
    // Counts surfaceCreated calls: the holder hands out the same Surface object for every new native window.
    private var windowGeneration = 0
    private var width = 0
    private var height = 0
    private var sizeChanged = false
    private var sizeGeneration = 0
    private var pauseRequested = false
    private var recreateContextRequested = false
    private var exitRequested = false
    // Callbacks of redraw requests not yet taken by a frame.
    private val pendingRedraws = ArrayList<Runnable>(2)

    // Acknowledgements, guarded by lock.
    private var paused = false
    private var windowReleased = true
    private var surfaceFailed = false
    private var presentedSizeGeneration = 0
    private var exited = false

    // Render thread only.
    private var exiting = false
    private var contextIsNew = false
    private var reportSize = false
    private var frameWidth = 0
    private var frameHeight = 0
    private var frameSizeGeneration = 0
    private var frameWindowGeneration = 0
    private var surfaceWindowGeneration = 0
    private var acknowledgedSizeGeneration = 0
    private var presentationTimeSet = false
    private var vsyncWaiter: VsyncWaiter? = null
    // Redraw callbacks to run once the current frame is done, or at once if no frame can be drawn.
    private var frameRedraws = ArrayList<Runnable>(2)
    // Swapped with frameRedraws while its callbacks run, so each is taken out before it runs and none can run twice.
    private var runningRedraws = ArrayList<Runnable>(2)

    fun surfaceCreated(surface: Surface) = lock.withLock {
        window = surface
        windowGeneration++
        windowReleased = false
        surfaceFailed = false
        stateChanged.signalAll()
    }

    /**
     * Returns once a frame with the new size has been presented, so the window never shows a frame drawn for the
     * old size stretched to the new one. Returns at once if no frame can be drawn (paused, no window, zero size or
     * the thread is not running) and gives up after 2000 ms.
     */
    fun surfaceChanged(width: Int, height: Int) = lock.withLock {
        this.width = width
        this.height = height
        sizeChanged = true
        val generation = ++sizeGeneration
        stateChanged.signalAll()
        awaitAcknowledgement { presentedSizeGeneration - generation >= 0 || !canDrawLocked() }
    }

    /** Returns once the render thread no longer uses the window, since the window becomes invalid afterwards. */
    fun surfaceDestroyed() = lock.withLock {
        window = null
        stateChanged.signalAll()
        awaitAcknowledgement { windowReleased }
    }

    /**
     * Returns once the render thread has paused and released its surface, or after 2000 ms if the thread has not
     * paused by then.
     */
    fun onPause() = lock.withLock {
        pauseRequested = true
        stateChanged.signalAll()
        awaitAcknowledgement { paused }
    }

    /** Returns once the render thread has resumed, or after 2000 ms if it has not resumed by then. */
    fun onResume() = lock.withLock {
        pauseRequested = false
        stateChanged.signalAll()
        awaitAcknowledgement { !paused || pauseRequested }
    }

    /**
     * Runs [drawingFinished] once the window can be shown with up-to-date content: on the render thread, at the
     * latest when the next frame attempt that starts after this call ends, whether that frame was presented or not.
     * If the thread can no longer draw or exits before that, it runs then. Runs it at once, on the calling thread,
     * if no frame can be drawn (paused, no window, zero size or the thread has ended).
     */
    fun requestRedraw(drawingFinished: Runnable) {
        lock.withLock {
            if (!exited && !exitRequested && canDrawLocked()) {
                pendingRedraws.add(drawingFinished)
                stateChanged.signalAll()
                return
            }
        }
        drawingFinished.run()
    }

    /** Releases the surface and the context before the next frame, so that both are created anew. */
    fun requestRecreateContext() = lock.withLock {
        recreateContextRequested = true
        stateChanged.signalAll()
    }

    /** Stops the thread after its current frame, releasing the surface and the context, and waits for it to end. */
    fun requestExitAndWait() {
        lock.withLock {
            exitRequested = true
            stateChanged.signalAll()
        }

        if (currentThread() === this) {
            return
        }

        try {
            join(ACKNOWLEDGEMENT_TIMEOUT_MILLIS)
        } catch (e: InterruptedException) {
            currentThread().interrupt()
        }
    }

    /**
     * Runs a single iteration of the render loop on the calling thread. Where the loop would wait, it returns
     * instead.
     *
     * @return Whether [FrameRenderer.onDrawFrame] ran.
     */
    fun runOneIterationForTest(): Boolean {
        workListener = frameWorkListener
        return iterate(blocking = false)
    }

    override fun run() {
        var failure: Throwable? = null
        val workListener = frameWorkListener
        this.workListener = workListener

        try {
            try {
                workListener?.onRenderThreadStarted(Process.myTid())
            } catch (e: RuntimeException) {
                // Only the listener's own bookkeeping is missing; the frames are drawn regardless.
                EngineDebug.w("Frame work listener failed on start: $e")
            }
            while (!exiting) {
                iterate(blocking = true)
            }
        } catch (e: InterruptedException) {
            // An interrupt is a request to stop.
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            try {
                releaseSurface()
                releaseContext()
                vsyncWaiter?.quit()
                vsyncWaiter = null
            } catch (e: Throwable) {
                val original = failure
                if (original == null) {
                    failure = e
                    throw e
                }
                original.addSuppressed(e)
            } finally {
                try {
                    workListener?.onRenderThreadExited()
                } catch (e: Throwable) {
                    val original = failure ?: throw e
                    original.addSuppressed(e)
                } finally {
                    lock.withLock {
                        exited = true
                        takePendingRedrawsLocked()
                        stateChanged.signalAll()
                    }
                    runFrameRedraws(failure)
                }
            }
        }
    }

    // Must be called with the lock held. Gives up after a timeout so that a dead or stuck render thread cannot
    // block the UI thread forever.
    private inline fun awaitAcknowledgement(acknowledged: () -> Boolean) {
        if (currentThread() === this) {
            return
        }

        var remaining = TimeUnit.MILLISECONDS.toNanos(ACKNOWLEDGEMENT_TIMEOUT_MILLIS)

        while (!acknowledged() && !exited && isAlive && remaining > 0) {
            try {
                remaining = stateChanged.awaitNanos(remaining)
            } catch (e: InterruptedException) {
                currentThread().interrupt()
                return
            }
        }
    }

    private fun iterate(blocking: Boolean): Boolean {
        var failure: Throwable? = null
        try {
            return drawFrame(blocking)
        } catch (e: Throwable) {
            failure = e
            throw e
        } finally {
            // A frame that failed to present cannot be waited for either, the window would stay blank.
            runFrameRedraws(failure)
        }
    }

    /**
     * Runs every callback even if one throws, since each one holds back a window. The first failure is thrown
     * afterwards, or added to [failure] if one is already on its way so that it does not replace it.
     */
    private fun runFrameRedraws(failure: Throwable?) {
        if (frameRedraws.isEmpty()) {
            return
        }

        val running = frameRedraws
        frameRedraws = runningRedraws
        runningRedraws = running

        var first: Throwable? = null
        try {
            for (i in running.indices) {
                try {
                    running[i].run()
                } catch (e: Throwable) {
                    val original = failure ?: first
                    if (original == null) {
                        first = e
                    } else {
                        original.addSuppressed(e)
                    }
                }
            }
        } finally {
            running.clear()
        }

        if (first != null) {
            throw first
        }
    }

    private fun drawFrame(blocking: Boolean): Boolean {
        val window = awaitDrawableWindow(blocking) ?: return false
        if (!backend.hasContext) {
            backend.createContext()
            contextIsNew = true
        }

        if (!backend.hasSurface) {
            if (!backend.createSurface(window)) {
                markSurfaceFailed()
                return false
            }

            surfaceWindowGeneration = frameWindowGeneration
            reportSize = true
            presentationTimeSet = false
            pacing.markSurfaceRecreated()
        }

        if (!backend.makeCurrent()) {
            markSurfaceFailed()
            return false
        }

        if (contextIsNew) {
            contextIsNew = false
            renderer.onContextCreated()
        }

        if (reportSize) {
            reportSize = false
            renderer.onSurfaceChanged(frameWidth, frameHeight)
        }

        val swapInterval = pacing.takeSwapIntervalChange()
        if (swapInterval != -1) {
            backend.setSwapInterval(swapInterval)
        }

        val workStart = Debug.threadCpuTimeNanos()
        renderer.onDrawFrame()

        if (pacing.presentationTimeEnabled) {
            // Stamped when the frame is done: an earlier vsync deadline has usually passed by then and the frame
            // would be shown late.
            backend.setPresentationTime(System.nanoTime())
            presentationTimeSet = true
        } else if (presentationTimeSet) {
            backend.clearPresentationTime()
            presentationTimeSet = false
        }

        // CPU time, so that blocking in the buffer queue, the swap or the update lock does not count. Taken before
        // present(). Reported as 0 when the platform cannot measure it.
        val workEnd = Debug.threadCpuTimeNanos()
        val workNanos = if (workStart < 0L || workEnd < workStart) 0L else workEnd - workStart

        when (backend.present()) {
            PresentResult.OK -> {
                if (frameSizeGeneration != acknowledgedSizeGeneration) {
                    acknowledgedSizeGeneration = frameSizeGeneration
                    lock.withLock {
                        presentedSizeGeneration = frameSizeGeneration
                        stateChanged.signalAll()
                    }
                }
            }

            PresentResult.CONTEXT_LOST -> {
                releaseSurface()
                releaseContext()
                return true
            }

            PresentResult.SURFACE_LOST -> {
                markSurfaceFailed()
                return true
            }
        }

        // Before the vsync wait, which would only delay showing the window.
        runFrameRedraws(null)

        RenderTiming.lastWorkNanos = workNanos
        RenderStats.onRenderWork(workNanos)

        // Before the vsync wait as well, so that the hint is not a refresh period late.
        workListener?.onFrameWork(workNanos)

        if (pacing.choreographerPacingEnabled) {
            (vsyncWaiter ?: vsyncWaiterFactory().also { vsyncWaiter = it }).waitForVsync()
        }

        // After the vsync wait, so that an update released by the swap starts on the vsync and samples input as
        // late as possible.
        renderer.onFrameSwapped()

        return true
    }

    /**
     * Applies pending requests and acknowledges them, then waits until a frame can be drawn.
     *
     * @return The window to draw to, or `null` if the thread is exiting or, when not [blocking], if it would wait.
     */
    private fun awaitDrawableWindow(blocking: Boolean): Surface? {
        lock.lock()
        try {
            while (true) {
                if (exitRequested) {
                    exiting = true
                    return null
                }

                if (recreateContextRequested) {
                    recreateContextRequested = false
                    releaseSurface()
                    releaseContext()
                }

                if (pauseRequested) {
                    releaseSurface()
                    if (!paused) {
                        paused = true
                        stateChanged.signalAll()
                    }
                } else if (paused) {
                    paused = false
                    stateChanged.signalAll()
                }

                val window = window

                if (window == null) {
                    releaseSurface()
                    if (!windowReleased) {
                        windowReleased = true
                        stateChanged.signalAll()
                    }
                }

                if (window != null && canDrawLocked()) {
                    // A surfaceDestroyed() that timed out never let this thread see the window go away, so the
                    // surface may still belong to the previous native window.
                    if (surfaceWindowGeneration != windowGeneration) {
                        releaseSurface()
                    }
                    frameWindowGeneration = windowGeneration
                    if (sizeChanged) {
                        sizeChanged = false
                        reportSize = true
                    }
                    frameWidth = width
                    frameHeight = height
                    frameSizeGeneration = sizeGeneration
                    takePendingRedrawsLocked()
                    return window
                }

                // Nothing will be drawn until the state changes, so the callers are not kept waiting.
                takePendingRedrawsLocked()
                if (frameRedraws.isNotEmpty()) {
                    return null
                }

                if (!blocking) {
                    return null
                }

                stateChanged.await()
            }
        } finally {
            lock.unlock()
        }
    }

    // Must be called with the lock held.
    private fun takePendingRedrawsLocked() {
        if (pendingRedraws.isNotEmpty()) {
            // Element by element: addAll() would copy the list into a new array.
            for (i in pendingRedraws.indices) {
                frameRedraws.add(pendingRedraws[i])
            }
            pendingRedraws.clear()
        }
    }

    // Must be called with the lock held.
    private fun canDrawLocked() = window != null && !pauseRequested && !paused && !surfaceFailed && width > 0 && height > 0

    /** Releases the surface and waits for the next window, since retrying with this one would fail the same way. */
    private fun markSurfaceFailed() {
        releaseSurface()
        lock.withLock {
            surfaceFailed = true
            stateChanged.signalAll()
        }
    }

    private fun releaseSurface() {
        if (backend.hasSurface) {
            backend.destroySurface()
        }
    }

    private fun releaseContext() {
        if (backend.hasContext) {
            backend.destroyContext()
        }
    }

    companion object {
        /** Distinct from the platform's own "RenderThread", so the two can be told apart in ps and systrace. */
        const val THREAD_NAME = "EngineRender"

        private const val ACKNOWLEDGEMENT_TIMEOUT_MILLIS = 2000L
    }
}
