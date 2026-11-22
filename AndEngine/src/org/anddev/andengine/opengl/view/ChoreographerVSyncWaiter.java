// osu!droid ONLY
package org.anddev.andengine.opengl.view;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.Choreographer;

import java.util.concurrent.TimeUnit;

/**
 * Paces a render loop to the display's vertical sync using {@link Choreographer} frame callbacks.
 * <p>
 * Once frames carry a presentation time (<code>eglPresentationTimeANDROID</code>), a swap interval
 * of 1 no longer reliably blocks buffer swaps, so this takes over pacing.
 */
final class ChoreographerVSyncWaiter {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * Upper bound for a single wait, so that a callback that never arrives cannot hang the GL thread.
	 */
	private static final long WAIT_TIMEOUT_NANOSECONDS = TimeUnit.MILLISECONDS.toNanos(100);

	// ===========================================================
	// Fields
	// ===========================================================

	private final VSyncSource mSource;
	private final long mTimeoutNanoseconds;

	private final Object mLock = new Object();

	/**
	 * The ID of the latest request, counting up from 1. Guarded by {@link #mLock}.
	 */
	private long mLatestRequest;

	/**
	 * The ID of the latest request that a vertical sync has answered. Guarded by {@link #mLock}.
	 * <p>
	 * A callback reports the latest request it answers rather than merely that it fired. A late callback of a wait
	 * that timed out therefore cannot end the wait for a later request early. A callback that never fires cannot
	 * stall later waits, since the next callback that does fire answers its request as well.
	 */
	private long mAnsweredRequest;

	private final VSyncSource.Callback mCallback = pAnsweredRequest -> {
		synchronized (ChoreographerVSyncWaiter.this.mLock) {
			if (pAnsweredRequest > ChoreographerVSyncWaiter.this.mAnsweredRequest) {
				ChoreographerVSyncWaiter.this.mAnsweredRequest = pAnsweredRequest;
				ChoreographerVSyncWaiter.this.mLock.notifyAll();
			}
		}
	};

	// ===========================================================
	// Constructors
	// ===========================================================

	ChoreographerVSyncWaiter() {
		this(new ChoreographerVSyncSource(), WAIT_TIMEOUT_NANOSECONDS);
	}

	/**
	 * @param pSource The source of the vertical sync callbacks.
	 * @param pTimeoutNanoseconds The upper bound for a single wait.
	 */
	ChoreographerVSyncWaiter(final VSyncSource pSource, final long pTimeoutNanoseconds) {
		this.mSource = pSource;
		this.mTimeoutNanoseconds = pTimeoutNanoseconds;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Waits for the vertical sync callback requested during the previous call, then requests the next one.
	 * <p>
	 * Because the callback is requested a frame in advance, waiting for vertical sync overlaps with processing the
	 * frame instead of following it.
	 * <p>
	 * This **must** be called from the GL thread.
	 */
	void waitForVSync() throws InterruptedException {
		final long request;

		synchronized (this.mLock) {
			// On the first call, no request has been made yet, so there is nothing to wait for.
			final long deadline = System.nanoTime() + this.mTimeoutNanoseconds;
			long remaining = this.mTimeoutNanoseconds;

			while (this.mAnsweredRequest < this.mLatestRequest && remaining > 0) {
				TimeUnit.NANOSECONDS.timedWait(this.mLock, remaining);
				remaining = deadline - System.nanoTime();
			}

			request = ++this.mLatestRequest;
		}

		this.mSource.requestVSync(this.mCallback, request);
	}

	void quit() {
		this.mSource.quit();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Asynchronously reports the display's vertical syncs.
	 */
	interface VSyncSource {
		interface Callback {
			/**
			 * @param pAnsweredRequest The id of the latest request this vertical sync answers. This may be later than
			 *                         the request the callback was made for, and is never earlier.
			 */
			void onVSync(long pAnsweredRequest);
		}

		/**
		 * Requests a callback on the next vertical sync.
		 *
		 * @param pCallback The callback to call.
		 * @param pRequest  The id of this request, which is larger than the id of every earlier request.
		 */
		void requestVSync(Callback pCallback, long pRequest);

		/**
		 * Releases the resources of this source. No request is answered afterwards.
		 */
		void quit();
	}

	/**
	 * A {@link VSyncSource} backed by {@link Choreographer} frame callbacks, which run on a looper thread of its own.
	 */
	static final class ChoreographerVSyncSource implements VSyncSource {
		private final HandlerThread mThread;
		private final Handler mHandler;

		/**
		 * The callback of the latest request. The waiter always passes the same one, so one field covers every
		 * pending request without allocating per frame.
		 */
		private volatile Callback mCallback;

		/**
		 * The id of the latest request, written by the waiting thread.
		 */
		private volatile long mLatestRequest;

		/**
		 * The id of the latest request that a frame callback has been posted for. Only accessed on {@link #mThread}.
		 */
		private long mPostedRequest;

		/**
		 * Posting frame callbacks and dispatching a frame both run on {@link #mThread}, and a frame runs every frame
		 * callback posted before it. So when a frame callback fires, every request up to {@link #mPostedRequest} is
		 * answered by this frame, even if the frame callback of one of them was lost.
		 */
		private final Choreographer.FrameCallback mFrameCallback = pFrameTimeNanos ->
			ChoreographerVSyncSource.this.mCallback.onVSync(ChoreographerVSyncSource.this.mPostedRequest);

		private final Runnable mPostFrameCallback = () -> {
			// Read here rather than passed along with each post, which needs no allocation per request, and a post
			// still queued behind a newer request covers that one as well.
			// This must be set on mThread, otherwise a late frame callback could report a request whose own frame
			// callback has not been posted yet, and end the wait for that request early.
			ChoreographerVSyncSource.this.mPostedRequest = ChoreographerVSyncSource.this.mLatestRequest;

			// Runs on mThread, so this is that thread's Choreographer and the callback fires on it.
			Choreographer.getInstance().postFrameCallback(ChoreographerVSyncSource.this.mFrameCallback);
		};

		ChoreographerVSyncSource() {
			this.mThread = new HandlerThread("ChoreographerVSync");
			this.mThread.start();
			this.mHandler = new Handler(this.mThread.getLooper());
		}

		@Override
		public void requestVSync(final Callback pCallback, final long pRequest) {
			this.mCallback = pCallback;
			this.mLatestRequest = pRequest;
			this.mHandler.post(this.mPostFrameCallback);
		}

		@Override
		public void quit() {
			this.mThread.quitSafely();
		}
	}
}
