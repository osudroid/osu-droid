package org.anddev.andengine.opengl.vertex;

import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.Letter;
import org.anddev.andengine.util.HorizontalAlign;

import java.nio.FloatBuffer;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 18:05:08 - 07.04.2010
 */
public class TextVertexBuffer extends VertexBuffer {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int VERTICES_PER_CHARACTER = 6;

	// ===========================================================
	// Fields
	// ===========================================================

	private final HorizontalAlign mHorizontalAlign;

	// ===========================================================
	// Constructors
	// ===========================================================

	public TextVertexBuffer(final int pCharacterCount, final HorizontalAlign pHorizontalAlign, final int pDrawType, final boolean pManaged) {
		super(2 * VERTICES_PER_CHARACTER * pCharacterCount, pDrawType, pManaged);

		this.mHorizontalAlign = pHorizontalAlign;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void update(final Font font, final int pMaximumLineWidth, final int[] pWidths, final String[] pLines) {
		final FloatBuffer floatBuffer = this.mFloatBuffer;
		int i = 0;

		final float lineHeight = font.getLineHeight();

		final int lineCount = pLines.length;
		for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
			final String line = pLines[lineIndex];

			float lineX;
			switch(this.mHorizontalAlign) {
				case RIGHT:
					lineX = pMaximumLineWidth - pWidths[lineIndex];
					break;
				case CENTER:
					lineX = (pMaximumLineWidth - pWidths[lineIndex]) >> 1;
					break;
				case LEFT:
				default:
					lineX = 0;
			}

			final float lineY = lineIndex * (font.getLineHeight() + font.getLineGap());

            final int lineLength = line.length();
			for (int letterIndex = 0; letterIndex < lineLength; letterIndex++) {
				final Letter letter = font.getLetter(line.charAt(letterIndex));

				final float lineY2 = lineY + lineHeight;
				final float lineX2 = lineX + letter.mWidth;

				floatBuffer.position(0);

                floatBuffer.put(i++, lineX);
				floatBuffer.put(i++, lineY);

				floatBuffer.put(i++, lineX);
				floatBuffer.put(i++, lineY2);

				floatBuffer.put(i++, lineX2);
				floatBuffer.put(i++, lineY2);

				floatBuffer.put(i++, lineX2);
				floatBuffer.put(i++, lineY2);

				floatBuffer.put(i++, lineX2);
				floatBuffer.put(i++, lineY);

				floatBuffer.put(i++, lineX);
				floatBuffer.put(i++, lineY);

				floatBuffer.position(0);

				lineX += letter.mAdvance;
			}
		}

		super.setHardwareBufferNeedsUpdate();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
