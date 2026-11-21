package org.andengine.input.touch.controller;

import org.andengine.input.touch.TouchEvent;
import org.andengine.engine.options.TouchOptions;
import org.andengine.util.adt.pool.RunnablePoolItem;
import org.andengine.util.adt.pool.RunnablePoolUpdateHandler;

import android.view.MotionEvent;

import java.util.concurrent.atomic.AtomicIntegerArray;
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
	//							  processHistoricalEvents flag for accurate MOVE tracking
	private static final int RAW_POINTER_CAPACITY = 10;

	// volatile so that changes from applyTouchOptions() (game-update thread) are
	// immediately visible to onHandleMotionEvent() (UI thread).
	private volatile boolean mUseRawPointer;
	private volatile boolean mProcessHistoricalEvents;

	// The surface X and Y coordinates of each pointer, packed into one long (X in the high 32 bits,
	// Y in the low 32 bits) so that both are always read together.
	private final AtomicLongArray    mRawPointerSurfacePosition = new AtomicLongArray(RAW_POINTER_CAPACITY);
	private final AtomicIntegerArray mRawPointerDown = new AtomicIntegerArray(RAW_POINTER_CAPACITY);
	private final AtomicLongArray    mRawPointerEventTime = new AtomicLongArray(RAW_POINTER_CAPACITY);
	// END osu!droid modified

	// BEGIN osu!droid modified - MotionEvent may not be retained
	// Volatile since this is read on the UI thread, but applied from other threads.
	private volatile boolean mRetainMotionEvent = true;
	// END osu!droid modified

	private ITouchEventCallback mTouchEventCallback;

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
		this.mTouchEventRunnablePoolUpdateHandler.reset();
	}

	@Override
	public void onUpdate(final float pSecondsElapsed) {
		this.mTouchEventRunnablePoolUpdateHandler.onUpdate(pSecondsElapsed);
	}

	protected void fireTouchEvent(final float pX, final float pY, final int pAction, final int pPointerID, final MotionEvent pMotionEvent) {
		// BEGIN osu!droid modified - MotionEvent may not be retained
		final MotionEvent motionEvent = this.mRetainMotionEvent ? MotionEvent.obtain(pMotionEvent) : null;
		final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, pMotionEvent.getEventTime(), motionEvent);
		// END osu!droid modified

		final TouchEventRunnablePoolItem touchEventRunnablePoolItem = this.mTouchEventRunnablePoolUpdateHandler.obtainPoolItem();
		touchEventRunnablePoolItem.set(touchEvent);
		this.mTouchEventRunnablePoolUpdateHandler.postPoolItem(touchEventRunnablePoolItem);
	}

	// BEGIN osu!droid modified - fire touch event with a custom event time (used for historical positions)
	protected void fireTouchEvent(final long pEventTime, final float pX, final float pY, final int pAction, final int pPointerID, final MotionEvent pMotionEvent) {
		final MotionEvent syntheticEvent = this.mRetainMotionEvent
				? MotionEvent.obtain(pMotionEvent.getDownTime(), pEventTime, pMotionEvent.getAction(), pX, pY, pMotionEvent.getMetaState())
				: null;
		final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, pEventTime, syntheticEvent);

		final TouchEventRunnablePoolItem touchEventRunnablePoolItem = this.mTouchEventRunnablePoolUpdateHandler.obtainPoolItem();
		touchEventRunnablePoolItem.set(touchEvent);
		this.mTouchEventRunnablePoolUpdateHandler.postPoolItem(touchEventRunnablePoolItem);
	}
	// END osu!droid modified

	// ===========================================================
	// Methods
	// ===========================================================

	// BEGIN osu!droid modified - raw pointer getters and setters
	/*
	 * Concurrency note for raw pointer reads:
	 *
	 * Writes happen on the UI thread (in updateRawPointer); reads happen on the engine update thread.
	 * The X and Y coordinates are packed into one atomic long, so getRawPointerSurfacePosition always
	 * returns a coherent position. The other getters return the latest value of their single field.
	 */
	@Override
	public boolean isRawPointerDown(int pointerId) {
		return pointerId >= 0 && pointerId < RAW_POINTER_CAPACITY && this.mRawPointerDown.get(pointerId) != 0;
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
		return pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY ? 0L : this.mRawPointerEventTime.get(pointerId);
	}

	@Override
	public int getRawPointerCapacity() {
		return RAW_POINTER_CAPACITY;
	}

	/**
	 * Clear all tracked raw pointer state. No-op when raw pointer tracking is disabled,
	 * to avoid unnecessary atomic increments at runtime.
	 */
	@Override
	public void clearRawPointers() {
		if (this.mUseRawPointer) {
			for (int i = 0; i < RAW_POINTER_CAPACITY; ++i) {
				clearRawPointer(i);
			}
		}
	}

	/**
	 * Hard-reset all raw pointer state. Always runs,
	 * regardless of {@link #mUseRawPointer}, so it can be used to wipe stale state when
	 * switching modes (e.g. on map start).
	 */
	@Override
	public void resetRawPointers() {
		for (int i = 0; i < RAW_POINTER_CAPACITY; ++i) {
			this.mRawPointerSurfacePosition.set(i, packSurfacePosition(0f, 0f));
			this.mRawPointerDown.set(i, 0);
			this.mRawPointerEventTime.set(i, 0L);
		}
	}

	@Override
	public boolean isUseRawPointer() {
		return this.mUseRawPointer;
	}

	public void setUseRawPointer(boolean useRawPointer) {
		this.mUseRawPointer = useRawPointer;
	}

	public boolean isProcessHistoricalEvents() {
		return this.mProcessHistoricalEvents;
	}

	public void setProcessHistoricalEvents(boolean processHistoricalEvents) {
		this.mProcessHistoricalEvents = processHistoricalEvents;
	}

	@Override
	public void applyTouchOptions(final TouchOptions pTouchOptions) {
		this.setUseRawPointer(pTouchOptions.isUseRawPointer());
		this.setProcessHistoricalEvents(pTouchOptions.isProcessHistoricalEvents());
		// BEGIN osu!droid modified - MotionEvent may not be retained
		this.mRetainMotionEvent = pTouchOptions.isRetainMotionEvent();
		// END osu!droid modified
	}

	protected final void updateRawPointer(final int id, final float x, final float y, final boolean down, final long eventTime) {
		if (!this.mUseRawPointer || id < 0 || id >= RAW_POINTER_CAPACITY) {
			return;
		}
		this.mRawPointerDown.set(id, down ? 1 : 0);
		this.mRawPointerEventTime.set(id, eventTime);
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
