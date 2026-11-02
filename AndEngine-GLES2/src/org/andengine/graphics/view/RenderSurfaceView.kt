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
import org.andengine.engine.Engine
import org.andengine.graphics.gles.EglSurfaceBackend
import org.andengine.graphics.gles.GlesConfigChooser
import org.andengine.graphics.gles.GlesEngineRenderer
import org.andengine.util.debug.Debug

/**
 * Shows the frames of an [Engine], drawn on a [RenderThread] that runs while the view is attached to a window.
 * Every method is called from the UI thread.
 */
open class RenderSurfaceView : SurfaceView, SurfaceHolder.Callback {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /** The frame pacing of the render thread; kept across the threads started for this view. */
    val framePacing = FramePacing()

    private var engine: Engine? = null
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
            ?: GlesConfigChooser(engine.engineOptions.renderOptions.isMultiSampling).also { this.configChooser = it }

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
}
