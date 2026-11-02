package org.andengine.opengl.font;

import org.andengine.opengl.font.exception.LetterNotFoundException;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 12:15:25 - 03.11.2011
 */
public interface IFont {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void load();
	public void unload();

	public float getLineHeight();

	public Letter getLetter(final char pChar) throws LetterNotFoundException;
}
