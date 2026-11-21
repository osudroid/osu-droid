package org.anddev.andengine.input.touch.controller;

import org.anddev.andengine.engine.options.TouchOptions;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.util.pool.RunnablePoolItem;
import org.anddev.andengine.util.pool.RunnablePoolUpdateHandler;

import android.view.MotionEvent;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 21:06:40 - 13.07.2010
 */
public abstract class BaseTouchController implements ITouchController  {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// BEGIN osu!droid modified - raw pointer for events without having to wait for an update tick
	private static final int RAW_POINTER_CAPACITY = 10;

	private boolean mUseRawPointer;
	// The surface X and Y coordinates of each pointer, packed into one long (X in the high 32 bits,
	// Y in the low 32 bits).
	private final AtomicLongArray mRawPointerSurfacePosition = new AtomicLongArray(RAW_POINTER_CAPACITY);
	private final boolean[] mRawPointerDown = new boolean[RAW_POINTER_CAPACITY];
	private final long[] mRawPointerEventTime = new long[RAW_POINTER_CAPACITY];
	// END osu!droid modified

	private ITouchEventCallback mTouchEventCallback;

	private boolean mRunOnUpdateThread;

	// BEGIN osu!droid modified - MotionEvent may not be retained
	// Volatile since this is read on the UI thread, but applied from other threads.
	private volatile boolean mRetainMotionEvent = true;
	// END osu!droid modified

	private final RunnablePoolUpdateHandler<TouchEventRunnablePoolItem> mTouchEventRunnablePoolUpdateHandler = new RunnablePoolUpdateHandler<TouchEventRunnablePoolItem>() {
		@Override
		protected TouchEventRunnablePoolItem onAllocatePoolItem() {
			return new TouchEventRunnablePoolItem();
		}
	};

	// ===========================================================
	// Constructors
	// ===========================================================

	public BaseTouchController() {

	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public void setTouchEventCallback(final ITouchEventCallback pTouchEventCallback) {
		this.mTouchEventCallback = pTouchEventCallback;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void reset() {
		if(this.mRunOnUpdateThread) {
			this.mTouchEventRunnablePoolUpdateHandler.reset();
		}
	}

	@Override
	public void onUpdate(final float pSecondsElapsed) {
		if(this.mRunOnUpdateThread) {
			this.mTouchEventRunnablePoolUpdateHandler.onUpdate(pSecondsElapsed);
		}
	}

	protected boolean fireTouchEvent(final float pX, final float pY, final int pAction, final int pPointerID, final MotionEvent pMotionEvent) {
		final boolean handled;

		if(this.mRunOnUpdateThread) {
			// BEGIN osu!droid modified - MotionEvent may not be retained
			final MotionEvent motionEvent = this.mRetainMotionEvent ? MotionEvent.obtain(pMotionEvent) : null;
			final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, pMotionEvent.getEventTime(), motionEvent);
			// END osu!droid modified

			final TouchEventRunnablePoolItem touchEventRunnablePoolItem = this.mTouchEventRunnablePoolUpdateHandler.obtainPoolItem();
			touchEventRunnablePoolItem.set(touchEvent);
			this.mTouchEventRunnablePoolUpdateHandler.postPoolItem(touchEventRunnablePoolItem);

			handled = true;
		} else {
			final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, pMotionEvent);
			handled = this.mTouchEventCallback.onTouchEvent(touchEvent);
			touchEvent.recycle();
		}

		return handled;
	}

    // BEGIN osu!droid modified - added method to fire touch events with custom event time
    protected boolean fireTouchEvent(final long pEventTime, final float pX, final float pY, final int pAction, final int pPointerID, final MotionEvent pMotionEvent) {
        final boolean handled;

        if(this.mRunOnUpdateThread) {
            final MotionEvent motionEvent = this.mRetainMotionEvent
                ? MotionEvent.obtain(pMotionEvent.getDownTime(), pEventTime, pMotionEvent.getAction(), pX, pY, pMotionEvent.getMetaState())
                : null;

            final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, pEventTime, motionEvent);

            final TouchEventRunnablePoolItem touchEventRunnablePoolItem = this.mTouchEventRunnablePoolUpdateHandler.obtainPoolItem();
            touchEventRunnablePoolItem.set(touchEvent);
            this.mTouchEventRunnablePoolUpdateHandler.postPoolItem(touchEventRunnablePoolItem);

            handled = true;
        } else {
            final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, pEventTime, pMotionEvent);
            handled = this.mTouchEventCallback.onTouchEvent(touchEvent);
            touchEvent.recycle();
        }

        return handled;
    }
    // END osu!droid modified

	// ===========================================================
	// Methods
	// ===========================================================

	@Override
	public void applyTouchOptions(final TouchOptions pTouchOptions) {
		this.mRunOnUpdateThread = pTouchOptions.isRunOnUpdateThread();
		this.mUseRawPointer = pTouchOptions.isUseRawPointer();
		// BEGIN osu!droid modified - MotionEvent may not be retained
		this.mRetainMotionEvent = pTouchOptions.isRetainMotionEvent();
		// END osu!droid modified
	}

	// BEGIN osu!droid modified - raw pointer getters and setters
	@Override
	public boolean isRawPointerDown(int pointerId) {
		return pointerId >= 0 && pointerId < RAW_POINTER_CAPACITY && this.mRawPointerDown[pointerId];
	}

	@Override
	public boolean getRawPointerSurfacePosition(int pointerId, float[] out) {
		if (pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY) {
			return false;
		}

		final long position = this.mRawPointerSurfacePosition.get(pointerId);

		out[0] = Float.intBitsToFloat((int) (position >>> 32));
		out[1] = Float.intBitsToFloat((int) position);

		return true;
	}

	@Override
	public long getRawPointerEventTime(int pointerId) {
		return pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY ? 0L : this.mRawPointerEventTime[pointerId];
	}

	@Override
	public int getRawPointerCapacity() {
		return RAW_POINTER_CAPACITY;
	}

	@Override
	public void clearRawPointers() {
		if (this.mUseRawPointer) {
			for (int i = 0; i < RAW_POINTER_CAPACITY; ++i) {
				clearRawPointer(i);
			}
		}
	}

	@Override
	public void resetRawPointers() {
		for (int i = 0; i < RAW_POINTER_CAPACITY; ++i) {
			this.mRawPointerSurfacePosition.set(i, packSurfacePosition(0f, 0f));
			this.mRawPointerDown[i] = false;
			this.mRawPointerEventTime[i] = 0L;
		}
	}

	@Override
	public boolean isUseRawPointers() {
		return this.mUseRawPointer;
	}

	protected final void updateRawPointer(final int id, final float x, final float y, final boolean down, final long eventTime) {
		if (!this.mUseRawPointer || id < 0 || id >= RAW_POINTER_CAPACITY) {
			return;
		}

		this.mRawPointerDown[id] = down;
		this.mRawPointerEventTime[id] = eventTime;
		this.mRawPointerSurfacePosition.set(id, packSurfacePosition(x, y));
	}

	protected final void clearRawPointer(final int id) {
		updateRawPointer(id, 0f, 0f, false, 0L);
	}

	private static long packSurfacePosition(final float x, final float y) {
		return ((long) Float.floatToRawIntBits(x) << 32) | (Float.floatToRawIntBits(y) & 0xFFFFFFFFL);
	}
	// END osu!droid modified

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	class TouchEventRunnablePoolItem extends RunnablePoolItem {
		// ===========================================================
		// Fields
		// ===========================================================

		private TouchEvent mTouchEvent;

		// ===========================================================
		// Getter & Setter
		// ===========================================================

		public void set(final TouchEvent pTouchEvent) {
			this.mTouchEvent = pTouchEvent;
		}

		// ===========================================================
		// Methods for/from SuperClass/Interfaces
		// ===========================================================

		@Override
		public void run() {
			BaseTouchController.this.mTouchEventCallback.onTouchEvent(this.mTouchEvent);
		}

		@Override
		protected void onRecycle() {
			super.onRecycle();
			final TouchEvent touchEvent = this.mTouchEvent;
			// BEGIN osu!droid modified - MotionEvent may not be retained
			final MotionEvent motionEvent = touchEvent.getMotionEvent();

			if (motionEvent != null) {
				motionEvent.recycle();
			}
			// END osu!droid modified
			touchEvent.recycle();
		}
	}
}
