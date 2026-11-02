package org.andengine.entity.primitive

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.opengl.util.GLState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RectangleBatchingTest {

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private lateinit var masks: ShapeMasks
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        masks = ShapeMasks(atlas)
        ShapeBatching.masks = masks
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        ShapeBatching.masks = null
        val array = masks.solid().array
        masks.dispose()
        if (array.hasTexture) array.texture.dispose()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
    }

    private fun rect(x: Float = 10f) = Rectangle(x, 0f, 8f, 6f)

    private fun vertices(): ByteBuffer = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }
    private fun legacyDraws() = gl.calls.count { it.startsWith("glDrawArrays") }

    @Test
    fun plainRectangle_addsOneBatchedQuad() {
        rect().onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
        QuadBatcher.flush()
        assertEquals(1, draws())
        val v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        assertEquals(10f, v.getFloat(0), 0f)
        assertEquals(0f, v.getFloat(4), 0f)
        assertEquals(18f, v.getFloat(48), 0f)
        assertEquals(6f, v.getFloat(52), 0f)
    }

    @Test
    fun rectangleColorAndBlend_carriedOver() {
        val r = rect()
        r.setColor(1f, 0f, 0f, 0.5f)
        r.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        r.onDraw(glState, camera)
        QuadBatcher.flush()

        val v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        val packed = r.color.abgrPackedInt
        assertEquals(packed and 0xFF, v.get(16).toInt() and 0xFF)
        assertEquals((packed shr 8) and 0xFF, v.get(17).toInt() and 0xFF)
        assertEquals((packed shr 16) and 0xFF, v.get(18).toInt() and 0xFF)
        assertEquals((packed ushr 24) and 0xFF, v.get(19).toInt() and 0xFF)
        assertEquals(127, v.get(19).toInt() and 0xFF)
        val expected = "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun transparentRectangle_addsNothing() {
        val r = rect()
        r.setColor(1f, 1f, 1f, 0f)
        r.onDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
    }

    @Test
    fun rectangle_colorReadAtDraw() {
        val r = rect()
        r.setColor(1f, 0f, 0f, 1f)
        r.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(255, vertices().get(16).toInt() and 0xFF)
        assertEquals(0, vertices().get(18).toInt() and 0xFF)

        r.setColor(0f, 0f, 1f)
        r.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(0, vertices().get(16).toInt() and 0xFF)
        assertEquals(255, vertices().get(18).toInt() and 0xFF)
    }

    @Test
    fun rectangle_sizeReadAtDraw() {
        val r = rect()
        r.setSize(20f, 4f)
        r.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(30f, vertices().getFloat(48), 0f)
        assertEquals(4f, vertices().getFloat(52), 0f)
    }

    @Test
    fun rectangleSubclassWithOwnDraw_stillBatched() {
        val r = object : Rectangle(0f, 0f, 8f, 6f) {
            override fun preDraw(pGLState: GLState, pCamera: Camera) = super.preDraw(pGLState, pCamera)
            override fun draw(pGLState: GLState, pCamera: Camera) = super.draw(pGLState, pCamera)
            override fun postDraw(pGLState: GLState, pCamera: Camera) = super.postDraw(pGLState, pCamera)
        }
        r.onDraw(glState, camera)
        assertTrue(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertEquals(0, legacyDraws())
        assertEquals(1, draws())
    }

    @Test
    fun rectangle_unknownBlendFallsBackToAlpha() {
        val r = rect()
        r.setBlendFunction(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE)
        r.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(1, draws())
        val alpha = "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == alpha })
    }
}
