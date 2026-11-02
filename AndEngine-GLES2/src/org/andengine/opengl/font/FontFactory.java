package org.andengine.opengl.font;

import android.content.res.AssetManager;
import android.graphics.Typeface;

import org.andengine.graphics.text.AndroidGlyphRasterizer;
import org.andengine.graphics.text.GlyphAtlas;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 17:17:28 - 16.06.2010
 */
// osu!droid modified: fonts rasterize with AndroidGlyphRasterizer into GlyphAtlas.getShared().
// Sizes and stroke widths are virtual units, multiplied by FontManager#getRasterScale() for the rasterizer.
// Every created font is already loaded into the given FontManager, so callers must not load it again.
public class FontFactory {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private static String sAssetBasePath = "";

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * @param pAssetBasePath must end with '<code>/</code>' or have <code>.length() == 0</code>.
	 */
	public static void setAssetBasePath(final String pAssetBasePath) {
		if(pAssetBasePath.endsWith("/") || pAssetBasePath.length() == 0) {
			FontFactory.sAssetBasePath = pAssetBasePath;
		} else {
			throw new IllegalStateException("pAssetBasePath must end with '/' or be lenght zero.");
		}
	}

	public static String getAssetBasePath() {
		return FontFactory.sAssetBasePath;
	}

	public static void onCreate() {
		FontFactory.setAssetBasePath("");
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public static Font create(final FontManager pFontManager, final Typeface pTypeface, final float pSize, final boolean pAntiAlias, final int pColor) {
		final AndroidGlyphRasterizer rasterizer = new AndroidGlyphRasterizer(pTypeface, pSize * FontManager.getRasterScale(), pAntiAlias);
		return FontFactory.load(pFontManager, new Font(pFontManager, rasterizer, GlyphAtlas.getShared(), pColor));
	}

	/**
	 * @param pAssetPath relative to {@link #getAssetBasePath()}.
	 */
	public static Font createFromAsset(final FontManager pFontManager, final AssetManager pAssetManager, final String pAssetPath, final float pSize, final boolean pAntiAlias, final int pColor) {
		return FontFactory.create(pFontManager, FontFactory.typefaceFromAsset(pAssetManager, pAssetPath), pSize, pAntiAlias, pColor);
	}

	/**
	 * @param pStrokeWidth must be positive, because the stroke layer only exists with a stroke width.
	 * @param pStrokeOnly draws only the outline, without the fill.
	 */
	public static StrokeFont createStroke(final FontManager pFontManager, final Typeface pTypeface, final float pSize, final boolean pAntiAlias, final int pColor, final float pStrokeWidth, final int pStrokeColor, final boolean pStrokeOnly) {
		if(!(pStrokeWidth > 0)) {
			throw new IllegalArgumentException("pStrokeWidth must be positive: " + pStrokeWidth);
		}
		final float scale = FontManager.getRasterScale();
		final AndroidGlyphRasterizer rasterizer = new AndroidGlyphRasterizer(pTypeface, pSize * scale, pAntiAlias, pStrokeWidth * scale);
		return FontFactory.load(pFontManager, new StrokeFont(pFontManager, rasterizer, GlyphAtlas.getShared(), pColor, pStrokeColor, pStrokeOnly));
	}

	/**
	 * @param pAssetPath relative to {@link #getAssetBasePath()}.
	 * @param pStrokeWidth must be positive, because the stroke layer only exists with a stroke width.
	 * @param pStrokeOnly draws only the outline, without the fill.
	 */
	public static StrokeFont createStrokeFromAsset(final FontManager pFontManager, final AssetManager pAssetManager, final String pAssetPath, final float pSize, final boolean pAntiAlias, final int pColor, final float pStrokeWidth, final int pStrokeColor, final boolean pStrokeOnly) {
		return FontFactory.createStroke(pFontManager, FontFactory.typefaceFromAsset(pAssetManager, pAssetPath), pSize, pAntiAlias, pColor, pStrokeWidth, pStrokeColor, pStrokeOnly);
	}

	private static Typeface typefaceFromAsset(final AssetManager pAssetManager, final String pAssetPath) {
		return Typeface.createFromAsset(pAssetManager, FontFactory.sAssetBasePath + pAssetPath);
	}

	private static <T extends Font> T load(final FontManager pFontManager, final T pFont) {
		pFontManager.loadFont(pFont);
		return pFont;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
