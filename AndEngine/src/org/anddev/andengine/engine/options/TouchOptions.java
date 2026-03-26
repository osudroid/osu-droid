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
