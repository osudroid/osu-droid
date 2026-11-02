/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:39:33 - 03.04.2010
 */
package org.andengine.graphics.text

import android.util.SparseArray
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.text.exception.FontException
import org.andengine.util.color.Color
import org.andengine.util.debug.Debug

// osu!droid modified:
// * Glyphs are rasterized in physical pixels (virtual size * raster scale, see FontManager#getRasterScale())
//   into the shared glyph atlas. Letter metrics stay in virtual units.
/**
 * @param pRasterizer produces glyphs in physical pixels, i.e. created with `size * FontManager.getRasterScale()`.
 */
open class Font @JvmOverloads constructor(
    private val mFontManager: FontManager,
    private val mRasterizer: GlyphRasterizer,
    private val mAtlas: GlyphAtlas = GlyphAtlas.shared,
    private val mColorARGBPackedInt: Int = Color.WHITE_ARGB_PACKED_INT
) : IFont {

    private val mRasterScale: Float = FontManager.getRasterScale()

    private val mManagedCharacterToLetterMap = SparseArray<Letter>()
    private val mLettersPendingToBeDrawnToTexture = ArrayList<Letter>()

    private val mMetrics = GlyphMetrics()
    private var mPixels: ByteBuffer? = null

    /** The gap between the lines; glyphs are laid out without one. */
    open val leading: Float
        get() = 0f

    /** The distance from the baseline to the top in virtual units, which is usually negative. */
    open val ascent: Float
        get() = mRasterizer.ascent / mRasterScale

    /** The distance from the baseline to the bottom in virtual units, which is usually positive. */
    open val descent: Float
        get() = mRasterizer.descent / mRasterScale

    /** Raster pixels per virtual unit, fixed when the font was created. */
    open val rasterScale: Float
        get() = mRasterScale

    /** The color passed when the font was created, applied when drawing (glyphs are alpha-only). */
    open val color: Int
        get() = mColorARGBPackedInt

    open val atlas: GlyphAtlas
        get() = mAtlas

    /** The number of atlas images per glyph: 1 (fill), or 2 (fill and stroke) for a [StrokeFont]. */
    protected open val glyphLayerCount: Int
        get() = 1

    @Synchronized
    override fun load() {
        mFontManager.loadFont(this)
    }

    @Synchronized
    override fun unload() {
        mFontManager.unloadFont(this)
    }

    override val lineHeight: Float
        get() = -ascent + descent

    @Synchronized
    @Throws(FontException::class)
    override fun getLetter(pChar: Char): Letter {
        var letter: Letter? = mManagedCharacterToLetterMap.get(pChar.code)
        if (letter == null) {
            letter = createLetter(pChar)
            if (!letter.isWhitespace) {
                mLettersPendingToBeDrawnToTexture.add(letter)
            }
            mManagedCharacterToLetterMap.put(pChar.code, letter)
        }
        return letter
    }

    /**
     * Queues every glyph for upload again into the slot it already has, e.g. after the atlas textures came back
     * empty from a context loss.
     */
    @Synchronized
    open fun invalidateLetters() {
        val lettersPendingToBeDrawnToTexture = mLettersPendingToBeDrawnToTexture
        val managedCharacterToLetterMap = mManagedCharacterToLetterMap

        lettersPendingToBeDrawnToTexture.clear()
        for (i in managedCharacterToLetterMap.size() - 1 downTo 0) {
            val letter = managedCharacterToLetterMap.valueAt(i)
            if (!letter.isWhitespace) {
                letter.mGlyph?.uploaded = false
                letter.mStrokeGlyph?.uploaded = false
                lettersPendingToBeDrawnToTexture.add(letter)
            }
        }
    }

    @Throws(FontException::class)
    open fun prepareLetters(vararg pCharacters: Char) {
        for (i in pCharacters.indices) {
            getLetter(pCharacters[i])
        }
    }

    private fun createLetter(pCharacter: Char): Letter {
        val metrics = mMetrics
        mRasterizer.measure(pCharacter, metrics)

        val scale = mRasterScale
        val advance = metrics.advance / scale

        if (Character.isWhitespace(pCharacter) || metrics.width <= 0 || metrics.height <= 0) {
            return Letter(pCharacter, advance)
        }

        val glyph = mAtlas.allocate(metrics.width, metrics.height)
        val strokeGlyph = if (glyph != null && glyphLayerCount > 1) mAtlas.allocate(metrics.width, metrics.height) else null
        if (glyph == null || (glyphLayerCount > 1 && strokeGlyph == null)) {
            // Larger than an atlas layer: keep the layout, draw nothing.
            Debug.w("Font: Glyph '" + pCharacter + "' (" + metrics.width + "x" + metrics.height + ") does not fit into the glyph atlas")
            return Letter(pCharacter, advance)
        }

        val offsetY = (metrics.offsetY - mRasterizer.ascent) / scale
        return Letter(pCharacter, glyph, strokeGlyph, metrics.width / scale, metrics.height / scale, metrics.offsetX / scale, offsetY, advance)
    }

    /** Rasterizes and uploads the pending glyphs. GL thread only. */
    @Synchronized
    open fun update() {
        val lettersPendingToBeDrawnToTexture = mLettersPendingToBeDrawnToTexture
        val count = lettersPendingToBeDrawnToTexture.size
        if (count == 0) {
            return
        }

        for (i in 0 until count) {
            val letter = lettersPendingToBeDrawnToTexture[i]
            upload(letter.mCharacter, 0, letter.mGlyph!!)
            val strokeGlyph = letter.mStrokeGlyph
            if (strokeGlyph != null) {
                upload(letter.mCharacter, 1, strokeGlyph)
            }
        }
        lettersPendingToBeDrawnToTexture.clear()
    }

    private fun upload(pCharacter: Char, pLayer: Int, pSlot: GlyphSlot) {
        val size = pSlot.width * pSlot.height
        var pixels = mPixels
        if (pixels == null || pixels.capacity() < size) {
            pixels = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            mPixels = pixels
        }
        pixels.clear()
        mRasterizer.rasterize(pCharacter, pLayer, pixels)
        pixels.position(0)
        pixels.limit(size)
        mAtlas.upload(pSlot, pixels)
    }
}
