package org.anddev.andengine.entity.text;

import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.StringUtils;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 18:07:06 - 08.07.2010
 */
// osu!droid modified: drawVertices() override removed as the base Text implementation's
// page-run-based drawing already handles variable-length text correctly.
public class ChangeableText extends Text {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final String ELLIPSIS = "...";
	private static final int ELLIPSIS_CHARACTER_COUNT = ELLIPSIS.length();

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public ChangeableText(final float pX, final float pY, final Font pFont, final String pText) {
		this(pX, pY, pFont, pText, pText.length() - StringUtils.countOccurrences(pText, '\n'));
	}

	public ChangeableText(final float pX, final float pY, final Font pFont, final String pText, final int pCharactersMaximum) {
		this(pX, pY, pFont, pText, HorizontalAlign.LEFT, pCharactersMaximum);
	}

	public ChangeableText(final float pX, final float pY, final Font pFont, final String pText, final HorizontalAlign pHorizontalAlign, final int pCharactersMaximum) {
		super(pX, pY, pFont, pText, pHorizontalAlign, pCharactersMaximum);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setText(final String pText) {
		this.setText(pText, false);
	}

	/**
	 * @param pText
	 * @param pAllowEllipsis in the case pText is longer than <code>pCharactersMaximum</code>,
	 *	which was passed to the constructor, the displayed text will end with an ellipsis ("...").
	 */
	public void setText(final String pText, final boolean pAllowEllipsis) {
		final int textCharacterCount = pText.length() - StringUtils.countOccurrences(pText, '\n');
		if(textCharacterCount > this.mCharactersMaximum) {
			if(pAllowEllipsis && this.mCharactersMaximum > ELLIPSIS_CHARACTER_COUNT) {
				this.updateText(pText.substring(0, this.mCharactersMaximum - ELLIPSIS_CHARACTER_COUNT).concat(ELLIPSIS)); // TODO This allocation could maybe be avoided...
			} else {
				this.updateText(pText.substring(0, this.mCharactersMaximum)); // TODO This allocation could be avoided...
			}
		} else {
			this.updateText(pText);
		}
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// osu!droid modified: drawVertices() no longer needs to be overridden here. The base Text
	// implementation now draws exactly the page runs produced by the last update(), which are
	// already sized to the current text's actual character count (see TextTextureBuffer.update()).

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
