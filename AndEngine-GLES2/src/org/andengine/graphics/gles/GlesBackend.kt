package org.andengine.graphics.gles

import android.opengl.GLES20
import android.util.Log

/**
 * Process-wide entry point to the GLES backend: the GL interfaces, the capabilities and the state
 * cache of the current context, plus the [registry] that is replayed on every new context.
 * GL thread only (except [registry], which is thread-safe).
 */
object GlesBackend {

    @JvmStatic lateinit var gl: GL30
        private set

    /** Non-null when the context is ES 3.1 or newer. */
    @JvmStatic var gl31: GL31? = null
        private set

    /** Non-null when the context is ES 3.2. */
    @JvmStatic var gl32: GL32? = null
        private set

    @JvmStatic lateinit var caps: GlesCapabilities
        private set

    @JvmStatic lateinit var state: GlesStateCache
        private set

    @JvmStatic val registry = GlesResourceRegistry()

    /** Size of the screen surface in pixels, set by `EngineRenderer.onSurfaceChanged`. */
    @JvmField var surfaceWidth = 0

    @JvmField var surfaceHeight = 0

    /**
     * Call once per new EGL context, on the GL thread, before anything draws. Detects the
     * capabilities, installs a fresh (empty) state cache and restores every registered resource.
     */
    @JvmStatic
    @JvmOverloads
    fun onContextCreated(gl: AndroidGL32 = AndroidGL32()) = install(gl)

    /** Same as [onContextCreated], with a fake GL. */
    fun onContextCreatedForTest(gl: GL32) = install(gl)

    private fun install(gl: GL32) {
        val versionString = gl.glGetString(GLES20.GL_VERSION)
        val caps = GlesCapabilities.detect(
            versionString,
            gl.glGetString(GLES20.GL_EXTENSIONS),
            GlesDebugOptions
        )
        this.gl = gl
        this.caps = caps
        gl31 = if (caps.hasGl31) gl else null
        gl32 = if (caps.hasGl32) gl else null
        state = GlesStateCache(gl)

        // Only through the ES 3.2 core entry points: with GL_KHR_debug on an older context the
        // *KHR variants would be needed. Installed before the restore so its errors are reported too.
        val gl32 = gl32
        val debugOutput = GlesDebugOptions.debugOutput && caps.hasDebugOutput && gl32 != null
        Log.i(
            TAG,
            "context: ES ${caps.version.major}.${caps.version.minor} ($versionString) " +
                "renderer=${gl.glGetString(GLES20.GL_RENDERER)} " +
                "simulateEs30=${GlesDebugOptions.simulateEs30} debugOutput=$debugOutput"
        )

        if (debugOutput && gl32 != null) GlesDebugOutput.install(gl32) else GlesDebugOutput.uninstall()

        registry.restoreAll(this)
    }

    private const val TAG = "GlesBackend"
}
