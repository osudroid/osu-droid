package org.andengine.graphics.view

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.PerformanceHintManager
import java.util.function.IntSupplier
import java.util.function.LongSupplier
import org.andengine.util.debug.Debug

/**
 * Reports the work of each frame to the system's performance hint manager, so that it can raise the clocks of the
 * render and update threads before frames miss their deadline instead of after. Does nothing below API 31 or when
 * the system declines a session. A session whose calls fail is dropped, so hints can never stop rendering.
 *
 * The reported work is the longer of the render thread's CPU time and the last update tick: the session covers
 * both threads, and a frame is late when either of them is. The render thread's CPU time is used rather than its
 * wall time, since waiting for the GPU or the display is not work the CPU clocks can shorten.
 *
 * [setTargetFrameRate] may be called from any thread; every other method is called on the render thread.
 *
 * @param updateThreadId The thread id of the update thread, or 0 while it is not known yet.
 * @param updateNanos The duration of the last update tick, in nanoseconds, or 0 while there was none.
 */
class PerformanceHints(
    private val factory: SessionFactory,
    private val updateThreadId: IntSupplier,
    private val updateNanos: LongSupplier,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) : FrameWorkListener {

    /** The calls made on a hint session, narrowed so that the platform class can be replaced in tests. */
    interface Session {
        fun updateTargetWorkDuration(targetNanos: Long)
        fun reportActualWorkDuration(workNanos: Long)
        fun close()
    }

    interface SessionFactory {
        /** @return The session, or `null` if the system declines one. */
        fun createSession(threadIds: IntArray, targetNanos: Long): Session?
    }

    @Volatile
    private var targetNanos = DEFAULT_TARGET_NANOS

    // Render thread only.
    private var session: Session? = null
    private var sessionTargetNanos = 0L
    private var renderThreadId = 0
    // The thread ids the last session was requested for, so that a declined or failed session is not requested
    // again every frame.
    private var requestedRenderThreadId = 0
    private var requestedUpdateThreadId = 0

    /** Sets the work duration every frame should stay within to one refresh period. Ignores rates that are not positive. */
    fun setTargetFrameRate(rate: Float) {
        if (rate > 0f) {
            targetNanos = (NANOS_PER_SECOND / rate).toLong()
        }
    }

    override fun onRenderThreadStarted(threadId: Int) {
        if (threadId != renderThreadId) {
            closeSession()
            renderThreadId = threadId
        }
    }

    override fun onRenderThreadExited() {
        closeSession()
        renderThreadId = 0
    }

    override fun onFrameWork(workNanos: Long) {
        if (sdkInt < Build.VERSION_CODES.S || renderThreadId == 0) {
            return
        }

        val updateId = updateThreadId.asInt
        if (updateId != requestedUpdateThreadId || renderThreadId != requestedRenderThreadId) {
            if (updateId == 0) {
                return
            }
            closeSession()
            requestedRenderThreadId = renderThreadId
            requestedUpdateThreadId = updateId
            sessionTargetNanos = targetNanos
            session = try {
                factory.createSession(intArrayOf(renderThreadId, updateId), sessionTargetNanos)
            } catch (e: RuntimeException) {
                Debug.w("Performance hint session not available: $e")
                null
            }
        }

        val session = session ?: return

        try {
            val target = targetNanos
            if (target != sessionTargetNanos) {
                sessionTargetNanos = target
                session.updateTargetWorkDuration(target)
            }

            val frameNanos = maxOf(workNanos, updateNanos.asLong)
            if (frameNanos > 0L) {
                session.reportActualWorkDuration(frameNanos)
            }
        } catch (e: RuntimeException) {
            // The requested thread ids are kept, so the session is treated like a declined one and not requested
            // again until a thread changes.
            Debug.w("Performance hint session failed, dropping it: $e")
            releaseSession()
        }
    }

    private fun closeSession() {
        releaseSession()
        requestedRenderThreadId = 0
        requestedUpdateThreadId = 0
    }

    private fun releaseSession() {
        val session = session ?: return
        this.session = null
        try {
            session.close()
        } catch (e: RuntimeException) {
            Debug.w("Performance hint session could not be closed: $e")
        }
    }

    /** Creates sessions through the system's [PerformanceHintManager]. */
    class PlatformSessionFactory(private val context: Context) : SessionFactory {

        override fun createSession(threadIds: IntArray, targetNanos: Long): Session? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return null
            }
            return PlatformSession.create(context, threadIds, targetNanos)
        }
    }

    @TargetApi(Build.VERSION_CODES.S)
    private class PlatformSession(private val session: PerformanceHintManager.Session) : Session {

        override fun updateTargetWorkDuration(targetNanos: Long) {
            session.updateTargetWorkDuration(targetNanos)
        }

        override fun reportActualWorkDuration(workNanos: Long) {
            session.reportActualWorkDuration(workNanos)
        }

        override fun close() {
            session.close()
        }

        companion object {
            fun create(context: Context, threadIds: IntArray, targetNanos: Long): Session? {
                val manager = context.getSystemService(PerformanceHintManager::class.java) ?: return null
                return manager.createHintSession(threadIds, targetNanos)?.let { PlatformSession(it) }
            }
        }
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val DEFAULT_TARGET_NANOS = 16_666_666L
    }
}
