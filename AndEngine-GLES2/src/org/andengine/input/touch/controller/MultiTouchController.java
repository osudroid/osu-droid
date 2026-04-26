package org.andengine.input.touch.controller;

import android.view.MotionEvent;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 20:23:33 - 13.07.2010
 */
public class MultiTouchController extends BaseTouchController {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onHandleMotionEvent(final MotionEvent pMotionEvent) {
		final int action = pMotionEvent.getAction() & MotionEvent.ACTION_MASK;
		switch(action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				this.onHandleTouchAction(MotionEvent.ACTION_DOWN, pMotionEvent);
				return;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				this.onHandleTouchAction(MotionEvent.ACTION_UP, pMotionEvent);
				return;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				this.onHandleTouchAction(action, pMotionEvent);
				return;
			case MotionEvent.ACTION_MOVE:
				this.onHandleTouchMove(pMotionEvent);
				return;
			default:
				throw new IllegalArgumentException("Invalid Action detected: " + action);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void onHandleTouchMove(final MotionEvent pMotionEvent) {
		// BEGIN osu!droid modified - process historical coordinates in time order and update raw pointer state
		// See: https://developer.android.com/reference/android/view/MotionEvent#batching
		// Process historical (batched) positions first, for accurate slider tracking.
		// Use fireTouchEvent(eventTime, ...) so each historical sample carries its own timestamp.
		if (isProcessHistoricalEvents()) {
			final int historySize = pMotionEvent.getHistorySize();
			for (int h = 0; h < historySize; h++) {
				final long historicalEventTime = pMotionEvent.getHistoricalEventTime(h);
				for (int i = pMotionEvent.getPointerCount() - 1; i >= 0; i--) {
					final int pointerID = pMotionEvent.getPointerId(i);
					this.fireTouchEvent(historicalEventTime, pMotionEvent.getHistoricalX(i, h), pMotionEvent.getHistoricalY(i, h), MotionEvent.ACTION_MOVE, pointerID, pMotionEvent);
				}
			}
		}
		// Process current positions and update raw pointer state
		for(int i = pMotionEvent.getPointerCount() - 1; i >= 0; i--) {
			final int pointerIndex = i;
			final int pointerID = pMotionEvent.getPointerId(pointerIndex);
			final float x = pMotionEvent.getX(pointerIndex);
			final float y = pMotionEvent.getY(pointerIndex);
			updateRawPointer(pointerID, x, y, true, pMotionEvent.getEventTime());
			this.fireTouchEvent(x, y, MotionEvent.ACTION_MOVE, pointerID, pMotionEvent);
		}
		// END osu!droid modified
	}

	// BEGIN osu!droid modified - update raw pointer state on DOWN/UP/CANCEL actions
	private void onHandleTouchAction(final int pAction, final MotionEvent pMotionEvent) {
		final int pointerIndex = MultiTouchController.getPointerIndex(pMotionEvent);
		final int pointerID = pMotionEvent.getPointerId(pointerIndex);
		final float x = pMotionEvent.getX(pointerIndex);
		final float y = pMotionEvent.getY(pointerIndex);
		final boolean isDown = pAction == MotionEvent.ACTION_DOWN;
		updateRawPointer(pointerID, x, y, isDown, pMotionEvent.getEventTime());
		this.fireTouchEvent(x, y, pAction, pointerID, pMotionEvent);
	}
	// END osu!droid modified

	private static int getPointerIndex(final MotionEvent pMotionEvent) {
		return (pMotionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
