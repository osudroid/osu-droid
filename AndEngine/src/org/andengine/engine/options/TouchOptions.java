package org.andengine.engine.options;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 23:18:06 - 22.11.2011
 */
public class TouchOptions {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long TOUCHEVENT_INTERVAL_MILLISECONDS_DEFAULT = 20;

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean mNeedsMultiTouch;
	private boolean mRunOnUpdateThread;
	private long mTouchEventIntervalMilliseconds = TouchOptions.TOUCHEVENT_INTERVAL_MILLISECONDS_DEFAULT;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public TouchOptions enableRunOnUpdateThread() {
		return this.setRunOnUpdateThread(true);
	}

	public TouchOptions disableRunOnUpdateThread() {
		return this.setRunOnUpdateThread(false);
	}

	public TouchOptions setRunOnUpdateThread(final boolean pRunOnUpdateThread) {
		this.mRunOnUpdateThread = pRunOnUpdateThread;
		return this;
	}

	public boolean needsMultiTouch() {
		return this.mNeedsMultiTouch;
	}

	public TouchOptions setNeedsMultiTouch(final boolean pNeedsMultiTouch) {
		this.mNeedsMultiTouch = pNeedsMultiTouch;
		return this;
	}

	/**
	 * <u><b>Default:</b></u> <code>true</code>
	 */
	public boolean isRunOnUpdateThread() {
		return this.mRunOnUpdateThread;
	}

	public long getTouchEventIntervalMilliseconds() {
		return this.mTouchEventIntervalMilliseconds;
	}

	public void setTouchEventIntervalMilliseconds(final long pTouchEventIntervalMilliseconds) {
		this.mTouchEventIntervalMilliseconds = pTouchEventIntervalMilliseconds;
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
