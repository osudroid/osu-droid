// osu!droid ONLY
package org.anddev.andengine.opengl.view;

/**
 * Counts of frame outcomes gathered since the previous drain of a {@link PresentedFrameTracker}.
 */
public final class PresentedFrameStats {
	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * The number of frames swapped.
	 */
	public int swapped;

	/**
	 * The number of frames that reached the display.
	 */
	public int presented;

	/**
	 * The number of frames that were replaced before reaching the display.
	 */
	public int dropped;

	/**
	 * The number of frames whose outcome could not be determined.
	 */
	public int unknown;
}
