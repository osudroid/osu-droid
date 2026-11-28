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
	 * The number of frames the driver reported as not presented. Some drivers report those frames
	 * as {@link #unknown} instead.
	 */
	public int dropped;

	/**
	 * The number of frames whose outcome could not be determined.
	 */
	public int unknown;
}
