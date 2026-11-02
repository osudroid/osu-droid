package org.andengine.opengl.font;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.andengine.graphics.text.GlyphAtlas;
import org.andengine.graphics.text.GlyphMetrics;
import org.andengine.graphics.text.GlyphRasterizer;
import org.andengine.graphics.text.GlyphSlot;
import org.andengine.opengl.font.exception.FontException;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.util.GLState;

import android.util.SparseArray;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 10:39:33 - 03.04.2010
 */
// osu!droid modified:
// * Glyphs are rasterized in physical pixels (virtual size * raster scale, see FontManager#getRasterScale())
//   into the shared glyph atlas. Letter metrics stay in virtual units.
public class Font implements IFont {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int WHITE_ARGB_PACKED_INT = 0xFFFFFFFF;

	// ===========================================================
	// Fields
	// ===========================================================

	private final FontManager mFontManager;
	private final GlyphRasterizer mRasterizer;
	private final GlyphAtlas mAtlas;
	private final float mRasterScale;
	private final int mColorARGBPackedInt;

	private final SparseArray<Letter> mManagedCharacterToLetterMap = new SparseArray<Letter>();
	private final ArrayList<Letter> mLettersPendingToBeDrawnToTexture = new ArrayList<Letter>();

	private final GlyphMetrics mMetrics = new GlyphMetrics();
	private ByteBuffer mPixels;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param pRasterizer produces glyphs in physical pixels, i.e. created with <code>size * FontManager.getRasterScale()</code>.
	 */
	public Font(final FontManager pFontManager, final GlyphRasterizer pRasterizer) {
		this(pFontManager, pRasterizer, GlyphAtlas.getShared());
	}

	public Font(final FontManager pFontManager, final GlyphRasterizer pRasterizer, final GlyphAtlas pAtlas) {
		this(pFontManager, pRasterizer, pAtlas, Font.WHITE_ARGB_PACKED_INT);
	}

	public Font(final FontManager pFontManager, final GlyphRasterizer pRasterizer, final GlyphAtlas pAtlas, final int pColorARGBPackedInt) {
		this.mFontManager = pFontManager;
		this.mRasterizer = pRasterizer;
		this.mAtlas = pAtlas;
		this.mRasterScale = FontManager.getRasterScale();
		this.mColorARGBPackedInt = pColorARGBPackedInt;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * @return the gap between the lines; glyphs are laid out without one.
	 */
	public float getLeading() {
		return 0;
	}

	/**
	 * @return the distance from the baseline to the top in virtual units, which is usually negative.
	 */
	public float getAscent() {
		return this.mRasterizer.getAscent() / this.mRasterScale;
	}

	/**
	 * @return the distance from the baseline to the bottom in virtual units, which is usually positive.
	 */
	public float getDescent() {
		return this.mRasterizer.getDescent() / this.mRasterScale;
	}

	/**
	 * @return raster pixels per virtual unit, fixed when the font was created.
	 */
	public float getRasterScale() {
		return this.mRasterScale;
	}

	/**
	 * @return the color passed when the font was created, applied when drawing (glyphs are alpha-only).
	 */
	public int getColor() {
		return this.mColorARGBPackedInt;
	}

	public GlyphAtlas getAtlas() {
		return this.mAtlas;
	}

	/**
	 * @return the number of atlas images per glyph: 1 (fill), or 2 (fill and stroke) for a {@link StrokeFont}.
	 */
	protected int getGlyphLayerCount() {
		return 1;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	/**
	 * @return <code>null</code>: glyphs live in the shared glyph atlas, see {@link Letter#mGlyph}.
	 */
	@Override
	public ITexture getTexture() {
		return null;
	}

	@Override
	public synchronized void load() {
		this.mFontManager.loadFont(this);
	}

	@Override
	public synchronized void unload() {
		this.mFontManager.unloadFont(this);
	}

	@Override
	public float getLineHeight() {
		return -this.getAscent() + this.getDescent();
	}

	@Override
	public synchronized Letter getLetter(final char pCharacter) throws FontException {
		Letter letter = this.mManagedCharacterToLetterMap.get(pCharacter);
		if(letter == null) {
			letter = this.createLetter(pCharacter);
			if(!letter.isWhitespace()) {
				this.mLettersPendingToBeDrawnToTexture.add(letter);
			}
			this.mManagedCharacterToLetterMap.put(pCharacter, letter);
		}
		return letter;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Queues every glyph for upload again into the slot it already has, e.g. after the atlas textures came back
	 * empty from a context loss.
	 */
	public synchronized void invalidateLetters() {
		final ArrayList<Letter> lettersPendingToBeDrawnToTexture = this.mLettersPendingToBeDrawnToTexture;
		final SparseArray<Letter> managedCharacterToLetterMap = this.mManagedCharacterToLetterMap;

		lettersPendingToBeDrawnToTexture.clear();
		for(int i = managedCharacterToLetterMap.size() - 1; i >= 0; i--) {
			final Letter letter = managedCharacterToLetterMap.valueAt(i);
			if(!letter.isWhitespace()) {
				if(letter.mGlyph != null) {
					letter.mGlyph.uploaded = false;
				}
				if(letter.mStrokeGlyph != null) {
					letter.mStrokeGlyph.uploaded = false;
				}
				lettersPendingToBeDrawnToTexture.add(letter);
			}
		}
	}

	public void prepareLetters(final char... pCharacters) throws FontException {
		for(final char character : pCharacters) {
			this.getLetter(character);
		}
	}

	private Letter createLetter(final char pCharacter) {
		final GlyphMetrics metrics = this.mMetrics;
		this.mRasterizer.measure(pCharacter, metrics);

		final float scale = this.mRasterScale;
		final float advance = metrics.advance / scale;

		if(Character.isWhitespace(pCharacter) || metrics.width <= 0 || metrics.height <= 0) {
			return new Letter(pCharacter, advance);
		}

		final GlyphSlot glyph = this.mAtlas.allocate(metrics.width, metrics.height);
		final GlyphSlot strokeGlyph = (glyph != null && this.getGlyphLayerCount() > 1) ? this.mAtlas.allocate(metrics.width, metrics.height) : null;
		if(glyph == null || (this.getGlyphLayerCount() > 1 && strokeGlyph == null)) {
			// Larger than an atlas layer: keep the layout, draw nothing.
			android.util.Log.w("Font", "Glyph '" + pCharacter + "' (" + metrics.width + "x" + metrics.height + ") does not fit into the glyph atlas");
			return new Letter(pCharacter, advance);
		}

		final float offsetY = (metrics.offsetY - this.mRasterizer.getAscent()) / scale;
		return new Letter(pCharacter, glyph, strokeGlyph, metrics.width / scale, metrics.height / scale, metrics.offsetX / scale, offsetY, advance);
	}

	/**
	 * Rasterizes and uploads the pending glyphs. GL thread only.
	 */
	public synchronized void update(final GLState pGLState) {
		final ArrayList<Letter> lettersPendingToBeDrawnToTexture = this.mLettersPendingToBeDrawnToTexture;
		final int count = lettersPendingToBeDrawnToTexture.size();
		if(count == 0) {
			return;
		}

		for(int i = 0; i < count; i++) {
			final Letter letter = lettersPendingToBeDrawnToTexture.get(i);
			this.upload(letter.mCharacter, 0, letter.mGlyph);
			if(letter.mStrokeGlyph != null) {
				this.upload(letter.mCharacter, 1, letter.mStrokeGlyph);
			}
		}
		lettersPendingToBeDrawnToTexture.clear();
	}

	private void upload(final char pCharacter, final int pLayer, final GlyphSlot pSlot) {
		final int size = pSlot.width * pSlot.height;
		ByteBuffer pixels = this.mPixels;
		if(pixels == null || pixels.capacity() < size) {
			pixels = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
			this.mPixels = pixels;
		}
		pixels.clear();
		this.mRasterizer.rasterize(pCharacter, pLayer, pixels);
		pixels.position(0);
		pixels.limit(size);
		this.mAtlas.upload(pSlot, pixels);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
