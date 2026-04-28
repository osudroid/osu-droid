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

	default int getRawPointerVersion(int pointerId) { return -1; }
	default boolean isRawPointerDown(int pointerId) { return false; }
	default float getRawPointerSurfaceX(int pointerId) { return 0f; }
	default float getRawPointerSurfaceY(int pointerId) { return 0f; }
	default long getRawPointerEventTime(int pointerId) { return 0L; }
	default int getRawPointerCapacity() { return 0; }
	default void clearRawPointers() { }
	default void resetRawPointers() { }
	default boolean isUseRawPointers() { return false; }
	// END osu!droid modified

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
