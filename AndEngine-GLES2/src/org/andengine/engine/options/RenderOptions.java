package org.andengine.engine.options;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 13:01:40 - 02.07.2010
 */
public class RenderOptions {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean mMultiSampling = false;
	private boolean mDithering = false;
	private ColorDepth mColorDepth = ColorDepth.RGB565;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean isMultiSampling() {
		return this.mMultiSampling;
	}

	public void setMultiSampling(final boolean pMultiSampling) {
		this.mMultiSampling = pMultiSampling;
	}

	public boolean isDithering() {
		return this.mDithering;
	}

	public void setDithering(final boolean pDithering) {
		this.mDithering = pDithering;
	}

	public ColorDepth getColorDepth() {
		return this.mColorDepth;
	}

	/**
	 * Only read when the render surface view gets its renderer, so it has to be set before that.
	 */
	public void setColorDepth(final ColorDepth pColorDepth) {
		if (pColorDepth == null) {
			throw new IllegalArgumentException("pColorDepth must not be null.");
		}
		this.mColorDepth = pColorDepth;
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
