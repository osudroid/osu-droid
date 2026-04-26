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
	/** Apply touch options (useRawPointer, processHistoricalEvents) to this controller. */
	void applyTouchOptions(TouchOptions pTouchOptions);


	int getRawPointerVersion(int pointerId);
	boolean isRawPointerDown(int pointerId);
	float getRawPointerSurfaceX(int pointerId);
	float getRawPointerSurfaceY(int pointerId);
	long getRawPointerEventTime(int pointerId);
	int getRawPointerCapacity();
	void clearRawPointers();
	void resetRawPointers();
	boolean isUseRawPointers();
	// END osu!droid modified

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
