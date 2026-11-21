package org.andengine.engine.options;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 23:18:06 - 22.11.2011
 */
public class TouchOptions {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long TOUCHEVENT_INTERVAL_MILLISECONDS_DEFAULT = 20;

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean mNeedsMultiTouch;
	private long mTouchEventIntervalMilliseconds = TouchOptions.TOUCHEVENT_INTERVAL_MILLISECONDS_DEFAULT;

	// BEGIN osu!droid modified - Option for historical events processing and raw pointer
	/** When true, historical (batched) touch positions inside a MOVE event are each fired as separate events. */
	private boolean mProcessHistoricalEvents = false;
	/** When true, raw pointer state is tracked immediately on the UI thread for low-latency reads. */
	private boolean mUseRawPointer = false;
	// END osu!droid modified
	// BEGIN osu!droid modified - Option for not retaining a copy of each MotionEvent
	private boolean mRetainMotionEvent = true;
	// END osu!droid modified

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean needsMultiTouch() {
		return this.mNeedsMultiTouch;
	}

	public TouchOptions setNeedsMultiTouch(final boolean pNeedsMultiTouch) {
		this.mNeedsMultiTouch = pNeedsMultiTouch;
		return this;
	}

	public long getTouchEventIntervalMilliseconds() {
		return this.mTouchEventIntervalMilliseconds;
	}

	public void setTouchEventIntervalMilliseconds(final long pTouchEventIntervalMilliseconds) {
		this.mTouchEventIntervalMilliseconds = pTouchEventIntervalMilliseconds;
	}

	// BEGIN osu!droid modified - Option for historical events processing and raw pointer
	public TouchOptions enableProcessHistoricalEvents() {
		return this.setProcessHistoricalEvents(true);
	}

	public TouchOptions disableProcessHistoricalEvents() {
		return this.setProcessHistoricalEvents(false);
	}

	public TouchOptions setProcessHistoricalEvents(final boolean pProcessHistoricalEvents) {
		this.mProcessHistoricalEvents = pProcessHistoricalEvents;
		return this;
	}

	/**
	 * <u><b>Default:</b></u> <code>false</code>
	 */
	public boolean isProcessHistoricalEvents() {
		return this.mProcessHistoricalEvents;
	}

	public TouchOptions enableUseRawPointer() {
		return this.setUseRawPointer(true);
	}

	public TouchOptions disableUseRawPointer() {
		return this.setUseRawPointer(false);
	}

	public TouchOptions setUseRawPointer(final boolean pUseRawPointer) {
		this.mUseRawPointer = pUseRawPointer;
		return this;
	}

	/**
	 * <u><b>Default:</b></u> <code>false</code>
	 */
	public boolean isUseRawPointer() {
		return this.mUseRawPointer;
	}
	// END osu!droid modified

	// BEGIN osu!droid modified - Option for not retaining a copy of each MotionEvent
	public TouchOptions enableRetainMotionEvent() {
		return this.setRetainMotionEvent(true);
	}

	public TouchOptions disableRetainMotionEvent() {
		return this.setRetainMotionEvent(false);
	}

	/**
	 * Sets whether a {@link org.andengine.input.touch.TouchEvent} retains a copy of the
	 * {@link android.view.MotionEvent} it came from.
	 * <p>
	 * Copying allocates a {@link android.view.MotionEvent} per touch event once the pool of Android runs out.
	 * When disabled, {@link org.andengine.input.touch.TouchEvent#getMotionEvent()} returns <code>null</code>,
	 * so only disable this when nothing handling the events needs it.
	 */
	public TouchOptions setRetainMotionEvent(final boolean pRetainMotionEvent) {
		this.mRetainMotionEvent = pRetainMotionEvent;
		return this;
	}

	/**
	 * <u><b>Default:</b></u> <code>true</code>
	 */
	public boolean isRetainMotionEvent() {
		return this.mRetainMotionEvent;
	}
	// END osu!droid modified

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
