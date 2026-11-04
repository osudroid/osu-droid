package org.andengine.graphics.view

import android.view.Surface

/**
 * The window-system binding of a [RenderThread]: owns the GL context and the surface frames are presented to.
 * Every method is called from the render thread only.
 */
interface SurfaceBackend {

    val hasContext: Boolean

    val hasSurface: Boolean

    /** @throws org.andengine.opengl.exception.GLESVersionUnsupportedException If no supported context can be created. */
    fun createContext()

    fun destroyContext()

    /** @return Whether a surface for [window] could be created. */
    fun createSurface(window: Surface): Boolean

    fun destroySurface()

    /** @return Whether the context and surface could be bound to the calling thread. */
    fun makeCurrent(): Boolean

    fun present(): PresentResult

    fun setSwapInterval(interval: Int)

    /** Sets when the next presented frame should be shown, in the [System.nanoTime] time base. */
    fun setPresentationTime(nanos: Long)

    fun clearPresentationTime()
}

enum class PresentResult {
    OK,

    /** The context and everything created with it is gone. */
    CONTEXT_LOST,

    /** The surface can no longer be presented to, but the context is intact. */
    SURFACE_LOST
}
