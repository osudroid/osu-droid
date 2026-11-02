/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:30:22 - 03.04.2010
 */
package org.andengine.graphics.text

import android.util.SparseIntArray

open class Letter private constructor(
    /** The fill image of this glyph in the shared glyph atlas, or `null` for whitespace. */
    @JvmField val mGlyph: GlyphSlot?,
    /**
     * The outline image of this glyph in the shared glyph atlas; only set by [StrokeFont]. It has its own
     * position in the atlas (and its own texture coordinates), only the pen placement and size
     * ([mOffsetX], [mOffsetY], [mWidth], [mHeight]) are shared with [mGlyph].
     */
    @JvmField val mStrokeGlyph: GlyphSlot?,
    @JvmField val mCharacter: Char,
    private val mWhitespace: Boolean,
    /** Size, offsets and advance are in virtual units (raster pixels / raster scale for atlas glyphs). */
    @JvmField val mWidth: Float,
    @JvmField val mHeight: Float,
    @JvmField val mOffsetX: Float,
    @JvmField val mOffsetY: Float,
    @JvmField val mAdvance: Float
) {

    private var mKernings: SparseIntArray? = null

    /** For invisible letters or letters without an extent (i.e. whitespaces). */
    constructor(pCharacter: Char, pAdvance: Float) :
        this(null, null, pCharacter, true, 0f, 0f, 0f, 0f, pAdvance)

    /** For glyphs in the shared glyph atlas. */
    constructor(
        pCharacter: Char,
        pGlyph: GlyphSlot?,
        pStrokeGlyph: GlyphSlot?,
        pWidth: Float,
        pHeight: Float,
        pOffsetX: Float,
        pOffsetY: Float,
        pAdvance: Float
    ) : this(pGlyph, pStrokeGlyph, pCharacter, false, pWidth, pHeight, pOffsetX, pOffsetY, pAdvance)

    open val isWhitespace: Boolean
        get() = mWhitespace

    open fun getKerning(pCharacter: Int): Int {
        val kernings = mKernings ?: return 0
        return kernings.get(pCharacter, 0)
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + mCharacter.code
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        return mCharacter == (other as Letter).mCharacter
    }

    override fun toString(): String {
        return javaClass.simpleName +
            "[Character=" + mCharacter +
            ", Whitespace=" + mWhitespace +
            ", Width=" + mWidth +
            ", Height=" + mHeight +
            ", OffsetX=" + mOffsetX +
            ", OffsetY=" + mOffsetY +
            ", Advance=" + mAdvance +
            ", Kernings=" + mKernings +
            "]"
    }

    fun addKerning(pCharacter: Int, pKerning: Int) {
        var kernings = mKernings
        if (kernings == null) {
            kernings = SparseIntArray()
            mKernings = kernings
        }
        kernings.put(pCharacter, pKerning)
    }
}
