package org.andengine.entity.text

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.entity.text.vbo.HighPerformanceTextVertexBufferObject
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphMetrics
import org.andengine.graphics.text.GlyphRasterizer
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.font.StrokeFont
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
class TextBatchingTest {

    /** Every visible glyph is 20x30 raster pixels; ' ' has no pixels. */
    private class FakeRasterizer : GlyphRasterizer {
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
            for (i in 0 until 20 * 30) out.put(1)
        }
    }

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private val fontManager = FontManager()
    private val fonts = ArrayList<Font>()
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 800f, 480f)

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val arrays = HashSet<GlyphArray>()
        for (font in fonts) {
            for (c in "abc ") {
                val letter = font.getLetter(c)
                letter.mGlyph?.let { arrays += it.array }
                letter.mStrokeGlyph?.let { arrays += it.array }
            }
        }
        for (a in arrays) if (a.hasTexture) a.texture.dispose()
    }

    private fun font() = Font(fontManager, FakeRasterizer(), atlas).also { fonts += it }

    private fun vbo(capacity: Int) = HighPerformanceTextVertexBufferObject(
        null, Text.LETTER_SIZE * capacity, DrawType.STATIC, false, Text.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
    )

    private fun text(font: Font, string: String): Text {
        val text = Text(0f, 0f, font, string, string.length, TextOptions(), vbo(string.length))
        font.update(null)
        return text
    }

    private fun legacyDraws() = gl.calls.count { it.startsWith("glDrawArrays") }
    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }

    private fun vertices(): ByteBuffer =
        ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    private fun quads() = gl.vertexUploads.last().size / (4 * QuadBatcher.GLYPH_VERTEX_SIZE_BYTES)

    /** Byte offset of vertex 0 of quad [quad]. */
    private fun at(quad: Int) = quad * 4 * QuadBatcher.GLYPH_VERTEX_SIZE_BYTES

    private fun color(v: ByteBuffer, quad: Int) = listOf(
        v.get(at(quad) + 16).toInt() and 0xFF,
        v.get(at(quad) + 17).toInt() and 0xFF,
        v.get(at(quad) + 18).toInt() and 0xFF,
        v.get(at(quad) + 19).toInt() and 0xFF
    )

    @Test
    fun text_drawsOneGlyphPerVisibleLetter() {
        val t = text(font(), "ab c")
        t.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, draws())
        assertEquals(3, quads())
    }

    @Test
    fun text_colorAndAlphaPacked() {
        val t = text(font(), "a")
        t.setColor(1f, 0f, 0.5f, 0.5f)
        t.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(listOf(255, 0, 128, 128), color(vertices(), 0))
    }

    @Test
    fun tickerText_drawsOnlyRevealedCharacters() {
        val tickerFont = font()
        val ticker = TickerText(0f, 0f, tickerFont, "abc", TickerText.TickerTextOptions(10f), null)
        tickerFont.update(null)
        ticker.onDraw(glState, camera)
        assertFalse(QuadBatcher.hasPending)

        ticker.onUpdate(0.15f)
        assertEquals(1, ticker.charactersVisible)
        ticker.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(1, quads())
    }

    @Test
    fun strokeText_drawsStrokeThenFill() {
        val font = StrokeFont(fontManager, FakeRasterizer(), atlas, 0xFFFF0000.toInt(), 0xFF0000FF.toInt(), false).also { fonts += it }
        text(font, "a").onDraw(glState, camera)
        QuadBatcher.flush()

        val v = vertices()
        assertEquals(2, quads())
        val letter = font.getLetter('a')
        // The outline comes first, with its own atlas position and the stroke color.
        assertEquals(letter.mStrokeGlyph!!.u, v.getFloat(at(0) + 8), 0f)
        assertEquals(letter.mStrokeGlyph!!.v, v.getFloat(at(0) + 12), 0f)
        assertEquals(listOf(0, 0, 255, 255), color(v, 0))
        assertEquals(letter.mGlyph!!.u, v.getFloat(at(1) + 8), 0f)
        assertEquals(letter.mGlyph!!.v, v.getFloat(at(1) + 12), 0f)
        assertEquals(listOf(255, 0, 0, 255), color(v, 1))
        assertTrue(letter.mGlyph!!.u != letter.mStrokeGlyph!!.u || letter.mGlyph!!.v != letter.mStrokeGlyph!!.v)
    }

    @Test
    fun strokeOnlyText_skipsFill() {
        val font = StrokeFont(fontManager, FakeRasterizer(), atlas, 0xFFFF0000.toInt(), 0xFF0000FF.toInt(), true).also { fonts += it }
        text(font, "a").onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(1, quads())
    }

    @Test
    fun text_defaultBlendIsStraightAlpha() {
        text(font(), "a").onDraw(glState, camera)
        QuadBatcher.flush()

        val expected = "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }
}
