/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 17:17:28 - 16.06.2010
 */
package org.andengine.graphics.text

import android.content.res.AssetManager
import android.graphics.Typeface

// osu!droid modified: fonts rasterize with AndroidGlyphRasterizer into GlyphAtlas.getShared().
// Sizes and stroke widths are virtual units, multiplied by FontManager#getRasterScale() for the rasterizer.
// Every created font is already loaded into the given FontManager, so callers must not load it again.
object FontFactory {

    private var sAssetBasePath = ""

    /**
     * @param pAssetBasePath must end with '`/`' or have `.length() == 0`.
     */
    @JvmStatic
    fun setAssetBasePath(pAssetBasePath: String) {
        if (pAssetBasePath.endsWith("/") || pAssetBasePath.length == 0) {
            sAssetBasePath = pAssetBasePath
        } else {
            throw IllegalStateException("pAssetBasePath must end with '/' or be lenght zero.")
        }
    }

    @JvmStatic
    fun getAssetBasePath(): String {
        return sAssetBasePath
    }

    @JvmStatic
    fun onCreate() {
        setAssetBasePath("")
    }

    @JvmStatic
    fun create(pFontManager: FontManager, pTypeface: Typeface, pSize: Float, pAntiAlias: Boolean, pColor: Int): Font {
        val rasterizer = AndroidGlyphRasterizer(pTypeface, pSize * FontManager.getRasterScale(), pAntiAlias)
        return load(pFontManager, Font(pFontManager, rasterizer, GlyphAtlas.shared, pColor))
    }

    /**
     * @param pAssetPath relative to [getAssetBasePath].
     */
    @JvmStatic
    fun createFromAsset(pFontManager: FontManager, pAssetManager: AssetManager, pAssetPath: String, pSize: Float, pAntiAlias: Boolean, pColor: Int): Font {
        return create(pFontManager, typefaceFromAsset(pAssetManager, pAssetPath), pSize, pAntiAlias, pColor)
    }

    /**
     * @param pStrokeWidth must be positive, because the stroke layer only exists with a stroke width.
     * @param pStrokeOnly draws only the outline, without the fill.
     */
    @JvmStatic
    fun createStroke(pFontManager: FontManager, pTypeface: Typeface, pSize: Float, pAntiAlias: Boolean, pColor: Int, pStrokeWidth: Float, pStrokeColor: Int, pStrokeOnly: Boolean): StrokeFont {
        if (!(pStrokeWidth > 0)) {
            throw IllegalArgumentException("pStrokeWidth must be positive: $pStrokeWidth")
        }
        val scale = FontManager.getRasterScale()
        val rasterizer = AndroidGlyphRasterizer(pTypeface, pSize * scale, pAntiAlias, pStrokeWidth * scale)
        return load(pFontManager, StrokeFont(pFontManager, rasterizer, GlyphAtlas.shared, pColor, pStrokeColor, pStrokeOnly))
    }

    /**
     * @param pAssetPath relative to [getAssetBasePath].
     * @param pStrokeWidth must be positive, because the stroke layer only exists with a stroke width.
     * @param pStrokeOnly draws only the outline, without the fill.
     */
    @JvmStatic
    fun createStrokeFromAsset(pFontManager: FontManager, pAssetManager: AssetManager, pAssetPath: String, pSize: Float, pAntiAlias: Boolean, pColor: Int, pStrokeWidth: Float, pStrokeColor: Int, pStrokeOnly: Boolean): StrokeFont {
        return createStroke(pFontManager, typefaceFromAsset(pAssetManager, pAssetPath), pSize, pAntiAlias, pColor, pStrokeWidth, pStrokeColor, pStrokeOnly)
    }

    private fun typefaceFromAsset(pAssetManager: AssetManager, pAssetPath: String): Typeface {
        return Typeface.createFromAsset(pAssetManager, sAssetBasePath + pAssetPath)
    }

    private fun <T : Font> load(pFontManager: FontManager, pFont: T): T {
        pFontManager.loadFont(pFont)
        return pFont
    }
}
