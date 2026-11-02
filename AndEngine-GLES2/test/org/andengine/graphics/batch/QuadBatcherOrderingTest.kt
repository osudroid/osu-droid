package org.andengine.graphics.batch

import android.opengl.GLES20
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.GlesTextures
import org.andengine.graphics.gles.IDENTITY
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.indexOfCall
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.gles.lastCallBefore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.andengine.graphics.gles.endOpenPass

/** The batch must not change the order of what reaches the screen. */
@RunWith(RobolectricTestRunner::class)
class QuadBatcherOrderingTest {

    private lateinit var gl: RecordingGL
    private lateinit var texture: GlesTexture
    private val glState = DrawContext()

    private val triangle = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)

    private fun drawMesh() = MeshRenderer.draw(
        PrimitiveType.Triangles, triangle, 0, 3, IDENTITY, IDENTITY, -1, BlendMode.Alpha, null, false
    )

    private fun newContext() {
        gl = installRecordingGL()
    }

    private fun add() = QuadBatcher.add(texture, BlendMode.Alpha, IDENTITY, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)

    private fun indexOfBatchDraw() = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }

    private fun assertBatchDrawnBefore(call: String) {
        val batch = indexOfBatchDraw()
        assertTrue("no batch draw in ${gl.calls}", batch >= 0)
        assertTrue(gl.calls.toString(), batch < gl.indexOfCall(call))
    }

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        newContext()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "t") as GlesTexture
        QuadBatcher.setProjection(IDENTITY)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        MeshRenderer.releaseResources()
        endOpenPass()
        texture.dispose()
        RenderStats.enabled = false
    }

    @Test
    fun meshDraw_afterQuads_leavesStateFoundBeforeThem() {
        // The first mesh draw creates its buffer; the state below is set up after that.
        drawMesh()
        gl.calls.clear()
        // Set up by someone else before the quad was added: the flush and the mesh both put it back.
        val state = GlesBackend.state
        state.useProgram(77)
        state.bindTexture(GLES20.GL_TEXTURE_2D, 55)
        state.setBlendEnabled(false)
        state.bindArrayBuffer(33)
        add()

        drawMesh()

        assertBatchDrawnBefore("glDrawArrays(${GLES20.GL_TRIANGLES},0,3)")
        assertFalse(QuadBatcher.hasPending)
        val end = gl.calls.size
        assertEquals("glUseProgram(77)", gl.lastCallBefore(end, "glUseProgram"))
        assertEquals("glBindTexture(${GLES20.GL_TEXTURE_2D},55)", gl.lastCallBefore(end, "glBindTexture(${GLES20.GL_TEXTURE_2D},"))
        val blendCalls = setOf("glEnable(${GLES20.GL_BLEND})", "glDisable(${GLES20.GL_BLEND})")
        assertEquals("glDisable(${GLES20.GL_BLEND})", gl.calls.lastOrNull { it in blendCalls })
        assertEquals("glBindBuffer(${GLES20.GL_ARRAY_BUFFER},33)", gl.lastCallBefore(end, "glBindBuffer(${GLES20.GL_ARRAY_BUFFER}"))
        assertEquals("glBindVertexArray(0)", gl.lastCallBefore(end, "glBindVertexArray"))
    }

    @Test
    fun externalStateChange_flushes() {
        add()

        GlesTextures.bind(99)

        assertBatchDrawnBefore("glBindTexture(${GLES20.GL_TEXTURE_2D},99)")
        assertFalse(QuadBatcher.hasPending)
        // The cache knows the binding: a second bind is skipped.
        GlesTextures.bind(99)
        assertEquals(1, gl.calls.count { it == "glBindTexture(${GLES20.GL_TEXTURE_2D},99)" })
    }

    @Test
    fun externalStateChange_keepsActiveTextureUnit() {
        GlesBackend.state.activeTexture(1)
        add()

        GlesTextures.bind(99)

        val bind = gl.indexOfCall("glBindTexture(${GLES20.GL_TEXTURE_2D},99)")
        assertTrue(indexOfBatchDraw() in 0 until bind)
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE1})", gl.lastCallBefore(bind, "glActiveTexture"))
    }

    @Test
    fun dedupedStateChange_doesNotFlush() {
        GlesTextures.bind(99)
        add()

        GlesTextures.bind(99)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(-1, indexOfBatchDraw())
    }

    @Test
    fun scissorChange_doesNotFlushUnclippedBatch() {
        add()

        GlesBackend.state.setScissorEnabled(true)
        GlesBackend.state.scissor(1, 2, 3, 4)

        // The batch draws with the scissor test off, so the GL scissor does not matter to it.
        assertTrue(QuadBatcher.hasPending)
        assertEquals(-1, indexOfBatchDraw())
    }

    @Test
    fun scissorChange_doesNotFlushCpuClippedBatch() {
        QuadBatcher.setClip(0, 0, 1, 1)
        // Mostly outside the clip: cut on the CPU.
        QuadBatcher.add(texture, BlendMode.Alpha, IDENTITY, -1f, -1f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
        try {
            GlesBackend.state.setScissorEnabled(true)
            GlesBackend.state.scissor(1, 2, 3, 4)

            assertTrue(QuadBatcher.hasPending)
            assertEquals(-1, indexOfBatchDraw())
        } finally {
            QuadBatcher.clearClip()
        }
    }

    @Test
    fun scissorChange_flushesScissorKeyedBatch() {
        // Turned by 45 degrees: cannot be cut on the CPU, so it is drawn with the GL scissor.
        val s = Math.sqrt(0.5).toFloat()
        val turned = floatArrayOf(s, s, 0f, 0f, -s, s, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
        QuadBatcher.setClip(0, 0, 400, 240)
        QuadBatcher.add(texture, BlendMode.Alpha, turned, -1f, -1f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
        try {
            GlesBackend.state.setScissorEnabled(true)

            // The batch enables the scissor for its own draw as well; the change asked for is the last.
            val draw = indexOfBatchDraw()
            assertTrue(gl.calls.toString(), draw in 0 until gl.calls.lastIndexOf("glEnable(${GLES20.GL_SCISSOR_TEST})"))
            assertFalse(QuadBatcher.hasPending)

            QuadBatcher.add(texture, BlendMode.Alpha, turned, -1f, -1f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
            GlesBackend.state.scissor(1, 2, 3, 4)

            assertEquals(2, gl.calls.count { it.startsWith("glDrawElements") })
            assertTrue(gl.calls.indexOfLast { it.startsWith("glDrawElements") } < gl.indexOfCall("glScissor(1,2,3,4)"))
        } finally {
            QuadBatcher.clearClip()
        }
    }

    @Test
    fun framebufferAndViewportChange_flush() {
        add()
        GlesBackend.state.bindFramebuffer(5)
        assertBatchDrawnBefore("glBindFramebuffer(${GLES20.GL_FRAMEBUFFER},5)")

        gl.calls.clear()
        add()
        GlesBackend.state.viewport(0, 0, 10, 10)
        assertBatchDrawnBefore("glViewport(0,0,10,10)")
    }

    @Test
    fun textureDispose_flushesFirst() {
        add()

        texture.dispose()

        assertBatchDrawnBefore(gl.calls.first { it.startsWith("glDeleteTextures") })
    }

    @Test
    fun newContext_rearms() {
        add()
        QuadBatcher.flush()
        newContext()
        gl.calls.clear()
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "t2") as GlesTexture
        gl.calls.clear()
        add()

        GlesTextures.bind(99)

        assertBatchDrawnBefore("glBindTexture(${GLES20.GL_TEXTURE_2D},99)")
    }

    @Test
    fun beginPass_flushes() {
        add()

        GlesRenderContext.instance.beginPass(null, floatArrayOf(0f, 0f, 0f, 1f), false)
        GlesRenderContext.instance.endPass()

        assertBatchDrawnBefore("glClear(${GLES20.GL_COLOR_BUFFER_BIT})")
    }

    @Test
    fun frameEnd_flushes() {
        val engine = Engine(EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f)))
        val lock = engine.engineLock
        val notifyCanDraw = Engine.EngineLock::class.java.getDeclaredMethod("notifyCanDraw")
        notifyCanDraw.isAccessible = true
        // Normally the update thread allows the draw; signalling needs the lock.
        lock.lock()
        try {
            notifyCanDraw.invoke(lock)
        } finally {
            lock.unlock()
        }
        gl.calls.clear()
        add()

        engine.onDrawFrame(glState)

        assertFalse(QuadBatcher.hasPending)
        assertTrue(gl.calls.toString(), indexOfBatchDraw() >= 0)
    }
}
