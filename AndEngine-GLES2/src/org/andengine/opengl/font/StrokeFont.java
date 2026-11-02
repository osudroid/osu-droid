package org.andengine.opengl.font;

import org.andengine.graphics.text.GlyphAtlas;
import org.andengine.graphics.text.GlyphRasterizer;


/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:39:33 - 03.04.2010
 */
// osu!droid modified: fill and stroke are separate alpha-only atlas images (Letter#mGlyph, Letter#mStrokeGlyph),
// colored when drawing with getFillColor() and getStrokeColor().
public class StrokeFont extends Font {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final int mStrokeColorARGBPackedInt;
	private final boolean mStrokeOnly;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param pRasterizer must provide a stroke layer (layer 1), in physical pixels.
	 */
	public StrokeFont(final FontManager pFontManager, final GlyphRasterizer pRasterizer, final int pFillColorARGBPackedInt, final int pStrokeColorARGBPackedInt, final boolean pStrokeOnly) {
		this(pFontManager, pRasterizer, GlyphAtlas.getShared(), pFillColorARGBPackedInt, pStrokeColorARGBPackedInt, pStrokeOnly);
	}

	public StrokeFont(final FontManager pFontManager, final GlyphRasterizer pRasterizer, final GlyphAtlas pAtlas, final int pFillColorARGBPackedInt, final int pStrokeColorARGBPackedInt, final boolean pStrokeOnly) {
		super(pFontManager, pRasterizer, pAtlas, pFillColorARGBPackedInt);
		this.mStrokeColorARGBPackedInt = pStrokeColorARGBPackedInt;
		this.mStrokeOnly = pStrokeOnly;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getFillColor() {
		return this.getColor();
	}

	public int getStrokeColor() {
		return this.mStrokeColorARGBPackedInt;
	}

	/**
	 * @return true if only the stroke is drawn; the fill image exists anyway.
	 */
	public boolean isStrokeOnly() {
		return this.mStrokeOnly;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected int getGlyphLayerCount() {
		return 2;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
