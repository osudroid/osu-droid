package org.andengine.entity.text;

import java.util.ArrayList;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.shape.RectangularShape;
import org.andengine.entity.text.exception.OutOfCharactersException;
import org.andengine.graphics.DrawContext;
import org.andengine.opengl.font.FontUtils;
import org.andengine.opengl.font.IFont;
import org.andengine.opengl.font.Letter;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.adt.list.FloatArrayList;
import org.andengine.util.adt.list.IFloatList;

/**
 * TODO Try Degenerate Triangles?
 *
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 10:54:59 - 03.04.2010
 */
public class Text extends RectangularShape {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final float LEADING_DEFAULT = 0;

	/** Floats per letter in {@link #getLetterQuads()}: x0, y0, x1, y1. */
	public static final int LETTER_QUAD_SIZE = 4;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final IFont mFont;

	protected float mLineWidthMaximum;
	protected float mLineAlignmentWidth;

	protected TextOptions mTextOptions;
	protected final int mCharactersMaximum;
	protected int mCharactersToDraw;

	protected CharSequence mText;
	protected ArrayList<CharSequence> mLines = new ArrayList<CharSequence>(1);
	protected IFloatList mLineWidths = new FloatArrayList(1);

	/** The local rectangle of each visible letter as x0, y0 (top left), x1, y1 (bottom right). */
	private final float[] mLetterQuads;
	/** The visible letters, one per rectangle of {@link #mLetterQuads}. */
	private final Letter[] mLetters;
	private int mLetterCount;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Text(final float pX, final float pY, final IFont pFont, final CharSequence pText) {
		this(pX, pY, pFont, pText, pText.length(), new TextOptions());
	}

	public Text(final float pX, final float pY, final IFont pFont, final CharSequence pText, final TextOptions pTextOptions) {
		this(pX, pY, pFont, pText, pText.length(), pTextOptions);
	}

	public Text(final float pX, final float pY, final IFont pFont, final CharSequence pText, final int pCharactersMaximum) {
		this(pX, pY, pFont, pText, pCharactersMaximum, new TextOptions());
	}

	public Text(final float pX, final float pY, final IFont pFont, final CharSequence pText, final int pCharactersMaximum, final TextOptions pTextOptions) {
		super(pX, pY, 0, 0);

		this.mFont = pFont;
		this.mTextOptions = pTextOptions;
		this.mCharactersMaximum = pCharactersMaximum;
		this.mLetterQuads = new float[Text.LETTER_QUAD_SIZE * pCharactersMaximum];
		this.mLetters = new Letter[pCharactersMaximum];

		this.setText(pText);

		// Glyph atlas fonts are alpha-only, not premultiplied: the default blend function fits, no initBlendFunction(texture).
		this.setBlendingEnabled(true);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public IFont getFont() {
		return this.mFont;
	}

	public int getCharactersMaximum() {
		return this.mCharactersMaximum;
	}

	public CharSequence getText() {
		return this.mText;
	}

	/**
	 * @param pText
	 * @throws OutOfCharactersException leaves this {@link Text} object in an undefined state, until {@link Text#setText(CharSequence)} is called again and no {@link OutOfCharactersException} is thrown.
	 */
	public void setText(final CharSequence pText) throws OutOfCharactersException {
		this.mText = Text.truncateToCapacity(pText, this.mCharactersMaximum);
		final IFont font = this.mFont;

		this.mLines.clear();
		this.mLineWidths.clear();

		if(this.mTextOptions.mAutoWrap == AutoWrap.NONE) {
			this.mLines = FontUtils.splitLines(this.mText, this.mLines); // TODO Add whitespace-trimming.
		} else {
			this.mLines = FontUtils.splitLines(this.mFont, this.mText, this.mLines, this.mTextOptions.mAutoWrap, this.mTextOptions.mAutoWrapWidth);
		}

		final int lineCount = this.mLines.size();
		float maximumLineWidth = 0;
		for (int i = 0; i < lineCount; i++) {
			final float lineWidth = FontUtils.measureText(font, this.mLines.get(i));
			maximumLineWidth = Math.max(maximumLineWidth, lineWidth);

			this.mLineWidths.add(lineWidth);
		}
		this.mLineWidthMaximum = maximumLineWidth;

		if(this.mTextOptions.mAutoWrap == AutoWrap.NONE) {
			this.mLineAlignmentWidth = this.mLineWidthMaximum;
		} else {
			this.mLineAlignmentWidth = this.mTextOptions.mAutoWrapWidth;
		}

		super.mWidth = this.mLineAlignmentWidth;
		super.mHeight = lineCount * font.getLineHeight() + (lineCount - 1) * this.mTextOptions.mLeading;

		this.mRotationCenterX = super.mWidth * 0.5f;
		this.mRotationCenterY = super.mHeight * 0.5f;

		this.mScaleCenterX = this.mRotationCenterX;
		this.mScaleCenterY = this.mRotationCenterY;

		this.onUpdateVertices();
	}

	public ArrayList<CharSequence> getLines() {
		return this.mLines;
	}

	public IFloatList getLineWidths() {
		return this.mLineWidths;
	}

	public float getLineAlignmentWidth() {
		return this.mLineAlignmentWidth;
	}

	public float getLineWidthMaximum() {
		return this.mLineWidthMaximum;
	}

	public float getLeading() {
		return this.mTextOptions.mLeading;
	}

	public void setLeading(final float pLeading) {
		this.mTextOptions.mLeading = pLeading;

		this.invalidateText();
	}

	public HorizontalAlign getHorizontalAlign() {
		return this.mTextOptions.mHorizontalAlign;
	}

	public void setHorizontalAlign(final HorizontalAlign pHorizontalAlign) {
		this.mTextOptions.mHorizontalAlign = pHorizontalAlign;

		this.invalidateText();
	}

	public AutoWrap getAutoWrap() {
		return this.mTextOptions.mAutoWrap;
	}

	public void setAutoWrap(final AutoWrap pAutoWrap) {
		this.mTextOptions.mAutoWrap = pAutoWrap;

		this.invalidateText();
	}

	public float getAutoWrapWidth() {
		return this.mTextOptions.mAutoWrapWidth;
	}

	public void setAutoWrapWidth(final float pAutoWrapWidth) {
		this.mTextOptions.mAutoWrapWidth = pAutoWrapWidth;

		this.invalidateText();
	}

	public TextOptions getTextOptions() {
		return this.mTextOptions;
	}

	public void setTextOptions(final TextOptions pTextOptions) {
		this.mTextOptions = pTextOptions;
	}

	public int getCharactersToDraw() {
		return this.mCharactersToDraw;
	}

	public void setCharactersToDraw(final int pCharactersToDraw) {
		if(pCharactersToDraw > this.mCharactersMaximum) {
			throw new OutOfCharactersException("Characters: maximum: '" + this.mCharactersMaximum + "' required: '" + pCharactersToDraw + "'.");
		}
		this.mCharactersToDraw = pCharactersToDraw;
	}

	/**
	 * @return the local rectangle of each visible letter, {@link #LETTER_QUAD_SIZE} floats per letter:
	 * x0, y0 (top left), x1, y1 (bottom right). Only the first {@link #getLetterCount()} are set. The
	 * array is the text's own, read only.
	 */
	public float[] getLetterQuads() {
		return this.mLetterQuads;
	}

	/**
	 * @return the visible letters, one per rectangle of {@link #getLetterQuads()}. The array is the
	 * text's own, read only.
	 */
	public Letter[] getLetters() {
		return this.mLetters;
	}

	/** @return how many entries of {@link #getLetters()} and {@link #getLetterQuads()} are set. */
	public int getLetterCount() {
		return this.mLetterCount;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void draw(final DrawContext pGLState, final Camera pCamera) {
		TextBatching.add(this, pGLState, this.mCharactersToDraw);
	}

	/**
	 * Lays out the letters of {@link #mLines}: each line is aligned in {@link #mLineAlignmentWidth},
	 * lines are {@link IFont#getLineHeight()} plus the leading apart. Whitespace takes up its advance
	 * but gets no rectangle.
	 */
	@Override
	protected void onUpdateVertices() {
		final float[] quads = this.mLetterQuads;

		final IFont font = this.mFont;
		final ArrayList<CharSequence> lines = this.mLines;
		final float lineHeight = font.getLineHeight();
		final IFloatList lineWidths = this.mLineWidths;

		final float lineAlignmentWidth = this.mLineAlignmentWidth;
		final int capacity = this.mCharactersMaximum;

		int charactersToDraw = 0;
		int letterCount = 0;

		final int lineCount = lines.size();
		for (int row = 0; row < lineCount; row++) {
			final CharSequence line = lines.get(row);

			float xBase;
			switch(this.mTextOptions.mHorizontalAlign) {
				case RIGHT:
					xBase = lineAlignmentWidth - lineWidths.get(row);
					break;
				case CENTER:
					xBase = (lineAlignmentWidth - lineWidths.get(row)) * 0.5f;
					break;
				case LEFT:
				default:
					xBase = 0;
			}

			final float yBase = row * (lineHeight + this.mTextOptions.mLeading);

			final int lineLength = line.length();
			Letter previousLetter = null;
			for(int i = 0; i < lineLength; i++) {
				final Letter letter = font.getLetter(line.charAt(i));
				if(previousLetter != null) {
					xBase += previousLetter.getKerning(letter.mCharacter);
				}

				if(!letter.isWhitespace()) {
					if(letterCount < capacity) {
						final float x = xBase + letter.mOffsetX;
						final float y = yBase + letter.mOffsetY;

						final int o = letterCount * Text.LETTER_QUAD_SIZE;
						quads[o] = x;
						quads[o + 1] = y;
						quads[o + 2] = x + letter.mWidth;
						quads[o + 3] = y + letter.mHeight;

						this.mLetters[letterCount] = letter;
						letterCount++;
					}
					charactersToDraw++;
				}

				xBase += letter.mAdvance;

				previousLetter = letter;
			}
		}

		// Letters left over from a longer text are not drawn, but should not be kept alive either.
		for(int i = letterCount; i < this.mLetterCount; i++) {
			this.mLetters[i] = null;
		}
		this.mLetterCount = letterCount;

		this.setCharactersToDraw(charactersToDraw);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void invalidateText() {
		this.setText(this.mText);
	}

	/**
	 * Cuts down pText so it can never exceed pCharactersMaximum drawable characters, appending an
	 * ellipsis when truncated. Whitespace/newlines don't count towards the drawn character total, so
	 * using the raw length here is a conservative (i.e. safe) upper bound on it.
	 */
	private static CharSequence truncateToCapacity(final CharSequence pText, final int pCharactersMaximum) {
		if (pText.length() <= pCharactersMaximum) {
			return pText;
		}

		final int cutIndex = Math.max(0, pCharactersMaximum - 1);
		return pText.subSequence(0, cutIndex).toString() + "…";
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
