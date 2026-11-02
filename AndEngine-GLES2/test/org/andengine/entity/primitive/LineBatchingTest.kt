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
class LineBatchingTest {

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

    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }
    private fun legacyDraws() = gl.calls.count { it.startsWith("glDrawArrays") }

    private fun vertices(): ByteBuffer = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    private fun corners(v: ByteBuffer): Set<Pair<Float, Float>> =
        (0 until 4).map { v.getFloat(it * QuadBatcher.VERTEX_SIZE_BYTES) to v.getFloat(it * QuadBatcher.VERTEX_SIZE_BYTES + 4) }.toSet()

    @Test
    fun line_drawsThroughShapeBatching() {
        val line = Line(10f, 0f, 30f, 0f, 4f)
        line.setColor(1f, 0f, 0f, 0.5f)
        line.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertEquals(0, legacyDraws())
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glLineWidth") })
        assertEquals(1, draws())

        val v = vertices()
        // A quad of the line width, centered on the segment.
        assertEquals(setOf(10f to 2f, 30f to 2f, 30f to -2f, 10f to -2f), corners(v))
        // The color sits 16 bytes into each vertex.
        assertEquals(255, v.get(16).toInt() and 0xFF)
        assertEquals(0, v.get(18).toInt() and 0xFF)
        assertEquals(127, v.get(19).toInt() and 0xFF)
    }

    @Test
    fun line_endPointsAndWidthReadAtDraw() {
        val line = Line(0f, 0f, 10f, 0f)
        line.setPosition(0f, 0f, 0f, 20f)
        line.lineWidth = 2f
        line.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(setOf(-1f to 0f, -1f to 20f, 1f to 20f, 1f to 0f), corners(vertices()))
    }

    @Test
    fun line_blendFunctionCarriedOver() {
        val line = Line(0f, 0f, 10f, 0f)
        line.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        line.onDraw(glState, camera)
        QuadBatcher.flush()

        val expected = "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun line_unknownBlendFallsBackToAlpha() {
        val line = Line(0f, 0f, 10f, 0f)
        line.setBlendFunction(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE)
        line.onDraw(glState, camera)
        QuadBatcher.flush()

        val alpha = "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == alpha })
    }

    @Test
    fun line_collisionUnchanged() {
        val a = Line(0f, 0f, 10f, 10f)
        assertTrue(a.collidesWith(Line(0f, 10f, 10f, 0f)))
        assertFalse(a.collidesWith(Line(20f, 0f, 30f, 0f)))
        assertTrue(a.collidesWith(Rectangle(4f, 4f, 2f, 2f)))
    }
}
