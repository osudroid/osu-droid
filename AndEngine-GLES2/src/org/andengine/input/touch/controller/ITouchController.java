package org.andengine.input.touch.controller;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.options.TouchOptions;

import android.view.MotionEvent;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 20:23:45 - 13.07.2010
 */
public interface ITouchController extends IUpdateHandler {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void setTouchEventCallback(final ITouchEventCallback pTouchEventCallback);

	public void onHandleMotionEvent(final MotionEvent pMotionEvent);

	// BEGIN osu!droid modified - raw pointer operations + touch option application
	// Provided as default no-ops so third-party ITouchController implementations
	// don't have to be updated for this fork-specific API surface.

	/** Apply touch options (useRawPointer, processHistoricalEvents) to this controller. */
	default void applyTouchOptions(TouchOptions pTouchOptions) { }

	default boolean isRawPointerDown(int pointerId) { return false; }

	/**
	 * Reads the latest surface coordinates of a pointer. This method is thread-safe.
	 *
	 * @param pointerId The ID of the pointer.
	 * @param out An array of at least two elements receiving the X and Y coordinates, in that order.
	 * @return <code>false</code> if the pointer is outside of {@link #getRawPointerCapacity()}, in which case
	 * <code>out</code> is left untouched.
	 */
	default boolean getRawPointerSurfacePosition(int pointerId, float[] out) { return false; }

	default long getRawPointerEventTime(int pointerId) { return 0L; }
	default int getRawPointerCapacity() { return 0; }
	default void clearRawPointers() { }
	default void resetRawPointers() { }
	default boolean isUseRawPointer() { return false; }
	// END osu!droid modified

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
