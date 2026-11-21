package org.andengine.input.touch;

import org.andengine.util.adt.pool.GenericPool;

import android.view.MotionEvent;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 10:17:42 - 13.07.2010
 */
public class TouchEvent {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int INVALID_POINTER_ID = -1;

	public static final int ACTION_CANCEL = MotionEvent.ACTION_CANCEL;
	public static final int ACTION_DOWN = MotionEvent.ACTION_DOWN;
	public static final int ACTION_MOVE = MotionEvent.ACTION_MOVE;
	public static final int ACTION_OUTSIDE = MotionEvent.ACTION_OUTSIDE;
	public static final int ACTION_UP = MotionEvent.ACTION_UP;

	private static final TouchEventPool TOUCHEVENT_POOL = new TouchEventPool();

	// ===========================================================
	// Fields
	// ===========================================================

	protected int mPointerID;

	protected float mX;
	protected float mY;

	protected int mAction;

	protected MotionEvent mMotionEvent;

	// BEGIN osu!droid modified - carry the event time and surface coordinates
	// without needing a MotionEvent
	protected long mEventTime;

	protected float mSurfaceX;
	protected float mSurfaceY;
	// END osu!droid modified

	// ===========================================================
	// Constructors
	// ===========================================================

	public static TouchEvent obtain(final float pX, final float pY, final int pAction, final int pPointerID, final MotionEvent pMotionEvent) {
		// BEGIN osu!droid modified - carry the event time without needing a MotionEvent
		return obtain(pX, pY, pAction, pPointerID, pMotionEvent != null ? pMotionEvent.getEventTime() : 0, pMotionEvent);
		// END osu!droid modified
	}

	// BEGIN osu!droid modified - carry the event time without needing a MotionEvent
	public static TouchEvent obtain(final float pX, final float pY, final int pAction, final int pPointerID, final long pEventTime, final MotionEvent pMotionEvent) {
		final TouchEvent touchEvent = TOUCHEVENT_POOL.obtainPoolItem();
		touchEvent.set(pX, pY, pAction, pPointerID, pEventTime, pMotionEvent);
		return touchEvent;
	}

	private void set(final float pX, final float pY, final int pAction, final int pPointerID, final long pEventTime, final MotionEvent pMotionEvent) {
		this.mX = pX;
		this.mY = pY;
		this.mSurfaceX = pX;
		this.mSurfaceY = pY;
		this.mAction = pAction;
		this.mPointerID = pPointerID;
		this.mEventTime = pEventTime;
		this.mMotionEvent = pMotionEvent;
	}
	// END osu!droid modified

	public void recycle() {
		TOUCHEVENT_POOL.recyclePoolItem(this);
	}

	public static void recycle(final TouchEvent pTouchEvent) {
		TOUCHEVENT_POOL.recyclePoolItem(pTouchEvent);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public float getX() {
		return this.mX;
	}

	public float getY() {
		return this.mY;
	}

	public void set(final float pX, final float pY) {
		this.mX = pX;
		this.mY = pY;
	}

	public void offset(final float pDeltaX, final float pDeltaY) {
		this.mX += pDeltaX;
		this.mY += pDeltaY;
	}

	// BEGIN osu!droid modified - carry the event time without needing a MotionEvent
	/**
	 * The time at which this {@link TouchEvent} occurred, in the {@link android.os.SystemClock#uptimeMillis()} time base.
	 * <p>
	 * Unlike {@link #getMotionEvent()}, this is always available.
	 */
	public long getEventTime() {
		return this.mEventTime;
	}

	/**
	 * The X coordinate this {@link TouchEvent} was obtained with, in surface coordinates, before any conversion such as
	 * {@link #set(float, float)} or {@link #offset(float, float)}.
	 */
	public float getSurfaceX() {
		return this.mSurfaceX;
	}

	/**
	 * The Y coordinate this {@link TouchEvent} was obtained with, in surface coordinates, before any conversion such as
	 * {@link #set(float, float)} or {@link #offset(float, float)}.
	 */
	public float getSurfaceY() {
		return this.mSurfaceY;
	}
	// END osu!droid modified

	public int getPointerID() {
		return this.mPointerID;
	}

	public int getAction() {
		return this.mAction;
	}

	public boolean isActionDown() {
		return this.mAction == TouchEvent.ACTION_DOWN;
	}

	public boolean isActionUp() {
		return this.mAction == TouchEvent.ACTION_UP;
	}

	public boolean isActionMove() {
		return this.mAction == TouchEvent.ACTION_MOVE;
	}

	public boolean isActionCancel() {
		return this.mAction == TouchEvent.ACTION_CANCEL;
	}

	public boolean isActionOutside() {
		return this.mAction == TouchEvent.ACTION_OUTSIDE;
	}

	/**
	 * Provides the raw {@link MotionEvent} that originally caused this {@link TouchEvent}.
	 * The coordinates of this {@link MotionEvent} are in surface-coordinates!
	 * <p>
	 * BEGIN osu!droid modified - MotionEvent may not be retained
	 * This is <code>null</code> for {@link TouchEvent}s handled on the update thread when
	 * {@link org.andengine.engine.options.TouchOptions#isRetainMotionEvent()} is disabled.
	 * END osu!droid modified
	 * @return
	 */
	public MotionEvent getMotionEvent() {
		return this.mMotionEvent;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	private static final class TouchEventPool extends GenericPool<TouchEvent> {
		// ===========================================================
		// Methods for/from SuperClass/Interfaces
		// ===========================================================

		@Override
		protected TouchEvent onAllocatePoolItem() {
			return new TouchEvent();
		}
	}
}
