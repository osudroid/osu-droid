package org.anddev.andengine.opengl.font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.TextureOptions;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLUtils;

import android.util.SparseArray;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 10:39:33 - 03.04.2010
 */
// osu!droid modified:
// * Support for UTF-16 characters by using String instead of Char where needed.
// * Removal of non-sense padding.
// * Use multiple atlas pages instead of overflowing a single fixed-size texture,
//   so already-placed glyphs remain valid.
public class Font {
	// ===========================================================
	// Constants
	// ===========================================================

	protected static final float LETTER_LEFT_OFFSET = 0;
	protected static final int LETTER_EXTRA_WIDTH = 10;

	//protected final static int PADDING = 0;

	// ===========================================================
	// Fields
	// ===========================================================

	private final List<ITexture> mPages = new ArrayList<>();
	private final Supplier<ITexture> mPageFactory;
	private final float mPageWidth;
	private final float mPageHeight;
	private int mCurrentTextureX = 0;
	private int mCurrentTextureY = 0;

	private final HashMap<String, Letter> mManagedCharacterToLetterMap = new HashMap<String, Letter>();
	private final ArrayList<Letter> mLettersPendingToBeDrawnToTexture = new ArrayList<Letter>();

	protected final Paint mPaint;
	private final Paint mBackgroundPaint;

	protected final FontMetrics mFontMetrics;
	private final int mLineHeight;
	private final int mLineGap;

	private final Size mCreateLetterTemporarySize = new Size();
	private final Rect mGetLetterBitmapTemporaryRect = new Rect();
	private final Rect mGetStringWidthTemporaryRect = new Rect();
	private final Rect mGetLetterBoundsTemporaryRect = new Rect();
	private final float[] mTemporaryTextWidthFetchers = new float[2]; // Size 2 to support surrogate pairs.

	protected final Canvas mCanvas = new Canvas();

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param pPageFactory creates and registers (e.g. with the {@link org.anddev.andengine.opengl.texture.TextureManager})
	 *                     an atlas page, invoked once immediately for the first page and again whenever the current
	 *                     page runs out of room for new glyphs. Every page it produces <b>must</b> share the same dimensions
	 *                     and options, since those of the first page are cached and reused for all subsequent pages.
	 */
	public Font(final Supplier<ITexture> pPageFactory, final Typeface pTypeface, final float pSize, final boolean pAntiAlias, final int pColor) {
		final ITexture firstPage = pPageFactory.get();
		this.mPages.add(firstPage);
		this.mPageFactory = pPageFactory;
		this.mPageWidth = firstPage.getWidth();
		this.mPageHeight = firstPage.getHeight();

		this.mPaint = new Paint();
		this.mPaint.setTypeface(pTypeface);
		this.mPaint.setColor(pColor);
		this.mPaint.setTextSize(pSize);
		this.mPaint.setAntiAlias(pAntiAlias);

		this.mBackgroundPaint = new Paint();
		this.mBackgroundPaint.setColor(Color.TRANSPARENT);
		this.mBackgroundPaint.setStyle(Style.FILL);

		this.mFontMetrics = this.mPaint.getFontMetrics();
		this.mLineHeight = (int) Math.ceil(Math.abs(this.mFontMetrics.ascent) + Math.abs(this.mFontMetrics.descent));
		this.mLineGap = (int) (Math.ceil(this.mFontMetrics.leading));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getLineGap() {
		return this.mLineGap;
	}

	public int getLineHeight() {
		return this.mLineHeight;
	}

	public ITexture getTexture() {
		return this.mPages.get(0);
	}

	public synchronized TextureOptions getTextureOptions() {
		return this.mPages.get(0).getTextureOptions();
	}

	private ITexture getCurrentPage() {
		return this.mPages.get(this.mPages.size() - 1);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void reload() {
		final ArrayList<Letter> lettersPendingToBeDrawnToTexture = this.mLettersPendingToBeDrawnToTexture;
		final HashMap<String, Letter> managedCharacterToLetterMap = this.mManagedCharacterToLetterMap;

		/* Make all letters redraw to the texture. */
		lettersPendingToBeDrawnToTexture.addAll(managedCharacterToLetterMap.values());
	}

	private int getLetterAdvance(final String pCharacter) {
		this.mPaint.getTextWidths(pCharacter, this.mTemporaryTextWidthFetchers);
		return (int) (Math.ceil(this.mTemporaryTextWidthFetchers[0]));
	}
	
	private Bitmap getLetterBitmap(final String pCharacter) {
		final Rect getLetterBitmapTemporaryRect = this.mGetLetterBitmapTemporaryRect;
        this.mPaint.getTextBounds(pCharacter, 0, pCharacter.length(), getLetterBitmapTemporaryRect);

		//getLetterBitmapTemporaryRect.right += PADDING * 2;

		final int lineHeight = this.getLineHeight();
		final Bitmap bitmap = Bitmap.createBitmap(getLetterBitmapTemporaryRect.width() == 0 ? 1 : getLetterBitmapTemporaryRect.width() + LETTER_EXTRA_WIDTH, lineHeight, Bitmap.Config.ARGB_8888);
		this.mCanvas.setBitmap(bitmap);

		/* Make background transparent. */
		this.mCanvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), this.mBackgroundPaint);

		/* Actually draw the character. */
		this.drawCharacterString(pCharacter);

		return bitmap;
	}

	protected void drawCharacterString(final String pCharacterAsString) {
		this.mCanvas.drawText(pCharacterAsString, LETTER_LEFT_OFFSET/* + PADDING*/, -this.mFontMetrics.ascent/* + PADDING*/, this.mPaint);
	}

	public int getStringWidth(final String pText) {
		this.mPaint.getTextBounds(pText, 0, pText.length(), this.mGetStringWidthTemporaryRect);
		return this.mGetStringWidthTemporaryRect.width();
	}

	private void getLetterBounds(final String pCharacter, final Size pSize) {
		this.mPaint.getTextBounds(pCharacter, 0, pCharacter.length(), this.mGetLetterBoundsTemporaryRect);
		pSize.set(this.mGetLetterBoundsTemporaryRect.width() + LETTER_EXTRA_WIDTH, this.getLineHeight());
	}

	public void prepareLetters(final char ... pCharacters) {
		for(final char character : pCharacters) {
			this.getLetter(character);
		}
	}

	public synchronized Letter getLetter(final char pCharacter) {
		return this.getLetter(String.valueOf(pCharacter));
	}

	public synchronized Letter getLetter(final String pCharacter) {
		final HashMap<String, Letter> managedCharacterToLetterMap = this.mManagedCharacterToLetterMap;
		Letter letter = managedCharacterToLetterMap.get(pCharacter);
		if (letter == null) {
			letter = this.createLetter(pCharacter);

			this.mLettersPendingToBeDrawnToTexture.add(letter);
			managedCharacterToLetterMap.put(pCharacter, letter);
		}
		return letter;
	}

	private Letter createLetter(final String pCharacter) {
		final float textureWidth = this.mPageWidth;
		final float textureHeight = this.mPageHeight;

		final Size createLetterTemporarySize = this.mCreateLetterTemporarySize;
		this.getLetterBounds(pCharacter, createLetterTemporarySize);

		final float letterWidth = createLetterTemporarySize.getWidth();
		final float letterHeight = createLetterTemporarySize.getHeight();

		if (this.mCurrentTextureX + letterWidth >= textureWidth) {
			this.mCurrentTextureX = 0;
			this.mCurrentTextureY += this.getLineGap() + this.getLineHeight();
		}

		if (this.mCurrentTextureY + letterHeight > textureHeight) {
			this.addPage();
		}

		final float letterTextureX = this.mCurrentTextureX / textureWidth;
		final float letterTextureY = this.mCurrentTextureY / textureHeight;
		final float letterTextureWidth = letterWidth / textureWidth;
		final float letterTextureHeight = letterHeight / textureHeight;

		final Letter letter = new Letter(this.getCurrentPage(), pCharacter, this.getLetterAdvance(pCharacter), (int)letterWidth, (int)letterHeight, letterTextureX, letterTextureY, letterTextureWidth, letterTextureHeight);
		this.mCurrentTextureX += letterWidth;

		return letter;
	}

	/**
	 * Allocates a new, empty atlas page rather than reusing/clearing the current one, so that
	 * {@link Letter}s already handed out (and any {@link org.anddev.andengine.entity.text.Text}
	 * built from them) keep pointing at valid, unmodified texture data.
	 */
	private void addPage() {
		this.mPages.add(this.mPageFactory.get());
		this.mCurrentTextureX = 0;
		this.mCurrentTextureY = 0;
	}

	public synchronized void update(final GL10 pGL) {
		final ArrayList<Letter> lettersPendingToBeDrawnToTexture = this.mLettersPendingToBeDrawnToTexture;
		if(lettersPendingToBeDrawnToTexture.size() > 0) {
			final float textureWidth = this.mPageWidth;
			final float textureHeight = this.mPageHeight;

			ITexture boundPage = null;
			for(int i = lettersPendingToBeDrawnToTexture.size() - 1; i >= 0; i--) {
				final Letter letter = lettersPendingToBeDrawnToTexture.get(i);
				final ITexture page = letter.getTexture();
				if (page != boundPage) {
					page.bind(pGL);
					boundPage = page;
				}

				final Bitmap bitmap = this.getLetterBitmap(letter.mCharacter);

				// TODO What about premultiplyalpha of the textureOptions?
				GLUtils.texSubImage2D(GL10.GL_TEXTURE_2D, 0, (int)(letter.mTextureX * textureWidth), (int)(letter.mTextureY * textureHeight), bitmap);

				bitmap.recycle();
			}
			lettersPendingToBeDrawnToTexture.clear();
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}