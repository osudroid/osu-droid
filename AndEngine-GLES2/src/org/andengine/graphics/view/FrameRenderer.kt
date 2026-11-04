package org.andengine.graphics.view

/** Draws the frames of a [RenderThread]. Every method is called on the render thread with the context current. */
interface FrameRenderer {

    /** Called for every newly created context, before anything is drawn with it. */
    fun onContextCreated()

    /** Called after every newly created surface and whenever the surface size changes. */
    fun onSurfaceChanged(width: Int, height: Int)

    fun onDrawFrame()

    /** Called after a frame drawn by [onDrawFrame] has been presented successfully. */
    fun onFrameSwapped()
}

/** Receives the CPU time a [RenderThread] spent drawing each presented frame; waiting for the display or other threads is not counted. */
interface FrameWorkListener {

    /** Called on the render thread when it starts, with its thread id, before any frame is reported. */
    fun onRenderThreadStarted(threadId: Int) {}

    fun onFrameWork(workNanos: Long)

    /** Called on the render thread when it ends, after its last frame was reported. */
    fun onRenderThreadExited() {}
}
