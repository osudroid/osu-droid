/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:39:33 - 03.04.2010
 */
package org.andengine.graphics.text

// osu!droid modified: fill and stroke are separate alpha-only atlas images (Letter#mGlyph, Letter#mStrokeGlyph),
// colored when drawing with getColor() (fill) and getStrokeColor().
open class StrokeFont(
    pFontManager: FontManager,
    pRasterizer: GlyphRasterizer,
    pAtlas: GlyphAtlas,
    pFillColorARGBPackedInt: Int,
    private val mStrokeColorARGBPackedInt: Int,
    private val mStrokeOnly: Boolean
) : Font(pFontManager, pRasterizer, pAtlas, pFillColorARGBPackedInt) {

    /**
     * @param pRasterizer must provide a stroke layer (layer 1), in physical pixels.
     */
    constructor(
        pFontManager: FontManager,
        pRasterizer: GlyphRasterizer,
        pFillColorARGBPackedInt: Int,
        pStrokeColorARGBPackedInt: Int,
        pStrokeOnly: Boolean
    ) : this(pFontManager, pRasterizer, GlyphAtlas.shared, pFillColorARGBPackedInt, pStrokeColorARGBPackedInt, pStrokeOnly)

    open val strokeColor: Int
        get() = mStrokeColorARGBPackedInt

    /** True if only the stroke is drawn; the fill image exists anyway. */
    open val isStrokeOnly: Boolean
        get() = mStrokeOnly

    override val glyphLayerCount: Int
        get() = 2
}
