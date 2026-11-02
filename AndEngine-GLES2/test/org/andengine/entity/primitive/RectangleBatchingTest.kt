package org.andengine.entity.primitive

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.entity.primitive.vbo.HighPerformanceRectangleVertexBufferObject
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
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

    private fun vbo() = HighPerformanceRectangleVertexBufferObject(
        null, Rectangle.RECTANGLE_SIZE, DrawType.STATIC, false, Rectangle.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
    )

    private fun rect(x: Float = 10f) = Rectangle(x, 0f, 8f, 6f, vbo())

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
    fun rectangleSubclassWithOwnDraw_notBatched() {
        val r = object : Rectangle(0f, 0f, 8f, 6f, vbo()) {
            override fun draw(pGLState: GLState, pCamera: Camera) = super.draw(pGLState, pCamera)
        }
        assertFalse(RectangleBatching.canBatch(r))
    }

    @Test
    fun plainSubclass_canBatch() {
        assertTrue(RectangleBatching.canBatch(object : Rectangle(0f, 0f, 8f, 6f, vbo()) {}))
    }

    @Test
    fun rectangleWithCustomShader_notBatched() {
        val r = rect()
        r.shaderProgram = ShaderProgram("void main(){}", "void main(){}")
        assertFalse(RectangleBatching.canBatch(r))
    }
}
