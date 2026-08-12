package org.andengine.entity.primitive;

import android.opengl.GLES32;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 18:46:51 - 28.03.2012
 */
public enum DrawMode {
	// ===========================================================
	// Elements
	// ===========================================================

	POINTS(GLES32.GL_POINTS),
	LINE_STRIP(GLES32.GL_LINE_STRIP),
	LINE_LOOP(GLES32.GL_LINE_LOOP),
	LINES(GLES32.GL_LINES),
	TRIANGLE_STRIP(GLES32.GL_TRIANGLE_STRIP),
	TRIANGLE_FAN(GLES32.GL_TRIANGLE_FAN),
	TRIANGLES(GLES32.GL_TRIANGLES);

	// ===========================================================
	// Constants
	// ===========================================================

	private final int mDrawMode;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	private DrawMode(final int pDrawMode) {
		this.mDrawMode = pDrawMode;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getDrawMode() {
		return this.mDrawMode;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}