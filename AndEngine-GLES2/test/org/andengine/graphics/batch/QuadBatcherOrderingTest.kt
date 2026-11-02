package org.andengine.graphics.batch

import android.opengl.GLES20
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.BlendMode
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.RecordingGL
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.VertexBufferObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The batch must not change the order of what reaches the screen. */
@RunWith(RobolectricTestRunner::class)
class QuadBatcherOrderingTest {

    private lateinit var gl: RecordingGL
    private lateinit var texture: GlesTexture
    private val glState = GLState()

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private class TestVbo : VertexBufferObject(null, 4, DrawType.STATIC, false, null) {
        override fun onBufferData() = Unit
        override fun getHeapMemoryByteSize() = 0
        override fun getNativeHeapMemoryByteSize() = 0
    }

    private fun newContext() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
    }

    private fun add() = QuadBatcher.add(texture, BlendMode.Alpha, identity, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)

    private fun indexOfBatchDraw() = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }

    private fun indexOf(call: String): Int {
        val index = gl.calls.indexOf(call)
        assertTrue("$call missing in ${gl.calls}", index >= 0)
        return index
    }

    private fun assertBatchDrawnBefore(call: String) {
        val batch = indexOfBatchDraw()
        assertTrue("no batch draw in ${gl.calls}", batch >= 0)
        assertTrue(gl.calls.toString(), batch < indexOf(call))
    }

    /** The last call starting with [prefix] before [index]. */
    private fun lastBefore(index: Int, prefix: String): String? =
        gl.calls.subList(0, index).lastOrNull { it.startsWith(prefix) }

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        newContext()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "t") as GlesTexture
        QuadBatcher.setProjection(identity)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        texture.dispose()
        RenderStats.enabled = false
    }

    @Test
    fun nonBatchedDraw_flushesPendingQuadsFirst() {
        add()

        TestVbo().draw(GLES20.GL_TRIANGLES, 3)

        assertBatchDrawnBefore("glDrawArrays(${GLES20.GL_TRIANGLES},0,3)")
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun nonBatchedDraw_drawsWithItsOwnState() {
        // Set up by the foreign draw before the quad was added: the flush must leave it in place.
        val state = GlesBackend.state
        state.useProgram(77)
        state.bindTexture(GLES20.GL_TEXTURE_2D, 55)
        state.setBlendEnabled(false)
        state.bindArrayBuffer(33)
        add()

        TestVbo().draw(GLES20.GL_TRIANGLES, 3)

        val draw = indexOf("glDrawArrays(${GLES20.GL_TRIANGLES},0,3)")
        assertEquals("glUseProgram(77)", lastBefore(draw, "glUseProgram"))
        assertEquals("glBindTexture(${GLES20.GL_TEXTURE_2D},55)", lastBefore(draw, "glBindTexture"))
        val blendCalls = setOf("glEnable(${GLES20.GL_BLEND})", "glDisable(${GLES20.GL_BLEND})")
        assertEquals("glDisable(${GLES20.GL_BLEND})", gl.calls.subList(0, draw).lastOrNull { it in blendCalls })
        assertEquals("glBindBuffer(${GLES20.GL_ARRAY_BUFFER},33)", lastBefore(draw, "glBindBuffer(${GLES20.GL_ARRAY_BUFFER}"))
        assertEquals("glBindVertexArray(0)", lastBefore(draw, "glBindVertexArray"))
    }

    @Test
    fun externalStateChange_flushes() {
        add()

        glState.bindTexture(99)

        assertBatchDrawnBefore("glBindTexture(${GLES20.GL_TEXTURE_2D},99)")
        assertFalse(QuadBatcher.hasPending)
        // The cache knows the binding: a second bind is skipped.
        glState.bindTexture(99)
        assertEquals(1, gl.calls.count { it == "glBindTexture(${GLES20.GL_TEXTURE_2D},99)" })
    }

    @Test
    fun externalStateChange_keepsActiveTextureUnit() {
        GlesBackend.state.activeTexture(1)
        add()

        glState.bindTexture(99)

        val bind = indexOf("glBindTexture(${GLES20.GL_TEXTURE_2D},99)")
        assertTrue(indexOfBatchDraw() in 0 until bind)
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE1})", lastBefore(bind, "glActiveTexture"))
    }

    @Test
    fun dedupedStateChange_doesNotFlush() {
        GlesBackend.state.useProgram(77)
        add()

        glState.useProgram(77)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(-1, indexOfBatchDraw())
    }

    @Test
    fun scissorChange_flushes() {
        add()

        GlesBackend.state.setScissorEnabled(true)

        assertBatchDrawnBefore("glEnable(${GLES20.GL_SCISSOR_TEST})")

        add()
        GlesBackend.state.scissor(1, 2, 3, 4)

        assertEquals(2, gl.calls.count { it.startsWith("glDrawElements") })
        assertTrue(gl.calls.lastIndexOf(gl.calls.last { it.startsWith("glDrawElements") }) < indexOf("glScissor(1,2,3,4)"))
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

        glState.bindTexture(99)

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
