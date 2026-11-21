package org.anddev.andengine.input.touch.controller;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.TouchOptions;
import org.anddev.andengine.input.touch.TouchEvent;

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
	// Final Fields
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void setTouchEventCallback(final ITouchEventCallback pTouchEventCallback);

	public void applyTouchOptions(final TouchOptions pTouchOptions);

	public boolean onHandleMotionEvent(final MotionEvent pMotionEvent);

	// BEGIN osu!droid modified - raw pointer operations
	boolean isRawPointerDown(int pointerId);

	/**
	 * Reads the latest surface coordinates of a pointer. This method is thread-safe.
	 *
	 * @param pointerId The ID of the pointer.
	 * @param out An array of at least two elements receiving the X and Y coordinates, in that order.
	 * @return <code>false</code> if the pointer is outside of {@link #getRawPointerCapacity()}, in which case
	 * <code>out</code> is left untouched.
	 */
	boolean getRawPointerSurfacePosition(int pointerId, float[] out);

	long getRawPointerEventTime(int pointerId);
	int getRawPointerCapacity();
	void clearRawPointers();
	void resetRawPointers();
	boolean isUseRawPointers();
	// END osu!droid modified

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	static interface ITouchEventCallback {
		public boolean onTouchEvent(final TouchEvent pTouchEvent);
	}
}
