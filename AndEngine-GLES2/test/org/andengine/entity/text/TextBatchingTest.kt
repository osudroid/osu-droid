package org.andengine.entity.text

import android.opengl.GLES20
import java.nio.ByteBuffer
import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.FakeRasterizer
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.arrayDraws
import org.andengine.graphics.gles.draws
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.gles.lastVertices
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.font.StrokeFont
import org.andengine.util.HorizontalAlign
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextBatchingTest {

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private val fontManager = FontManager()
    private val fonts = ArrayList<Font>()
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 800f, 480f)

    @Before
    fun setUp() {
        gl = installRecordingGL()
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

    private fun text(font: Font, string: String, capacity: Int = string.length, options: TextOptions = TextOptions()): Text {
        val text = Text(0f, 0f, font, string, capacity, options)
        font.update()
        return text
    }

    private fun quads() = gl.vertexUploads.last().size / (4 * QuadBatcher.VERTEX_SIZE_BYTES)

    /** Byte offset of vertex 0 of quad [quad]. */
    private fun at(quad: Int) = quad * 4 * QuadBatcher.VERTEX_SIZE_BYTES

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
        assertEquals(0, gl.arrayDraws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertEquals(3, quads())
    }

    @Test
    fun text_colorAndAlphaPacked() {
        val t = text(font(), "a")
        t.setColor(1f, 0f, 0.5f, 0.5f)
        t.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(listOf(255, 0, 128, 128), color(gl.lastVertices(), 0))
    }

    @Test
    fun strokeText_drawsFillThenStroke() {
        val font = StrokeFont(fontManager, FakeRasterizer(layers = 2), atlas, 0xFFFF0000.toInt(), 0xFF0000FF.toInt(), false).also { fonts += it }
        text(font, "a").onDraw(glState, camera)
        QuadBatcher.flush()

        val v = gl.lastVertices()
        assertEquals(2, quads())
        val letter = font.getLetter('a')
        // The fill comes first; the outline goes on top, with its own atlas position and the stroke color.
        assertEquals(letter.mGlyph!!.u, v.getFloat(at(0) + 8), 0f)
        assertEquals(letter.mGlyph!!.v, v.getFloat(at(0) + 12), 0f)
        assertEquals(listOf(255, 0, 0, 255), color(v, 0))
        assertEquals(letter.mStrokeGlyph!!.u, v.getFloat(at(1) + 8), 0f)
        assertEquals(letter.mStrokeGlyph!!.v, v.getFloat(at(1) + 12), 0f)
        assertEquals(listOf(0, 0, 255, 255), color(v, 1))
        assertTrue(letter.mGlyph!!.u != letter.mStrokeGlyph!!.u || letter.mGlyph!!.v != letter.mStrokeGlyph!!.v)
    }

    @Test
    fun strokeOnlyText_skipsFill() {
        val font = StrokeFont(fontManager, FakeRasterizer(layers = 2), atlas, 0xFFFF0000.toInt(), 0xFF0000FF.toInt(), true).also { fonts += it }
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

    /** The letters and their rectangles as "c(x0,y0,x1,y1)", plus the size of the text. */
    private fun layout(t: Text): String {
        val sb = StringBuilder()
        sb.append("w=").append(t.width).append(" h=").append(t.height).append(" n=").append(t.charactersToDraw).append(' ')
        val q = t.letterQuads
        for (i in 0 until t.letterCount) {
            val o = i * Text.LETTER_QUAD_SIZE
            sb.append(t.letters[i]!!.mCharacter)
                .append('(').append(q[o]).append(',').append(q[o + 1]).append(',')
                .append(q[o + 2]).append(',').append(q[o + 3]).append(')')
        }
        return sb.toString()
    }

    @Test
    fun text_letterQuadsForAlignWrapLeadingAndSetText() {
        val font = font()

        assertEquals(
            "w=44.0 h=100.0 n=3 a(2.0,10.0,22.0,40.0)b(24.0,10.0,44.0,40.0)c(2.0,60.0,22.0,90.0)",
            layout(text(font, "ab\nc", 4, TextOptions(HorizontalAlign.LEFT)))
        )
        assertEquals(
            "w=44.0 h=100.0 n=3 a(2.0,10.0,22.0,40.0)b(24.0,10.0,44.0,40.0)c(14.0,60.0,34.0,90.0)",
            layout(text(font, "ab\nc", 4, TextOptions(HorizontalAlign.CENTER)))
        )
        assertEquals(
            "w=44.0 h=100.0 n=3 a(2.0,10.0,22.0,40.0)b(24.0,10.0,44.0,40.0)c(26.0,60.0,46.0,90.0)",
            layout(text(font, "ab\nc", 4, TextOptions(HorizontalAlign.RIGHT)))
        )
        assertEquals(
            "w=50.0 h=150.0 n=5 a(2.0,10.0,22.0,40.0)b(24.0,10.0,44.0,40.0)c(2.0,60.0,22.0,90.0)" +
                "a(2.0,110.0,22.0,140.0)b(24.0,110.0,44.0,140.0)",
            layout(text(font, "ab c ab", 7, TextOptions(AutoWrap.WORDS, 50f)))
        )
        assertEquals(
            "w=60.0 h=150.0 n=5 a(10.0,10.0,30.0,40.0)b(32.0,10.0,52.0,40.0)c(22.0,60.0,42.0,90.0)" +
                "a(10.0,110.0,30.0,140.0)b(32.0,110.0,52.0,140.0)",
            layout(text(font, "ab c ab", 7, TextOptions(AutoWrap.WORDS, 60f, HorizontalAlign.CENTER)))
        )
        assertEquals(
            "w=20.0 h=105.0 n=2 a(2.0,10.0,22.0,40.0)b(2.0,65.0,22.0,95.0)",
            layout(text(font, "a\nb", 3, TextOptions(AutoWrap.NONE, 0f, HorizontalAlign.LEFT, 5f)))
        )

        val t = text(font, "ab", 8)
        assertEquals("w=44.0 h=50.0 n=2 a(2.0,10.0,22.0,40.0)b(24.0,10.0,44.0,40.0)", layout(t))
        t.setText("abc a")
        assertEquals(
            "w=100.0 h=50.0 n=4 a(2.0,10.0,22.0,40.0)b(24.0,10.0,44.0,40.0)c(46.0,10.0,66.0,40.0)a(80.0,10.0,100.0,40.0)",
            layout(t)
        )
        t.setText("b")
        assertEquals("w=20.0 h=50.0 n=1 b(2.0,10.0,22.0,40.0)", layout(t))
        // Letters of the longer text are dropped.
        for (i in 1 until t.letters.size) assertNull(t.letters[i])
        t.setText("")
        assertEquals("w=0.0 h=50.0 n=0 ", layout(t))

        assertEquals("w=0.0 h=50.0 n=0 ", layout(text(font, "", 4)))
    }

    @Test
    fun text_shorterTextDrawsOnlyItsLetters() {
        val t = text(font(), "abc", 8)
        t.setText("a")
        t.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(1, quads())
    }

    @Test
    fun emptyText_drawsNothing() {
        text(font(), "", 4).onDraw(glState, camera)
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun text_colorReadAtDraw() {
        val t = text(font(), "a")
        t.setColor(1f, 0f, 0f, 1f)
        t.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(listOf(255, 0, 0, 255), color(gl.lastVertices(), 0))

        t.setColor(0f, 0f, 1f)
        t.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(listOf(0, 0, 255, 255), color(gl.lastVertices(), 0))
    }

    @Test
    fun text_unknownBlendFallsBackToAlpha() {
        val t = text(font(), "a")
        t.setBlendFunction(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE)
        t.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertEquals(1, gl.draws())
        assertEquals(0, gl.arrayDraws())
        val expected = "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }
}
