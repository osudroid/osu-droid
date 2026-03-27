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
	int getRawPointerVersion(int pointerId);
	boolean isRawPointerDown(int pointerId);
	float getRawPointerSurfaceX(int pointerId);
	float getRawPointerSurfaceY(int pointerId);
	long getRawPointerEventTime(int pointerId);
	int getRawPointerCapacity();
	void clearRawPointers();
	boolean isUseRawPointers();
	// END osu!droid modified

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	static interface ITouchEventCallback {
		public boolean onTouchEvent(final TouchEvent pTouchEvent);
	}
}
