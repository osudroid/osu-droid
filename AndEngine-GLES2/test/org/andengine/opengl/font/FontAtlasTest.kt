package org.andengine.opengl.font

import android.opengl.GLES20
import java.nio.ByteBuffer
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphMetrics
import org.andengine.graphics.text.GlyphRasterizer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FontAtlasTest {

    /** Every visible glyph is 20x30 raster pixels with advance 22; ' ' has no pixels. */
    private class FakeRasterizer(private val layers: Int = 1) : GlyphRasterizer {
        val rasterized = ArrayList<String>()

        override val ascent: Float get() = -40f
        override val descent: Float get() = 10f

        override fun measure(c: Char, out: GlyphMetrics) {
            if (c == ' ') {
                out.width = 0
                out.height = 0
                out.offsetX = 0f
                out.offsetY = 0f
                out.advance = 12f
                return
            }
            out.width = 20
            out.height = 30
            out.offsetX = 2f
            out.offsetY = -30f
            out.advance = 22f
        }

        override fun rasterize(c: Char, layer: Int, out: ByteBuffer) {
            require(layer < layers)
            rasterized += "$c$layer"
            for (i in 0 until 20 * 30) out.put(layer.toByte())
        }
    }

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private val fontManager = FontManager()
    private val fonts = ArrayList<Font>()
    private var baseline = 0

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        FontManager.setRasterScale(2f)
        baseline = uploads()
    }

    @After
    fun tearDown() {
        FontManager.setRasterScale(1f)
        val arrays = HashSet<GlyphArray>()
        for (font in fonts) {
            for (c in "AB ") {
                val letter = font.getLetter(c)
                letter.mGlyph?.let { arrays += it.array }
                letter.mStrokeGlyph?.let { arrays += it.array }
            }
        }
        for (a in arrays) if (a.hasTexture) a.texture.dispose()
    }

    private fun uploads() = gl.calls.count { it.startsWith("glTexSubImage3D") }

    private fun font(rasterizer: FakeRasterizer = FakeRasterizer()) = Font(fontManager, rasterizer, atlas).also { fonts += it }

    @Test
    fun letterMetrics_areVirtualUnits() {
        val font = font()
        val letter = font.getLetter('A')
        assertEquals(10f, letter.mWidth, 0f)
        assertEquals(15f, letter.mHeight, 0f)
        assertEquals(11f, letter.mAdvance, 0f)
        assertEquals(1f, letter.mOffsetX, 0f)
        // Top of the image below the line top: (-30 - (-40)) / 2.
        assertEquals(5f, letter.mOffsetY, 0f)
        assertEquals(25f, font.lineHeight, 0f)
    }

    @Test
    fun glyph_isAllocatedAtRasterSize() {
        val glyph = font().getLetter('A').mGlyph!!
        assertEquals(20, glyph.width)
        assertEquals(30, glyph.height)
    }

    @Test
    fun update_uploadsPendingGlyphsOnce() {
        val rasterizer = FakeRasterizer()
        val font = font(rasterizer)
        font.getLetter('A')
        font.getLetter('B')
        font.getLetter('A')
        assertEquals(0, uploads() - baseline)

        font.update()
        assertEquals(2, uploads() - baseline)
        assertEquals(listOf("A0", "B0"), rasterizer.rasterized)

        font.update()
        assertEquals(2, uploads() - baseline)
    }

    @Test
    fun contextLoss_reuploadsAllGlyphsInPlace() {
        val font = font()
        val a = font.getLetter('A')
        val b = font.getLetter('B')
        font.getLetter(' ')
        font.update()
        val slotA = a.mGlyph
        val slotB = b.mGlyph

        val reference = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        reference.allocate(20, 30)
        reference.allocate(20, 30)
        val expectedProbe = reference.allocate(5, 5)!!

        fontManager.loadFont(font)
        fontManager.onReload()
        assertFalse(slotA!!.uploaded)
        assertFalse(slotB!!.uploaded)
        val before = uploads()
        fontManager.updateFonts()
        assertTrue(slotA.uploaded)
        assertTrue(slotB!!.uploaded)

        assertEquals(2, uploads() - before)
        assertSame(slotA, font.getLetter('A').mGlyph)
        assertSame(slotB, font.getLetter('B').mGlyph)
        val upload = "glTexSubImage3D(${GL_TEXTURE_2D_ARRAY},0,${slotA!!.x - 1},${slotA.y - 1},${slotA.layer},22,32,1,"
        assertTrue(gl.calls.toString(), gl.calls.drop(before).any { it.startsWith(upload) })

        // No further space was taken from the atlas.
        val probe = atlas.allocate(5, 5)!!
        assertEquals(expectedProbe.x, probe.x)
        assertEquals(expectedProbe.y, probe.y)
        assertEquals(expectedProbe.layer, probe.layer)
    }

    @Test
    fun strokeFont_hasFillAndStrokeGlyphs() {
        val rasterizer = FakeRasterizer(layers = 2)
        val font = StrokeFont(fontManager, rasterizer, atlas, 0xFFFF0000.toInt(), 0xFF000000.toInt(), false).also { fonts += it }
        val letter = font.getLetter('A')
        val fill = letter.mGlyph!!
        val stroke = letter.mStrokeGlyph!!
        assertTrue(fill !== stroke)
        assertEquals(fill.width, stroke.width)
        assertEquals(fill.height, stroke.height)
        assertEquals(0xFFFF0000.toInt(), font.fillColor)
        assertEquals(0xFF000000.toInt(), font.strokeColor)

        font.update()
        assertEquals(2, uploads() - baseline)
        assertEquals(listOf("A0", "A1"), rasterizer.rasterized)
    }

    @Test
    fun whitespace_hasNoGlyph() {
        val font = font()
        val space = font.getLetter(' ')
        assertTrue(space.isWhitespace)
        assertNull(space.mGlyph)
        assertNull(space.mStrokeGlyph)
        assertEquals(6f, space.mAdvance, 0f)
        assertNotNull(font.getLetter('A').mGlyph)
        assertNull(font.getLetter('A').mStrokeGlyph)

        font.update()
        assertEquals(1, uploads() - baseline)
    }

    private companion object {
        const val GL_TEXTURE_2D_ARRAY = 0x8C1A
    }
}
