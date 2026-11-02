package org.andengine.graphics.view

import android.os.Debug
import android.view.Surface
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

    @Volatile
    var frameWorkListener: FrameWorkListener? = null

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
    private var lastVsyncNanos = 0L
    private var vsyncWaiter: VsyncWaiter? = null

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
    fun runOneIterationForTest(): Boolean = iterate(blocking = false)

    override fun run() {
        var failure: Throwable? = null

        try {
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
                val original = failure ?: throw e
                original.addSuppressed(e)
            } finally {
                lock.withLock {
                    exited = true
                    stateChanged.signalAll()
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
            backend.setPresentationTime(if (lastVsyncNanos != 0L) lastVsyncNanos else System.nanoTime())
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
                renderer.onFrameSwapped()

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

        RenderTiming.lastWorkNanos = workNanos

        lastVsyncNanos = if (pacing.choreographerPacingEnabled) {
            (vsyncWaiter ?: vsyncWaiterFactory().also { vsyncWaiter = it }).waitForVsync()
        } else {
            0L
        }

        frameWorkListener?.onFrameWork(workNanos)
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
                    return window
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
