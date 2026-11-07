package com.reco1l.andengine.text

import org.andengine.graphics.BlendMode
import com.osudroid.testing.IDENTITY
import com.osudroid.testing.Page
import com.osudroid.testing.RecordingGl
import com.osudroid.testing.quadVerticesOf
import com.osudroid.testing.vertexLayersOf
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.osudroid.testing.endOpenPass

@RunWith(RobolectricTestRunner::class)
class UITextureTextBatchingTest {

    private lateinit var gl: RecordingGl
    private val drawContext = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        QuadBatcher.setProjection(IDENTITY)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    private fun draws() = gl.count("glDrawElements")

    /** Positions and UVs of the sprite quads of [upload]: x, y, u, v per corner, 4 corners per quad. */
    private fun quadsOf(upload: ByteArray): List<FloatArray> {
        assertTrue(vertexLayersOf(upload).all { it == QuadBatcher.SPRITE_LAYER })
        return quadVerticesOf(upload)
    }

    private fun lastQuads() = quadsOf(gl.vertexUploads.last())

    private fun draw(text: UITextureText) {
        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
        text.doDraw(drawContext, camera)
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
        assertFalse("No glyph is uploaded on its own", gl.calls.any { it.startsWith("glDrawArrays(") })
    }

    @Test
    fun glyphsOnOwnTextures_quadCoversCellAndWholeTexture() {
        // Each glyph on a texture of its own: its quad covers (0, 0)-(width, height) translated to
        // its cell and the whole texture.
        val sizes = mapOf('1' to (10 to 20), '2' to (14 to 18), '3' to (8 to 20))
        val characters = mutableMapOf<Char, TextureRegion>()
        var id = 10
        for ((char, size) in sizes) {
            val page = Page(id++, size.first, size.second)
            characters[char] = TextureRegion(page, 0f, 0f, size.first.toFloat(), size.second.toFloat())
        }

        val text = UITextureText(characters)
        text.spacing = 2f
        text.textureScaleX = 1.5f
        text.textureScaleY = 0.5f
        text.fixedCharWidths = mapOf('2' to 20f)
        text.text = "123"

        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
        drawContext.translateModelViewGLMatrixf(30f, 40f, 0f)

        text.doDraw(drawContext, camera)
        // Different textures: one draw per glyph.
        QuadBatcher.flush()
        assertEquals(3, draws())

        // Every glyph went out in its own upload; collect them.
        val actual = gl.vertexUploads.flatMap { quadsOf(it) }

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
        val second = UITextureText(characters).apply { text = "11"; blendMode = BlendMode.AdditiveAllChannels }
        val third = UITextureText(characters).apply { text = "11" }

        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
        first.doDraw(drawContext, camera)
        second.doDraw(drawContext, camera)
        third.doDraw(drawContext, camera)
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

        val buffer = gl.lastUpload()
        // Color bytes R, G, B, A follow position and UV.
        assertEquals(255, buffer.get(16).toInt() and 0xFF)
        assertEquals(0, buffer.get(17).toInt() and 0xFF)
        assertEquals(0, buffer.get(18).toInt() and 0xFF)
        // 0.5 * 255 rounds to 128, as for every batched quad.
        assertEquals(128, buffer.get(19).toInt() and 0xFF)
    }
}
