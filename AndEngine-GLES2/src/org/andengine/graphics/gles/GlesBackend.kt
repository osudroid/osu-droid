package org.andengine.graphics.gles

import android.opengl.GLES20
import android.util.Log
import org.andengine.graphics.atlas.TextureAtlasPages
import org.andengine.graphics.debug.RenderStats

/**
 * Process-wide entry point to the GLES backend: the GL interfaces, the capabilities and the state
 * cache of the current context, plus the [registry] that is replayed on every new context.
 * GL thread only (except [registry], which is thread-safe).
 */
object GlesBackend {

    @JvmStatic
    lateinit var gl: GL30
        private set

    /** Non-null when the context is ES 3.1 or newer. */
    @JvmStatic
    var gl31: GL31? = null
        private set

    /** Non-null when the context is ES 3.2. */
    @JvmStatic
    var gl32: GL32? = null
        private set

    @JvmStatic
    lateinit var caps: GlesCapabilities
        private set

    /** Whether a context has been installed, so that [caps] can be read. */
    @JvmStatic
    val hasCaps: Boolean get() = ::caps.isInitialized

    @JvmStatic
    lateinit var state: GlesStateCache
        private set

    @JvmStatic
    val registry = GlesResourceRegistry()

    /** Size of the screen surface in pixels, set by `GlesEngineRenderer.onSurfaceChanged`. */
    @JvmField
    var surfaceWidth = 0

    @JvmField
    var surfaceHeight = 0

    /**
     * Call once per new EGL context, on the GL thread, before anything draws. Detects the
     * capabilities, installs a fresh (empty) state cache and restores every registered resource.
     * Tests pass a fake [gl].
     */
    @JvmStatic
    @JvmOverloads
    fun onContextCreated(gl: GL32 = AndroidGL32()) {
        val versionString = gl.glGetString(GLES20.GL_VERSION)
        val maxTextureSize = IntArray(1)
        gl.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0)
        val caps = GlesCapabilities.detect(
            versionString,
            gl.glGetString(GLES20.GL_EXTENSIONS),
            maxTextureSize[0]
        )
        this.gl = gl
        this.caps = caps
        gl31 = if (caps.hasGl31) gl else null
        gl32 = if (caps.hasGl32) gl else null
        state = GlesStateCache(gl)

        // Only through the ES 3.2 core entry points: with GL_KHR_debug on an older context the
        // *KHR variants would be needed. Installed before the restore so its errors are reported too.
        val debugGl = if (GlesDebugOptions.debugOutput && caps.hasDebugOutput) gl32 else null
        Log.i(
            TAG,
            "context: ES ${caps.version.major}.${caps.version.minor} ($versionString) " +
                "renderer=${gl.glGetString(GLES20.GL_RENDERER)} " +
                "simulateEs30=${GlesDebugOptions.simulateEs30} debugOutput=${debugGl != null}"
        )

        if (debugGl != null) GlesDebugOutput.install(debugGl) else GlesDebugOutput.uninstall()

        registry.restoreAll()
    }

    /**
     * Sets the start state of a new context, after [onContextCreated] and before anything draws:
     * VAO 0, no culling, depth test off with a clear depth of 1, `GL_LESS` and the depth mask on,
     * blending on and dithering as [dithering] says. Also publishes the maximum texture size to
     * the atlas pages and resets the render statistics. GL thread only.
     */
    @JvmStatic
    fun resetDrawState(dithering: Boolean) {
        val state = state
        // The restore may have bound another VAO.
        state.bindVertexArray(0)
        state.setCullFaceEnabled(false)

        TextureAtlasPages.deviceMaxTextureSize = caps.maxTextureSize
        RenderStats.reset()

        state.setDepthTestEnabled(false)
        // A clear depth of 1.0 (maximum) lets GL_LESS tests pass on the first draw and makes the
        // per-entity depth buffer clears work.
        state.clearDepth(1.0f)
        state.depthFunc(GLES20.GL_LESS)
        state.depthMask(true)
        state.setBlendEnabled(true)
        state.setDitherEnabled(dithering)
        /* Enabling culling doesn't really make sense, because triangles are never drawn 'backwards' on purpose. */
    }

    /**
     * Starts a frame: clears the depth buffer, so depth values left by the previous frame (slider
     * bodies) never affect this one. `glClear` honours the depth mask, which a batch flush may have
     * left off, so it is turned on first. GL thread only; does not allocate.
     */
    @JvmStatic
    fun beginFrame() {
        val state = state
        state.depthMask(true)
        state.clear(GLES20.GL_DEPTH_BUFFER_BIT)
    }

    private const val TAG = "GlesBackend"
}
