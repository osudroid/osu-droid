/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 11:57:29 - 08.03.2010
 */
package org.andengine.graphics.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.IntSupplier
import java.util.function.LongSupplier
import org.andengine.engine.Engine
import org.andengine.graphics.gles.EglSurfaceBackend
import org.andengine.graphics.gles.GlesConfigChooser
import org.andengine.graphics.gles.GlesEngineRenderer
import org.andengine.util.debug.Debug

/**
 * Shows the frames of an [Engine], drawn on a [RenderThread] that runs while the view is attached to a window.
 * Every method is called from the UI thread.
 */
open class RenderSurfaceView : SurfaceView, SurfaceHolder.Callback2 {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /** The frame pacing of the render thread; kept across the threads started for this view. */
    val framePacing = FramePacing()

    /** See [FramePacing.vsyncEnabled]. */
    fun setVSyncEnabled(enabled: Boolean) {
        framePacing.vsyncEnabled = enabled
    }

    /** See [FramePacing.presentationTimeEnabled]. */
    fun setPresentationTimeEnabled(enabled: Boolean) {
        framePacing.presentationTimeEnabled = enabled
    }

    /** See [FramePacing.choreographerPacingEnabled]. */
    fun setChoreographerPacingEnabled(enabled: Boolean) {
        framePacing.choreographerPacingEnabled = enabled
    }

    private var engine: Engine? = null

    // Kept across the threads started for this view, so that a restarted thread replaces the old session.
    private val performanceHints = PerformanceHints(
        PerformanceHints.PlatformSessionFactory(context),
        // The update thread is never restarted, so its id stays valid for the engine's lifetime.
        IntSupplier { engine?.updateThreadId ?: 0 },
        LongSupplier { engine?.lastUpdateNanos ?: 0L }
    )

    /** The refresh rate of the display, in Hz, which every frame's work should fit into. See [PerformanceHints]. */
    fun setTargetFrameRate(rate: Float) {
        performanceHints.setTargetFrameRate(rate)
    }

    private var renderer: GlesEngineRenderer? = null
    private var configChooser: GlesConfigChooser? = null
    private var renderThread: RenderThread? = null

    // A thread that did not end within requestExitAndWait()'s timeout. It still uses the engine and the renderer,
    // so no other thread is started while it is alive.
    private var exitingRenderThread: RenderThread? = null

    // The window state, replayed to a thread that starts while the surface already exists.
    private var hasSurface = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var paused = false

    init {
        holder.addCallback(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val engine = engine
        if (isInEditMode || engine == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        engine.engineOptions.resolutionPolicy.onMeasure(this, widthMeasureSpec, heightMeasureSpec)
    }

    fun setMeasuredDimensionProxy(measuredWidth: Int, measuredHeight: Int) {
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /** Must be called once, before the view is attached to a window. */
    fun setRenderer(engine: Engine, rendererListener: IRendererListener?) {
        check(renderer == null) { "setRenderer has already been called for this instance." }

        val configChooser = configChooser
            ?: GlesConfigChooser(
                engine.engineOptions.renderOptions.isMultiSampling,
                engine.engineOptions.renderOptions.colorDepth,
            ).also { this.configChooser = it }

        setOnTouchListener(engine)
        this.engine = engine
        renderer = GlesEngineRenderer(engine, configChooser, rendererListener)

        if (isAttachedToWindow) {
            startRenderThread()
        }
    }

    /** Releases the surface while keeping the context, so GPU resources survive a background/foreground transition. */
    fun onPause() {
        paused = true
        renderThread?.onPause()
    }

    fun onResume() {
        paused = false
        renderThread?.onResume()
    }

    /**
     * Debug aid: destroys the context and creates a new one, as the system does under memory pressure, so that
     * the resource restore path can be exercised on demand. The engine and the activity are not paused.
     */
    fun debugRecreateContext() {
        renderThread?.requestRecreateContext()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (renderer != null && renderThread == null) {
            startRenderThread()
        }
    }

    override fun onDetachedFromWindow() {
        val thread = renderThread
        if (thread != null) {
            thread.requestExitAndWait()
            if (thread.isAlive) {
                exitingRenderThread = thread
            }
        }
        renderThread = null
        super.onDetachedFromWindow()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        hasSurface = true
        renderThread?.surfaceCreated(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        renderThread?.surfaceChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        hasSurface = false
        renderThread?.surfaceDestroyed()
    }

    /**
     * Lets the window wait for a frame drawn for its current state before it is shown, e.g. after a resize or
     * when it becomes visible. Gives up after [REDRAW_TIMEOUT_MILLIS], so a stuck render thread cannot keep the
     * window from being shown.
     */
    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) {
        val request = RedrawRequest(drawingFinished)
        val thread = renderThread
        if (thread == null) {
            request.run()
            return
        }
        postDelayed(request, REDRAW_TIMEOUT_MILLIS)
        thread.requestRedraw(request)
    }

    @Deprecated("Deprecated in Java")
    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        val drawn = CountDownLatch(1)
        surfaceRedrawNeededAsync(holder) { drawn.countDown() }
        try {
            drawn.await(REDRAW_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    /** Reports the redraw finished exactly once: after the frame, or on the timeout, whichever comes first. */
    private class RedrawRequest(private val drawingFinished: Runnable) : Runnable {

        private val finished = AtomicBoolean(false)

        override fun run() {
            // Also runs on the render thread, so the pending timeout is not removed here: View.removeCallbacks
            // belongs to the UI thread, and the flag already turns the timeout into a no-op.
            if (finished.compareAndSet(false, true)) {
                drawingFinished.run()
            }
        }
    }

    private fun startRenderThread() {
        val previous = exitingRenderThread
        if (previous != null) {
            if (previous.isAlive) {
                previous.requestExitAndWait()
            }
            if (previous.isAlive) {
                Debug.w("Previous render thread is still running, not starting another one.")
                return
            }
            exitingRenderThread = null
        }

        val thread = RenderThread(EglSurfaceBackend(configChooser!!), renderer!!, framePacing)
        thread.frameWorkListener = performanceHints
        renderThread = thread
        thread.start()

        if (paused) {
            thread.onPause()
        }
        if (hasSurface) {
            thread.surfaceCreated(holder.surface)
            thread.surfaceChanged(surfaceWidth, surfaceHeight)
        }
    }

    private companion object {
        // Matches the time the render thread's other UI calls wait for an acknowledgement.
        const val REDRAW_TIMEOUT_MILLIS = 2000L
    }
}
