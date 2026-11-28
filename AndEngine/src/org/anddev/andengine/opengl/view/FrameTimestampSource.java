// osu!droid ONLY
package org.anddev.andengine.opengl.view;

import org.anddev.andengine.util.annotation.GLThread;

/**
 * Reports when frames reach the display. Every method works on the surface that is current on the calling thread.
 */
@GLThread
public interface FrameTimestampSource {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * The result of a frame that was never presented.
	 */
	long RESULT_DROPPED = -1;

	/**
	 * The result of a frame whose present time is not known yet.
	 */
	long RESULT_PENDING = -2;

	/**
	 * The result of a frame that cannot be queried anymore.
	 */
	long RESULT_UNKNOWN = -3;

	/**
	 * The frame ID returned when there is no frame about to be swapped.
	 */
	long NO_FRAME_ID = -1;

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Enables frame timestamps.
	 *
	 * @return Whether frame timestamps are available.
	 */
	boolean start();

	/**
	 * Disables frame timestamps.
	 */
	void stop();

	/**
	 * Looks up the present times of the given frames, then gets the ID of the frame about to be swapped.
	 * <p>
	 * The frames are expected oldest first. If a frame is still pending, the frames after it may be
	 * reported as {@link #RESULT_PENDING} without being looked up.
	 *
	 * @param pFrameIDs The IDs of the frames to look up.
	 * @param pCount    The number of IDs in {@code pFrameIDs}.
	 * @param pResults  Receives one result per frame ID (its present time in nanoseconds, or one of
	 *                  the {@code RESULT_} constants).
	 * @return The ID of the frame about to be swapped, or {@link #NO_FRAME_ID}.
	 */
	long queryAndGetNextFrameId(final long[] pFrameIDs, final int pCount, final long[] pResults);
}
