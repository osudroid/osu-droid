package org.anddev.andengine.extension.input.touch.controller;

import org.anddev.andengine.engine.options.TouchOptions;
import org.anddev.andengine.input.touch.controller.BaseTouchController;

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

    // BEGIN osu!droid modified - Option to disable historical events processing.
    private boolean mProcessHistoricalEvents;
    // END osu!droid modified

	// ===========================================================
	// Constructors
	// ===========================================================

	// BEGIN osu!droid modified - Remove Android version check.
	public MultiTouchController() /*throws MultiTouchException*/ {
		/*if(MultiTouch.isSupportedByAndroidVersion() == false) {
			throw new MultiTouchException();
		}*/
	}
	// END osu!droid modified

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean onHandleMotionEvent(final MotionEvent pMotionEvent) {
		final int action = pMotionEvent.getAction() & MotionEvent.ACTION_MASK;
		switch(action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				return this.onHandleTouchAction(MotionEvent.ACTION_DOWN, pMotionEvent);
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				return this.onHandleTouchAction(MotionEvent.ACTION_UP, pMotionEvent);
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_OUTSIDE:
				return this.onHandleTouchAction(action, pMotionEvent);
			case MotionEvent.ACTION_MOVE:
				return this.onHandleTouchMove(pMotionEvent);
			default:
				return false;
		}
	}

    @Override
    public void applyTouchOptions(TouchOptions pTouchOptions) {
        super.applyTouchOptions(pTouchOptions);

        this.mProcessHistoricalEvents = pTouchOptions.isProcessHistoricalEvents();
    }

	// ===========================================================
	// Methods
	// ===========================================================

	private boolean onHandleTouchMove(final MotionEvent pMotionEvent) {
		boolean handled = false;
        // BEGIN osu!droid modified - process historical coordinates in time order and update raw pointer state
        // See: https://developer.android.com/reference/android/view/MotionEvent#batching
        final int pointerCount = pMotionEvent.getPointerCount();

        if (this.mProcessHistoricalEvents) {
            final int historySize = pMotionEvent.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                final long time = pMotionEvent.getHistoricalEventTime(h);

                for (int p = 0; p < pointerCount; p++) {
                    final int pointerID = pMotionEvent.getPointerId(p);
                    final boolean handledInner = this.fireTouchEvent(time, pMotionEvent.getHistoricalX(p, h), pMotionEvent.getHistoricalY(p, h), MotionEvent.ACTION_MOVE, pointerID, pMotionEvent);
                    handled = handled || handledInner;
                }
            }
        }

//        for(int i = pMotionEvent.getPointerCount() - 1; i >= 0; i--) {
        for (int i = pointerCount - 1; i >= 0; i--) {
            final int pointerID = pMotionEvent.getPointerId(i);
			updateRawPointer(pointerID, pMotionEvent.getX(i), pMotionEvent.getY(i), true, pMotionEvent.getEventTime());
			final boolean handledInner = this.fireTouchEvent(pMotionEvent.getX(i), pMotionEvent.getY(i), MotionEvent.ACTION_MOVE, pointerID, pMotionEvent);
			handled = handled || handledInner;
		}
        // END osu!droid modified
		return handled;
	}

	private boolean onHandleTouchAction(final int pAction, final MotionEvent pMotionEvent) {
		final int pointerIndex = this.getPointerIndex(pMotionEvent);
		final int pointerID = pMotionEvent.getPointerId(pointerIndex);

		// BEGIN osu!droid modified - update raw pointer state
		if (pAction == MotionEvent.ACTION_CANCEL || pAction == MotionEvent.ACTION_OUTSIDE) {
			clearRawPointers();
		} else {
			updateRawPointer(pointerID, pMotionEvent.getX(pointerIndex), pMotionEvent.getY(pointerIndex), pAction == MotionEvent.ACTION_DOWN, pMotionEvent.getEventTime());
		}
		// END osu!droid modified

		return this.fireTouchEvent(pMotionEvent.getX(pointerIndex), pMotionEvent.getY(pointerIndex), pAction, pointerID, pMotionEvent);
	}

	private int getPointerIndex(final MotionEvent pMotionEvent) {
		return (pMotionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
