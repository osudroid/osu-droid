package org.anddev.andengine.input.touch.controller;

import android.view.MotionEvent;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 20:23:33 - 13.07.2010
 */
public class SingleTouchControler extends BaseTouchController {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public SingleTouchControler() {

	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean onHandleMotionEvent(final MotionEvent pMotionEvent) {
		// BEGIN osu!droid modified - update raw pointer state
		final int action = pMotionEvent.getAction();
		final boolean isDown = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE;

		updateRawPointer(0, pMotionEvent.getX(), pMotionEvent.getY(), isDown, pMotionEvent.getEventTime());
		// END osu!droid modified

		return this.fireTouchEvent(pMotionEvent.getX(), pMotionEvent.getY(), pMotionEvent.getAction(), 0, pMotionEvent);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
