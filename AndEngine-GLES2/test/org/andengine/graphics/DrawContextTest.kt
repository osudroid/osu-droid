package org.andengine.graphics

import android.opengl.GLES20
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.IDENTITY
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.opengl.util.GLMatrixStack
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.andengine.graphics.gles.endOpenPass

// Robolectric for android.opengl.Matrix (the matrix stacks).
@RunWith(RobolectricTestRunner::class)
class DrawContextTest {

    private lateinit var gl: RecordingGL
    private lateinit var texture: GlesTexture
    private val context = DrawContext()

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "a") as GlesTexture
        QuadBatcher.setProjection(IDENTITY)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
        texture.dispose()
        GlesBackend.state.beforeExternalChange = null
        GlesBackend.state.beforeScissorChange = null
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        RenderStats.enabled = false
    }

    @Test
    fun matrixStacks_pushPopTransformAndOrtho() {
        context.resetModelViewGLMatrixStack()
        context.resetProjectionGLMatrixStack()
        val translated = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 10f, 20f, 0f, 1f)

        context.translateModelViewGLMatrixf(10f, 20f, 0f)
        context.pushModelViewGLMatrix()
        context.rotateModelViewGLMatrixf(90f, 0f, 0f, 1f)
        context.scaleModelViewGLMatrixf(2f, 3f, 1f)
        context.skewModelViewGLMatrixf(45f, 0f)
        assertArrayEquals(
            floatArrayOf(-8.742278E-8f, 2.0f, 0f, 0f, -3.0f, -2.0000002f, 0f, 0f, 0f, 0f, 1f, 0f, 10f, 20f, 0f, 1f),
            context.modelViewGLMatrix, 0f
        )
        context.popModelViewGLMatrix()
        assertArrayEquals(translated, context.modelViewGLMatrix, 0f)

        context.pushModelViewGLMatrix()
        context.loadModelViewGLMatrixIdentity()
        context.translateModelViewGLMatrixf(1f, 2f, 3f)
        assertArrayEquals(floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 2f, 3f, 1f), context.modelViewGLMatrix, 0f)
        context.popModelViewGLMatrix()
        assertArrayEquals(translated, context.modelViewGLMatrix, 0f)

        val ortho = floatArrayOf(0.0025f, 0f, 0f, 0f, 0f, -0.004166667f, 0f, 0f, 0f, 0f, -1f, 0f, -1f, 1f, -0f, 1f)
        context.orthoProjectionGLMatrixf(0f, 800f, 480f, 0f, -1f, 1f)
        assertArrayEquals(ortho, context.projectionGLMatrix, 0f)
        context.pushProjectionGLMatrix()
        context.translateProjectionGLMatrixf(400f, 240f, 0f)
        context.rotateProjectionGLMatrixf(180f, 0f, 0f, 1f)
        context.translateProjectionGLMatrixf(-400f, -240f, 0f)
        assertArrayEquals(
            floatArrayOf(-0.0025f, 3.642616E-10f, 0f, 0f, 2.1855694E-10f, 0.004166667f, 0f, 0f, 0f, 0f, -1f, 0f, 0.99999994f, -1.0000001f, 0f, 1f),
            context.projectionGLMatrix, 0f
        )
        context.popProjectionGLMatrix()
        assertArrayEquals(ortho, context.projectionGLMatrix, 0f)
        context.loadProjectionGLMatrixIdentity()
        assertArrayEquals(IDENTITY, context.projectionGLMatrix, 0f)

        // Reads reuse one array per stack, and a reset goes back to a single identity matrix.
        assertSame(context.modelViewGLMatrix, context.modelViewGLMatrix)
        assertSame(context.projectionGLMatrix, context.projectionGLMatrix)
        context.pushModelViewGLMatrix()
        context.resetModelViewGLMatrixStack()
        assertArrayEquals(IDENTITY, context.modelViewGLMatrix, 0f)
        assertThrows(GLMatrixStack.GLMatrixStackUnderflowException::class.java) { context.popModelViewGLMatrix() }
    }

    @Test
    fun drawContext_readsStateSetThroughCache() {
        // The context keeps no state of its own: a change made through the cache is what it sees,
        // and it does not repeat it.
        GlesBackend.state.setScissorEnabled(true)
        assertTrue(context.isScissorTestEnabled)
        assertTrue(context.enableScissorTest())
        GlesBackend.state.setDepthTestEnabled(true)
        assertTrue(context.disableDepthTest())
        assertFalse(context.disableDepthTest())
        assertEquals(1, gl.calls.count { it == "glEnable(${GLES20.GL_SCISSOR_TEST})" })
        assertEquals(1, gl.calls.count { it == "glEnable(${GLES20.GL_DEPTH_TEST})" })
        assertEquals(1, gl.calls.count { it == "glDisable(${GLES20.GL_DEPTH_TEST})" })
    }

    @Test
    fun drawContext_stateChangesFlushBatchFirst() {
        val depth = GLES20.GL_DEPTH_TEST
        GlesBackend.state.setDepthTestEnabled(true)
        assertTrue(GlesBackend.state.isDepthTestEnabled)
        gl.calls.clear()

        // A queued quad is drawn before the depth test changes.
        QuadBatcher.add(texture, BlendMode.Alpha, IDENTITY, 0f, 0f, 10f, 10f, 0f, 0f, 1f, 1f, -1)
        assertTrue(QuadBatcher.hasPending)
        assertTrue(context.disableDepthTest())
        assertFalse(GlesBackend.state.isDepthTestEnabled)
        assertFalse(QuadBatcher.hasPending)
        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        assertTrue(gl.calls.toString(), draw >= 0)
        assertTrue(gl.calls.toString(), draw < gl.calls.lastIndexOf("glDisable($depth)"))

        // A change that is already in effect neither flushes nor reaches GL.
        val hooks = ArrayList<String>()
        val state = GlesBackend.state
        state.beforeExternalChange = Runnable { hooks += "external"; gl.calls += "external" }
        state.beforeScissorChange = Runnable { hooks += "scissor"; gl.calls += "scissor" }
        gl.calls.clear()
        assertFalse(context.disableDepthTest())
        assertTrue(hooks.isEmpty())
        assertTrue(gl.calls.isEmpty())

        // The scissor test runs only the scissor hook, right before it reaches GL.
        assertFalse(context.enableScissorTest())
        assertTrue(context.isScissorTestEnabled)
        assertEquals(listOf("scissor", "glEnable(${GLES20.GL_SCISSOR_TEST})"), gl.calls)
        state.beforeScissorChange = Runnable { hooks += "scissor"; gl.calls += "scissor" }
        gl.calls.clear()
        assertTrue(context.disableScissorTest())
        assertFalse(context.isScissorTestEnabled)
        assertEquals(listOf("scissor", "glDisable(${GLES20.GL_SCISSOR_TEST})"), gl.calls)
        assertEquals(listOf("scissor", "scissor"), hooks)

        // The line width does not flush; it only skips duplicates.
        gl.calls.clear()
        context.lineWidth(3f)
        context.lineWidth(3f)
        assertEquals(listOf("glLineWidth(3.0)"), gl.calls)
        assertEquals(listOf("scissor", "scissor"), hooks)
        assertSame(state, GlesBackend.state)
    }
}
