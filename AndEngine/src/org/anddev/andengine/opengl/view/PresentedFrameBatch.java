// osu!droid ONLY
package org.anddev.andengine.opengl.view;

/**
 * The presented frames gathered since the previous drain of a {@link PresentedFrameTracker}.
 */
public final class PresentedFrameBatch {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int CAPACITY = 16;

	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * The present times in nanoseconds, oldest first. Only the first {@link #count} entries are valid.
	 */
	public final long[] presentTimes = new long[CAPACITY];

	/**
	 * The number of valid entries in {@link #presentTimes}.
	 */
	public int count;

	/**
	 * Whether presented frames are being tracked and have been seen since tracking was last reset.
	 */
	public boolean isAvailable;

	/**
	 * Whether tracking was reset, or present times were discarded, since the previous drain.
	 * <p>
	 * Time between presented frames is not meaningful across a reset.
	 */
	public boolean wasReset;
}
