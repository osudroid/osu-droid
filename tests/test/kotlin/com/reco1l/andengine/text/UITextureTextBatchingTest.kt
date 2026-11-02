package com.reco1l.andengine.text

import android.opengl.GLES20
import com.reco1l.andengine.component.BlendInfo
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UITextureTextBatchingTest {

    private val glCalls = ArrayList<String>()

    /** The bytes of every vertex upload (glBufferData or glBufferSubData on the array buffer), in order. */
    private val vertexUploads = ArrayList<ByteArray>()

    private var nextId = 1
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    /** Fake GL that records calls, hands out ids and reports every compile and link as successful. */
    private fun fakeGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        glCalls += method.name
        when (method.name) {
            "glGetIntegerv" -> {
                val params = args!![1] as IntArray
                params[args[2] as Int] = when (args[0]) {
                    GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                    GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                    else -> 0
                }
            }
            "glGetShaderiv", "glGetProgramiv" -> (args!![2] as IntArray)[args[3] as Int] = 1
            "glGenBuffers", "glGenTextures", "glGenVertexArrays", "glGenSamplers", "glGenFramebuffers" -> {
                val ids = args!![1] as IntArray
                for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
            }
            "glBufferData" -> if (args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer) {
                val data = (args[2] as ByteBuffer).duplicate()
                data.position(0)
                val bytes = ByteArray(args[1] as Int)
                data.get(bytes)
                vertexUploads += bytes
            }
            "glBufferSubData" -> if (args!![0] == GLES20.GL_ARRAY_BUFFER) {
                val data = (args[3] as ByteBuffer).duplicate()
                data.position(0)
                val bytes = ByteArray(args[2] as Int)
                data.get(bytes)
                vertexUploads += bytes
            }
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (method.name == "glCreateShader" || method.name == "glCreateProgram") nextId++ else 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    private class Page(id: Int, size: Int) : BitmapTextureAtlas(TextureManager(), size, size, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = id
        }
    }

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(fakeGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        QuadBatcher.setProjection(identity)
        glCalls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
    }

    private fun draws() = glCalls.count { it == "glDrawElements" }

    /** Positions and UVs of the quads of the last vertex upload: x, y, u, v per corner, 4 corners per quad. */
    private fun lastQuads(): List<FloatArray> {
        val buffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        val quads = ArrayList<FloatArray>()
        while (buffer.remaining() >= QuadBatcher.VERTEX_SIZE_BYTES * 4) {
            val quad = FloatArray(16)
            for (corner in 0 until 4) {
                for (k in 0 until 4) quad[corner * 4 + k] = buffer.float
                buffer.int // color
                assertEquals(QuadBatcher.SPRITE_LAYER, buffer.int and 0xFFFF) // layer marker + padding
            }
            quads += quad
        }
        return quads
    }

    private fun draw(text: UITextureText) {
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        text.doDraw(glState, camera)
        QuadBatcher.flush()
    }

    @Test
    fun fiveDigits_sameAtlasPage_oneDraw() {
        val page = Page(7, 64)
        val characters = mutableMapOf<Char, TextureRegion>()
        for (i in 0..9) characters['0' + i] = TextureRegion(page, (i % 4) * 16f, (i / 4) * 16f, 12f, 14f)

        val text = UITextureText(characters)
        text.text = "12345"
        draw(text)

        assertEquals(1, draws())
        assertEquals(5, lastQuads().size)
        assertFalse("No glyph is uploaded on its own", glCalls.contains("glDrawArrays"))
    }

    @Test
    fun glyphsOnTexturesOfTheirOwn_matchThePreviousQuads() {
        // Each glyph on a texture of its own, as the previous path assumed: its quad covered
        // (0, 0)-(width, height) translated to its cell and the whole texture.
        val sizes = mapOf('1' to (10 to 20), '2' to (14 to 18), '3' to (8 to 20))
        val characters = mutableMapOf<Char, TextureRegion>()
        var id = 10
        for ((char, size) in sizes) {
            val page = object : BitmapTextureAtlas(TextureManager(), size.first, size.second, TextureOptions.BILINEAR) {
                init { mHardwareTextureID = id++ }
            }
            characters[char] = TextureRegion(page, 0f, 0f, size.first.toFloat(), size.second.toFloat())
        }

        val text = UITextureText(characters)
        text.spacing = 2f
        text.textureScaleX = 1.5f
        text.textureScaleY = 0.5f
        text.fixedCharWidths = mapOf('2' to 20f)
        text.text = "123"

        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        glState.translateModelViewGLMatrixf(30f, 40f, 0f)

        text.doDraw(glState, camera)
        // Different textures: one draw per glyph.
        QuadBatcher.flush()
        assertEquals(3, draws())

        // Every glyph went out in its own upload; collect them.
        val actual = ArrayList<FloatArray>()
        for (upload in vertexUploads) {
            val buffer = ByteBuffer.wrap(upload).order(ByteOrder.nativeOrder())
            val quad = FloatArray(16)
            for (corner in 0 until 4) {
                for (k in 0 until 4) quad[corner * 4 + k] = buffer.float
                buffer.int // color
                assertEquals(QuadBatcher.SPRITE_LAYER, buffer.int and 0xFFFF) // layer marker + padding
            }
            actual += quad
        }

        var offsetX = 0f
        val expected = ArrayList<FloatArray>()
        for (char in "123") {
            val (w, h) = sizes.getValue(char)
            val width = w * 1.5f
            val height = h * 0.5f
            val cell = (text.fixedCharWidths!![char] ?: w.toFloat()) * 1.5f
            val x0 = 30f + offsetX + (cell - width) / 2f
            val y0 = 40f
            val x1 = x0 + width
            val y1 = y0 + height
            expected += floatArrayOf(
                x0, y0, 0f, 0f,
                x1, y0, 1f, 0f,
                x1, y1, 1f, 1f,
                x0, y1, 0f, 1f
            )
            offsetX += cell + 2f
        }

        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            for (k in 0 until 16) assertEquals("glyph $i value $k", expected[i][k], actual[i][k], 1e-4f)
        }
    }

    @Test
    fun atlasRegion_usesTheRegionUVs() {
        val page = Page(7, 64)
        val region = TextureRegion(page, 16f, 32f, 8f, 16f)
        val text = UITextureText(mutableMapOf('5' to region))
        text.text = "5"
        draw(text)

        val quad = lastQuads().single()
        assertEquals(0.25f, quad[2], 0f)
        assertEquals(0.5f, quad[3], 0f)
        assertEquals(0.375f, quad[10], 0f)
        assertEquals(0.75f, quad[11], 0f)
    }

    @Test
    fun unloadedGlyphTexture_isSkipped() {
        val loaded = Page(7, 64)
        val unloaded = Page(-1, 64)
        val text = UITextureText(mutableMapOf(
            '1' to TextureRegion(loaded, 0f, 0f, 8f, 8f),
            '2' to TextureRegion(unloaded, 0f, 0f, 8f, 8f)
        ))
        text.text = "121"
        draw(text)

        assertEquals(1, draws())
        assertEquals(2, lastQuads().size)
    }

    @Test
    fun additiveBlend_breaksTheBatchFromAlphaText() {
        val page = Page(7, 64)
        val characters = mutableMapOf<Char, TextureRegion>('1' to TextureRegion(page, 0f, 0f, 8f, 8f))
        val first = UITextureText(characters).apply { text = "11" }
        val second = UITextureText(characters).apply { text = "11"; blendInfo = BlendInfo.Additive }
        val third = UITextureText(characters).apply { text = "11" }

        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        first.doDraw(glState, camera)
        second.doDraw(glState, camera)
        third.doDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(3, draws())
    }

    @Test
    fun drawColor_isPacked() {
        val page = Page(7, 64)
        val text = UITextureText(mutableMapOf('1' to TextureRegion(page, 0f, 0f, 8f, 8f)))
        text.text = "1"
        text.setColor(1f, 0f, 0f)
        text.alpha = 0.5f
        draw(text)

        val buffer = ByteBuffer.wrap(vertexUploads.last())
        // Color bytes R, G, B, A follow position and UV.
        assertEquals(255, buffer.get(16).toInt() and 0xFF)
        assertEquals(0, buffer.get(17).toInt() and 0xFF)
        assertEquals(0, buffer.get(18).toInt() and 0xFF)
        // 0.5 * 255 rounds to 128, as for every batched quad.
        assertEquals(128, buffer.get(19).toInt() and 0xFF)
    }
}
