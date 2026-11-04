package org.andengine.graphics.gles

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.os.Build
import android.view.Surface
import org.andengine.graphics.view.PresentResult
import org.andengine.graphics.view.SurfaceBackend
import org.andengine.opengl.exception.GLESVersionUnsupportedException
import org.andengine.util.debug.Debug

/**
 * [SurfaceBackend] on EGL14: an OpenGL ES 3.2 context (3.0 if 3.2 is unavailable) with a config from
 * [configChooser], presenting to window surfaces. Render thread only.
 */
class EglSurfaceBackend(private val configChooser: GlesConfigChooser) : SurfaceBackend {

    // Held once per context and surface: the EGL14 getters allocate a wrapper object on every call.
    private var display: EGLDisplay? = null
    private var config: EGLConfig? = null
    private var context: EGLContext? = null
    private var surface: EGLSurface? = null

    private var presentationTimeUnsupported = false

    /** Whether the current surface was given an explicit presentation time that has not been cleared yet. */
    private var hasPresentationTime = false

    override val hasContext: Boolean get() = context != null

    override val hasSurface: Boolean get() = surface != null

    override fun createContext() {
        check(context == null) { "Context already created" }

        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed: ${eglErrorString()}" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed: ${eglErrorString()}" }

        val config = configChooser.chooseConfig(display)

        var context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, CONTEXT_ATTRIBUTES_32, 0)
        if (context != null && context != EGL14.EGL_NO_CONTEXT) {
            Debug.i("Created OpenGL ES 3.2 context.")
        } else {
            Debug.w("OpenGL ES 3.2 not available, falling back to ES 3.0.")
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, CONTEXT_ATTRIBUTES_30, 0)
            if (context == null || context == EGL14.EGL_NO_CONTEXT) {
                Debug.e("Failed to create OpenGL ES 3.0 or 3.2 context.")
                val detected = detectedRenderableType(display, config)
                EGL14.eglTerminate(display)
                throw GLESVersionUnsupportedException(
                    "OpenGL ES 3.0",
                    detected,
                    Build.MANUFACTURER + " " + Build.MODEL,
                    Build.HARDWARE,
                    Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")",
                )
            }
            Debug.i("Created OpenGL ES 3.0 context.")
        }

        this.display = display
        this.config = config
        this.context = context
    }

    override fun destroyContext() {
        val context = context ?: return
        destroySurface()
        val display = display!!
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (!EGL14.eglDestroyContext(display, context)) {
            Debug.w("eglDestroyContext failed: ${eglErrorString()}")
        }
        EGL14.eglTerminate(display)
        this.context = null
        this.config = null
        this.display = null
    }

    override fun createSurface(window: Surface): Boolean {
        check(context != null) { "No context to create a surface for" }
        check(surface == null) { "Surface already created" }

        val surface = EGL14.eglCreateWindowSurface(display, config, window, SURFACE_ATTRIBUTES, 0)
        if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
            Debug.w("eglCreateWindowSurface failed: ${eglErrorString()}")
            return false
        }

        this.surface = surface
        // A new native window starts out with automatic timestamps.
        hasPresentationTime = false
        return true
    }

    override fun destroySurface() {
        val surface = surface ?: return
        val display = display!!
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (!EGL14.eglDestroySurface(display, surface)) {
            Debug.w("eglDestroySurface failed: ${eglErrorString()}")
        }
        this.surface = null
        hasPresentationTime = false
    }

    override fun makeCurrent(): Boolean {
        val surface = surface ?: return false
        val context = context ?: return false
        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
            Debug.w("eglMakeCurrent failed: ${eglErrorString()}")
            return false
        }
        return true
    }

    override fun present(): PresentResult {
        if (EGL14.eglSwapBuffers(display, surface)) {
            return PresentResult.OK
        }

        return when (val error = EGL14.eglGetError()) {
            EGL14.EGL_CONTEXT_LOST -> PresentResult.CONTEXT_LOST
            EGL14.EGL_BAD_SURFACE, EGL14.EGL_BAD_NATIVE_WINDOW -> PresentResult.SURFACE_LOST
            else -> {
                // Any other swap failure leaves the surface unusable as well; the context is still intact.
                Debug.w("eglSwapBuffers failed: " + errorString(error))
                PresentResult.SURFACE_LOST
            }
        }
    }

    override fun setSwapInterval(interval: Int) {
        if (!EGL14.eglSwapInterval(display, interval)) {
            Debug.w("eglSwapInterval(" + interval + ") failed: " + eglErrorString())
        }
    }

    /** Warns once and does nothing from then on if the device lacks `EGL_ANDROID_presentation_time`. */
    override fun setPresentationTime(nanos: Long) {
        if (presentationTimeUnsupported) {
            return
        }

        if (!EGLExt.eglPresentationTimeANDROID(display, surface, nanos)) {
            presentationTimeUnsupported = true
            Debug.w("eglPresentationTimeANDROID failed: " + eglErrorString() + ", not using presentation times")
            return
        }

        hasPresentationTime = true
    }

    /**
     * The native window stamps every later frame with the last presentation time it was given, and an explicit
     * timestamp lets the compositor drop queued frames, so it has to be reset to automatic explicitly.
     */
    override fun clearPresentationTime() {
        if (!hasPresentationTime) {
            return
        }

        hasPresentationTime = false
        EGLExt.eglPresentationTimeANDROID(display, surface, NATIVE_WINDOW_TIMESTAMP_AUTO)
    }

    /**
     * Reads the config's EGL_RENDERABLE_TYPE bitmask to report the highest ES version the hardware advertises,
     * since no context (and thus no GL_VERSION string) exists at this point.
     */
    private fun detectedRenderableType(display: EGLDisplay, config: EGLConfig): String {
        val value = IntArray(1)
        if (!EGL14.eglGetConfigAttrib(display, config, EGL14.EGL_RENDERABLE_TYPE, value, 0)) {
            return "Unknown"
        }

        val renderableType = value[0]
        return when {
            renderableType and EGLExt.EGL_OPENGL_ES3_BIT_KHR != 0 -> "OpenGL ES 3.x"
            renderableType and EGL14.EGL_OPENGL_ES2_BIT != 0 -> "OpenGL ES 2.0"
            renderableType and EGL14.EGL_OPENGL_ES_BIT != 0 -> "OpenGL ES 1.x"
            else -> "Unknown"
        }
    }

    private fun eglErrorString() = errorString(EGL14.eglGetError())

    private fun errorString(error: Int) = "EGL error 0x" + Integer.toHexString(error)

    private companion object {
        /** Lets the native window pick the presentation time itself, as without a presentation time call. */
        const val NATIVE_WINDOW_TIMESTAMP_AUTO = Long.MIN_VALUE

        val CONTEXT_ATTRIBUTES_32 = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGLExt.EGL_CONTEXT_MINOR_VERSION_KHR, 2,
            EGL14.EGL_NONE,
        )

        val CONTEXT_ATTRIBUTES_30 = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE,
        )

        val SURFACE_ATTRIBUTES = intArrayOf(EGL14.EGL_NONE)
    }
}
