package org.andengine.graphics.text

import android.graphics.Color
import android.graphics.Typeface
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontFactoryTest {

    private class RecordingFontManager : FontManager() {
        val loaded = ArrayList<Font>()

        override fun loadFont(pFont: Font) {
            loaded += pFont
            super.loadFont(pFont)
        }
    }

    private val manager = RecordingFontManager()
    private val typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    @Before
    fun setUp() {
        installRecordingGL()
        FontManager.setRasterScale(2f)
    }

    @After
    fun tearDown() {
        FontManager.setRasterScale(1f)
        FontFactory.onCreate()
    }

    @Test
    fun create_loadsTheFontOnTheSharedAtlas() {
        val font = FontFactory.create(manager, typeface, 20f, true, Color.RED)

        assertEquals(listOf(font), manager.loaded)
        assertSame(GlyphAtlas.shared, font.atlas)
        assertEquals(Color.RED, font.color)
        assertEquals(2f, font.rasterScale, 0f)
    }

    @Test
    fun create_rasterizesAtTheRasterScale() {
        val font = FontFactory.create(manager, typeface, 20f, true, Color.WHITE)
        val reference = Font(FontManager(), AndroidGlyphRasterizer(typeface, 40f, true), GlyphAtlas.shared, Color.WHITE)

        assertTrue(font.ascent < 0f)
        assertEquals(reference.ascent, font.ascent, 0f)
        assertEquals(reference.descent, font.descent, 0f)
        assertEquals(reference.getLetter('A').mAdvance, font.getLetter('A').mAdvance, 0f)
    }

    @Test
    fun create_plainFontHasNoStrokeLayer() {
        val font = FontFactory.create(manager, typeface, 20f, true, Color.WHITE)

        assertNull(font.getLetter('A').mStrokeGlyph)
    }

    @Test
    fun createStroke_loadsAStrokeFontWithAnOutlineLayer() {
        val font = FontFactory.createStroke(manager, typeface, 20f, true, Color.WHITE, 2f, Color.BLACK, true)

        assertEquals(listOf<Font>(font), manager.loaded)
        assertSame(GlyphAtlas.shared, font.atlas)
        assertEquals(Color.WHITE, font.color)
        assertEquals(Color.BLACK, font.strokeColor)
        assertTrue(font.isStrokeOnly)
        assertNotNull(font.getLetter('A').mStrokeGlyph)
    }

    @Test
    fun createStroke_scalesTheStrokeWidth() {
        val font = FontFactory.createStroke(manager, typeface, 20f, true, Color.WHITE, 2f, Color.BLACK, false)
        val reference = StrokeFont(
            FontManager(), AndroidGlyphRasterizer(typeface, 40f, true, 4f), GlyphAtlas.shared, Color.WHITE, Color.BLACK, false
        )

        assertFalse(font.isStrokeOnly)
        assertEquals(reference.getLetter('A').mWidth, font.getLetter('A').mWidth, 0f)
        assertEquals(reference.getLetter('A').mHeight, font.getLetter('A').mHeight, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createStroke_withoutStrokeWidth_throws() {
        FontFactory.createStroke(manager, typeface, 20f, true, Color.WHITE, 0f, Color.BLACK, false)
    }

    @Test
    fun createFromAsset_prefixesTheAssetBasePath() {
        FontFactory.setAssetBasePath("base/")

        val e = assertThrows(RuntimeException::class.java) {
            FontFactory.createFromAsset(manager, RuntimeEnvironment.getApplication().assets, "missing.ttf", 20f, true, Color.WHITE)
        }
        assertTrue(e.message, e.message!!.contains("base/missing.ttf"))
        assertTrue(manager.loaded.isEmpty())
    }
}
