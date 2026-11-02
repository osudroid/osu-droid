package org.andengine.opengl.font;

import org.andengine.graphics.text.GlyphSlot;

import android.util.SparseIntArray;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 10:30:22 - 03.04.2010
 */
public class Letter {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	public final char mCharacter;
	/**
	 * The fill image of this glyph in the shared glyph atlas, or <code>null</code> for whitespace.
	 */
	public final GlyphSlot mGlyph;
	/**
	 * The outline image of this glyph in the shared glyph atlas; only set by {@link StrokeFont}. It has its own
	 * position in the atlas (and its own texture coordinates), only the pen placement and size
	 * ({@link #mOffsetX}, {@link #mOffsetY}, {@link #mWidth}, {@link #mHeight}) are shared with {@link #mGlyph}.
	 */
	public final GlyphSlot mStrokeGlyph;
	private final boolean mWhitespace;
	/** Size, offsets and advance are in virtual units (raster pixels / raster scale for atlas glyphs). */
	public final float mWidth;
	public final float mHeight;
	public final float mOffsetX;
	public final float mOffsetY;
	public final float mAdvance;
	private SparseIntArray mKernings;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * For invisible letters or letters without an extent (i.e. whitespaces).
	 */
	/* package */ Letter(final char pCharacter, final float pAdvance) {
		this(null, null, pCharacter, true, 0, 0, 0, 0, pAdvance);
	}

	/**
	 * For glyphs in the shared glyph atlas.
	 */
	/* package */ Letter(final char pCharacter, final GlyphSlot pGlyph, final GlyphSlot pStrokeGlyph, final float pWidth, final float pHeight, final float pOffsetX, final float pOffsetY, final float pAdvance) {
		this(pGlyph, pStrokeGlyph, pCharacter, false, pWidth, pHeight, pOffsetX, pOffsetY, pAdvance);
	}

	private Letter(final GlyphSlot pGlyph, final GlyphSlot pStrokeGlyph, final char pCharacter, final boolean pWhitespace, final float pWidth, final float pHeight, final float pOffsetX, final float pOffsetY, final float pAdvance) {
		this.mGlyph = pGlyph;
		this.mStrokeGlyph = pStrokeGlyph;
		this.mCharacter = pCharacter;
		this.mWhitespace = pWhitespace;
		this.mWidth = pWidth;
		this.mHeight = pHeight;
		this.mOffsetX = pOffsetX;
		this.mOffsetY = pOffsetY;
		this.mAdvance = pAdvance;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getKerning(final int pCharacter) {
		if(this.mKernings == null) {
			return 0;
		}
		return this.mKernings.get(pCharacter, 0);
	}

	public boolean isWhitespace() {
		return this.mWhitespace;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + this.mCharacter;
		return result;
	}

	@Override
	public boolean equals(final Object pObject) {
		if(this == pObject) {
			return true;
		}
		if(pObject == null) {
			return false;
		}
		if(this.getClass() != pObject.getClass()) {
			return false;
		}
		final Letter other = (Letter) pObject;
		if(this.mCharacter != other.mCharacter) {
			return false;
		}
		return true;
	}



	// ===========================================================
	// Methods
	// ===========================================================

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ "[Character=" + this.mCharacter
				+ ", Whitespace=" + this.mWhitespace
				+ ", Width=" + this.mWidth
				+ ", Height=" + this.mHeight
				+ ", OffsetX=" + this.mOffsetX
				+ ", OffsetY=" + this.mOffsetY
				+ ", Advance=" + this.mAdvance
				+ ", Kernings=" + this.mKernings
				+ "]";
	}

	void addKerning(final int pCharacter, final int pKerning) {
		if(this.mKernings == null) {
			this.mKernings = new SparseIntArray();
		}
		this.mKernings.put(pCharacter, pKerning);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}