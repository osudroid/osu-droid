package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.DrawContext
import org.andengine.graphics.view.IRendererListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log in GlesBackend.
@RunWith(RobolectricTestRunner::class)
class GlesEngineRendererTest {

    private class CountingEngine(options: EngineOptions) : Engine(options) {
        var swaps = 0
        var onDraw: () -> Unit = {}

        override fun onFrameSwapped() {
            swaps++
        }

        override fun onDrawFrame(pDrawContext: DrawContext) {
            onDraw()
        }
    }

    private class RecordingListener : IRendererListener {
        var created = 0
        val sizes = ArrayList<String>()

        override fun onSurfaceCreated(pDrawContext: DrawContext) {
            created++
        }

        override fun onSurfaceChanged(pDrawContext: DrawContext, pWidth: Int, pHeight: Int) {
            sizes += "${pWidth}x$pHeight"
        }
    }

    private class CountingRestorable : GlesRestorable {
        override val restorePhase = RestorePhase.Legacy
        var restores = 0

        override fun onContextLost() {}

        override fun restore() {
            restores++
        }
    }

    private lateinit var gl: RecordingGL
    private lateinit var engine: CountingEngine
    private lateinit var listener: RecordingListener
    private lateinit var renderer: GlesEngineRenderer
    private val restorable = CountingRestorable()

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        installRecordingGL()
        gl = RecordingGL().apply {
            intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
            intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        }
        val options = EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f))
        options.renderOptions.isDithering = true
        engine = CountingEngine(options)
        listener = RecordingListener()
        renderer = GlesEngineRenderer(engine, GlesConfigChooser(false), listener) { gl }
        GlesBackend.registry.register(restorable)
    }

    @After
    fun tearDown() {
        GlesDebugOptions.debugOutput = false
        GlesDebugOutput.uninstall()
        GlesBackend.registry.unregister(restorable)
        GlesLegacyRestorers.clearForTest()
    }

    @Test
    fun onContextCreated_restoresAndResetsDrawState() {
        renderer.onContextCreated()

        assertEquals(1, restorable.restores)
        assertTrue(gl.calls.toString(), "glEnable(${GLES20.GL_BLEND})" in gl.calls)
        assertTrue(gl.calls.toString(), "glEnable(${GLES20.GL_DITHER})" in gl.calls)
        assertEquals(1, listener.created)
    }

    @Test
    fun onSurfaceChanged_setsViewportAndSurfaceSize() {
        renderer.onContextCreated()
        gl.calls.clear()

        renderer.onSurfaceChanged(800, 480)

        assertTrue(gl.calls.toString(), "glViewport(0,0,800,480)" in gl.calls)
        assertEquals(800, GlesBackend.surfaceWidth)
        assertEquals(480, GlesBackend.surfaceHeight)
        assertEquals(listOf("800x480"), listener.sizes)
    }

    @Test
    fun onDrawFrame_clearsDepthThenDrawsThenPollsDebugOutput() {
        val options = EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f))
        val engine = CountingEngine(options).also { it.onDraw = { gl.calls += "engine.onDrawFrame" } }
        val renderer = GlesEngineRenderer(engine, GlesConfigChooser(false), null) { gl }
        // Polling instead of the callback, so the per-frame poll shows up in the recorded calls.
        gl.debugCallbackUnsupported = true
        GlesDebugOptions.debugOutput = true
        renderer.onContextCreated()
        gl.calls.clear()

        renderer.onDrawFrame()

        val depthClear = gl.indexOfCall("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})")
        val draw = gl.indexOfCall("engine.onDrawFrame")
        val poll = gl.calls.indexOfFirst { it.startsWith("glGetDebugMessageLog(") }
        assertTrue(gl.calls.toString(), depthClear in 0 until draw && draw < poll)
    }

    @Test
    fun onFrameSwapped_forwardsToEngine() {
        renderer.onFrameSwapped()

        assertEquals(1, engine.swaps)
    }
}
