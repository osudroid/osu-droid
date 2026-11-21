package org.anddev.andengine.engine.options;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 16:03:09 - 08.09.2010
 */
public class TouchOptions {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean mRunOnUpdateThread;

    // BEGIN osu!droid modified - Option for historical events processing and raw pointer
    private boolean mProcessHistoricalEvents;
    private boolean mUseRawPointer;
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

	public TouchOptions enableRunOnUpdateThread() {
		return this.setRunOnUpdateThread(true);
	}

	public TouchOptions disableRunOnUpdateThread() {
		return this.setRunOnUpdateThread(false);
	}

	public TouchOptions setRunOnUpdateThread(final boolean pRunOnUpdateThread) {
		this.mRunOnUpdateThread = pRunOnUpdateThread;
		return this;
	}

	/**
	 * <u><b>Default:</b></u> <code>true</code>
	 */
	public boolean isRunOnUpdateThread() {
		return this.mRunOnUpdateThread;
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
     * Sets whether a {@link org.anddev.andengine.input.touch.TouchEvent} that is handled on the update thread
     * retains a copy of the {@link android.view.MotionEvent} it came from.
     * <p>
     * Copying allocates a {@link android.view.MotionEvent} per touch event once Android's pool of them runs out.
     * When disabled, {@link org.anddev.andengine.input.touch.TouchEvent#getMotionEvent()} returns <code>null</code>
     * for such events, so only disable this when nothing handling them needs it.
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
