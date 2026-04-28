package org.andengine.input.touch.controller;

import org.andengine.input.touch.TouchEvent;
import org.andengine.engine.options.TouchOptions;
import org.andengine.util.adt.pool.RunnablePoolItem;
import org.andengine.util.adt.pool.RunnablePoolUpdateHandler;

import android.view.MotionEvent;

import java.util.concurrent.atomic.AtomicIntegerArray;

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

	private boolean mUseRawPointer;
	private boolean mProcessHistoricalEvents;
	private final float[] mRawPointerX = new float[RAW_POINTER_CAPACITY];
	private final float[] mRawPointerY = new float[RAW_POINTER_CAPACITY];
	private final boolean[] mRawPointerDown = new boolean[RAW_POINTER_CAPACITY];
	private final long[] mRawPointerEventTime = new long[RAW_POINTER_CAPACITY];
	private final AtomicIntegerArray mRawPointerVersion = new AtomicIntegerArray(RAW_POINTER_CAPACITY);
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
		final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, MotionEvent.obtain(pMotionEvent));

		final TouchEventRunnablePoolItem touchEventRunnablePoolItem = this.mTouchEventRunnablePoolUpdateHandler.obtainPoolItem();
		touchEventRunnablePoolItem.set(touchEvent);
		this.mTouchEventRunnablePoolUpdateHandler.postPoolItem(touchEventRunnablePoolItem);
	}

	// BEGIN osu!droid modified - fire touch event with a custom event time (used for historical positions)
	protected void fireTouchEvent(final long pEventTime, final float pX, final float pY, final int pAction, final int pPointerID, final MotionEvent pMotionEvent) {
		final MotionEvent syntheticEvent = MotionEvent.obtain(
				pMotionEvent.getDownTime(), pEventTime, pMotionEvent.getAction(),
				pX, pY, pMotionEvent.getMetaState());
		final TouchEvent touchEvent = TouchEvent.obtain(pX, pY, pAction, pPointerID, syntheticEvent);

		final TouchEventRunnablePoolItem touchEventRunnablePoolItem = this.mTouchEventRunnablePoolUpdateHandler.obtainPoolItem();
		touchEventRunnablePoolItem.set(touchEvent);
		this.mTouchEventRunnablePoolUpdateHandler.postPoolItem(touchEventRunnablePoolItem);
	}
	// END osu!droid modified

	// ===========================================================
	// Methods
	// ===========================================================

	// BEGIN osu!droid modified - raw pointer getters and setters
	/**
	 * Concurrency note for raw pointer reads:
	 * <p>
	 * Writes happen on the UI thread (in {@link #updateRawPointer}); reads happen
	 * on the engine update thread. The per-field getters below ({@link #getRawPointerSurfaceX},
	 * {@link #getRawPointerSurfaceY}, {@link #getRawPointerEventTime}, etc.) return the
	 * latest value of a single field but provide <b>no cross-field coherence</b> &mdash; a
	 * reader may observe X from one event and Y from the next.
	 * <p>
	 * For a coherent snapshot of multiple fields, callers should perform a seqlock-style
	 * read using {@link #getRawPointerVersion}:
	 * <pre>
	 * int v1, v2;
	 * float x, y;
	 * do {
	 *     v1 = controller.getRawPointerVersion(id);
	 *     if ((v1 &amp; 1) != 0) continue;        // writer in progress (odd version)
	 *     x = controller.getRawPointerSurfaceX(id);
	 *     y = controller.getRawPointerSurfaceY(id);
	 *     v2 = controller.getRawPointerVersion(id);
	 * } while (v1 != v2);
	 * </pre>
	 * For rhythm-game gameplay where a one-frame torn read is harmless, the per-field
	 * getters are fine to use directly.
	 */
	@Override
	public int getRawPointerVersion(int pointerId) {
		return pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY ? -1 : this.mRawPointerVersion.get(pointerId);
	}

	@Override
	public boolean isRawPointerDown(int pointerId) {
		return pointerId >= 0 && pointerId < RAW_POINTER_CAPACITY && this.mRawPointerDown[pointerId];
	}

	@Override
	public float getRawPointerSurfaceX(int pointerId) {
		return pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY ? 0f : this.mRawPointerX[pointerId];
	}

	@Override
	public float getRawPointerSurfaceY(int pointerId) {
		return pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY ? 0f : this.mRawPointerY[pointerId];
	}

	@Override
	public long getRawPointerEventTime(int pointerId) {
		return pointerId < 0 || pointerId >= RAW_POINTER_CAPACITY ? 0L : this.mRawPointerEventTime[pointerId];
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
	 * Hard-reset all raw pointer state, including the version counters. Always runs,
	 * regardless of {@link #mUseRawPointer}, so it can be used to wipe stale state when
	 * switching modes (e.g. on map start).
	 */
	@Override
	public void resetRawPointers() {
		for (int i = 0; i < RAW_POINTER_CAPACITY; ++i) {
			this.mRawPointerVersion.set(i, 0);
			this.mRawPointerX[i] = 0f;
			this.mRawPointerY[i] = 0f;
			this.mRawPointerDown[i] = false;
			this.mRawPointerEventTime[i] = 0L;
		}
	}

	@Override
	public boolean isUseRawPointers() {
		return this.mUseRawPointer;
	}

	public void setUseRawPointers(boolean useRawPointers) {
		this.mUseRawPointer = useRawPointers;
	}

	public boolean isProcessHistoricalEvents() {
		return this.mProcessHistoricalEvents;
	}

	public void setProcessHistoricalEvents(boolean processHistoricalEvents) {
		this.mProcessHistoricalEvents = processHistoricalEvents;
	}

	@Override
	public void applyTouchOptions(final TouchOptions pTouchOptions) {
		this.setUseRawPointers(pTouchOptions.isUseRawPointer());
		this.setProcessHistoricalEvents(pTouchOptions.isProcessHistoricalEvents());
	}

	protected final void updateRawPointer(final int id, final float x, final float y, final boolean down, final long eventTime) {
		if (!this.mUseRawPointer || id < 0 || id >= RAW_POINTER_CAPACITY) {
			return;
		}
		this.mRawPointerVersion.incrementAndGet(id);
		this.mRawPointerX[id] = x;
		this.mRawPointerY[id] = y;
		this.mRawPointerDown[id] = down;
		this.mRawPointerEventTime[id] = eventTime;
		this.mRawPointerVersion.incrementAndGet(id);
	}

	protected final void clearRawPointer(final int id) {
		updateRawPointer(id, 0f, 0f, false, 0L);
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
			touchEvent.getMotionEvent().recycle();
			touchEvent.recycle();
		}
	}
}
