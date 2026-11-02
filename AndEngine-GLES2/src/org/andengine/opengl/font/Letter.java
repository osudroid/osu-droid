package org.andengine.opengl.font;

import org.andengine.graphics.text.GlyphSlot;
import org.andengine.opengl.texture.ITexture;

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
	 * The page texture of a {@link BitmapFont} glyph; <code>null</code> for whitespace and for glyphs in the shared
	 * glyph atlas (see {@link #mGlyph}).
	 */
	public final ITexture mTexture;
	/**
	 * The fill image of this glyph in the shared glyph atlas, or <code>null</code> for whitespace and
	 * {@link BitmapFont} glyphs.
	 */
	public final GlyphSlot mGlyph;
	/**
	 * The outline image of this glyph in the shared glyph atlas; only set by {@link StrokeFont}. It has its own
	 * position in the atlas (and its own texture coordinates), only the pen placement and size
	 * ({@link #mOffsetX}, {@link #mOffsetY}, {@link #mWidth}, {@link #mHeight}) are shared with {@link #mGlyph}.
	 */
	public final GlyphSlot mStrokeGlyph;
	private final boolean mWhitespace;
	public final int mTextureX;
	public final int mTextureY;
	/** Size, offsets and advance are in virtual units (raster pixels / raster scale for atlas glyphs). */
	public final float mWidth;
	public final float mHeight;
	public final float mOffsetX;
	public final float mOffsetY;
	public final float mAdvance;
	public final float mU;
	public final float mV;
	public final float mU2;
	public final float mV2;
	private SparseIntArray mKernings;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * For invisible letters or letters without an extent (i.e. whitespaces).
	 */
	/* package */ Letter(final char pCharacter, final float pAdvance) {
		this(null, null, null, pCharacter, true, 0, 0, 0, 0, 0, 0, pAdvance, 0, 0, 0, 0);
	}

	/* package */ Letter(final ITexture pTexture, final char pCharacter, final int pTextureX, final int pTextureY, final float pWidth, final float pHeight, final float pOffsetX, final float pOffsetY, final float pAdvance, final float pU, final float pV, final float pU2, final float pV2) {
		this(pTexture, null, null, pCharacter, false, pTextureX, pTextureY, pWidth, pHeight, pOffsetX, pOffsetY, pAdvance, pU, pV, pU2, pV2);
	}

	/**
	 * For glyphs in the shared glyph atlas. The texture coordinates mirror those of <code>pGlyph</code>.
	 */
	/* package */ Letter(final char pCharacter, final GlyphSlot pGlyph, final GlyphSlot pStrokeGlyph, final float pWidth, final float pHeight, final float pOffsetX, final float pOffsetY, final float pAdvance) {
		this(null, pGlyph, pStrokeGlyph, pCharacter, false, 0, 0, pWidth, pHeight, pOffsetX, pOffsetY, pAdvance, pGlyph.u, pGlyph.v, pGlyph.u2, pGlyph.v2);
	}

	private Letter(final ITexture pTexture, final GlyphSlot pGlyph, final GlyphSlot pStrokeGlyph, final char pCharacter, final boolean pWhitespace, final int pTextureX, final int pTextureY, final float pWidth, final float pHeight, final float pOffsetX, final float pOffsetY, final float pAdvance, final float pU, final float pV, final float pU2, final float pV2) {
		this.mTexture = pTexture;
		this.mGlyph = pGlyph;
		this.mStrokeGlyph = pStrokeGlyph;
		this.mCharacter = pCharacter;
		this.mWhitespace = pWhitespace;
		this.mWidth = pWidth;
		this.mHeight = pHeight;
		this.mTextureX = pTextureX;
		this.mTextureY = pTextureY;
		this.mOffsetX = pOffsetX;
		this.mOffsetY = pOffsetY;
		this.mAdvance = pAdvance;
		this.mU = pU;
		this.mV = pV;
		this.mU2 = pU2;
		this.mV2 = pV2;
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
				+ ", TextureX=" + this.mTextureX
				+ ", TextureY=" + this.mTextureY
				+ ", Width=" + this.mWidth
				+ ", Height=" + this.mHeight
				+ ", OffsetX=" + this.mOffsetX
				+ ", OffsetY=" + this.mOffsetY
				+ ", Advance=" + this.mAdvance
				+ ", U=" + this.mU
				+ ", V=" + this.mV
				+ ", U2=" + this.mU2
				+ ", V2=" + this.mV2
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