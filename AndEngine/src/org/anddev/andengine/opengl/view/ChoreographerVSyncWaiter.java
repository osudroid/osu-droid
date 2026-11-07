// osu!droid ONLY
package org.anddev.andengine.opengl.view;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Choreographer;

/**
 * Paces a render loop to the display's vertical sync using {@link Choreographer} frame callbacks.
 * <p>
 * Once frames carry a presentation time (<code>eglPresentationTimeANDROID</code>), a swap interval
 * of 1 no longer reliably blocks buffer swaps, so this takes over pacing.
 * <p>
 * {@link #waitForVSync()} must only be called from a single thread (the GL thread).
 */
final class ChoreographerVSyncWaiter {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * Upper bound for a single wait, so that a callback that never arrives cannot hang the GL thread.
	 */
	private static final long WAIT_TIMEOUT_MILLISECONDS = 100;

	// ===========================================================
	// Fields
	// ===========================================================

	private final HandlerThread mThread;
	private final Handler mHandler;

	private final Object mLock = new Object();

	/**
	 * Whether the frame callback posted last has fired. Guarded by {@link #mLock}.
	 */
	private boolean mVSyncSignaled;

	/**
	 * Whether a frame callback has been posted yet. Only accessed by the waiting thread.
	 */
	private boolean mHasPostedCallback;

	private final Choreographer.FrameCallback mFrameCallback = pFrameTimeNanos -> {
        synchronized (ChoreographerVSyncWaiter.this.mLock) {
            ChoreographerVSyncWaiter.this.mVSyncSignaled = true;
            ChoreographerVSyncWaiter.this.mLock.notifyAll();
        }
    };

	private final Runnable mPostFrameCallback = () -> {
        // Runs on mThread, so this is that thread's Choreographer and the callback fires on it.
        Choreographer.getInstance().postFrameCallback(ChoreographerVSyncWaiter.this.mFrameCallback);
    };

	// ===========================================================
	// Constructors
	// ===========================================================

	ChoreographerVSyncWaiter() {
		this.mThread = new HandlerThread("ChoreographerVSync");
		this.mThread.start();
		this.mHandler = new Handler(this.mThread.getLooper());
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Waits for the vertical sync callback posted during the previous call, then posts the next one.
	 * <p>
	 * Because the callback is posted a frame in advance, waiting for vertical sync overlaps with processing the frame
	 * instead of following it.
	 */
	void waitForVSync() throws InterruptedException {
		synchronized (this.mLock) {
			// On the first call, no callback has been posted yet, so there is nothing to wait for.
			if (this.mHasPostedCallback) {
				final long deadline = SystemClock.uptimeMillis() + WAIT_TIMEOUT_MILLISECONDS;
				long remaining;

				while (!this.mVSyncSignaled && (remaining = deadline - SystemClock.uptimeMillis()) > 0) {
					this.mLock.wait(remaining);
				}
			}

			this.mVSyncSignaled = false;
		}

		this.mHandler.post(this.mPostFrameCallback);
		this.mHasPostedCallback = true;
	}

	void quit() {
		this.mThread.quitSafely();
	}
}
