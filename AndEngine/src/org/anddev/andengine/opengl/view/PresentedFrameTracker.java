// osu!droid ONLY
package org.anddev.andengine.opengl.view;

import org.anddev.andengine.util.annotation.GLThread;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Tracks which swapped frames reach the display, using a {@link FrameTimestampSource}.
 */
public final class PresentedFrameTracker {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * The maximum number of swapped frames awaiting a result.
	 */
	public static final int MAX_PENDING_FRAMES = 16;

	/**
	 * How long tracking may go without a presented frame before it gives up.
	 */
	public static final long NO_PRESENT_TIMEOUT_NANOS = 1_000_000_000L;

	// ===========================================================
	// Fields
	// ===========================================================

	private final FrameTimestampSource mSource;
	private final LongSupplier mNanoTime;

	private volatile boolean mRequestedEnabled;
	private final AtomicInteger mResetGeneration = new AtomicInteger();

	@GLThread
	private boolean mAppliedEnabled;
	@GLThread
	private int mAppliedResetGeneration;
	@GLThread
	private boolean mSurfaceExists;
	@GLThread
	private boolean mSurfaceChanged;

	@GLThread
	private boolean mActive;
	@GLThread
	private boolean mAvailable;
	@GLThread
	private boolean mWasReset;
	@GLThread
	private long mLastPresentNanos;

	@GLThread
	private final long[] mPendingFrameIDs = new long[MAX_PENDING_FRAMES];
	@GLThread
	private int mPendingCount;
	@GLThread
	private final long[] mResults = new long[MAX_PENDING_FRAMES];

	@GLThread
	private final long[] mPresentTimes = new long[PresentedFrameBatch.CAPACITY];
	@GLThread
	private int mPresentTimeCount;

	@GLThread
	private int mSwapped;
	@GLThread
	private int mPresented;
	@GLThread
	private int mDropped;
	@GLThread
	private int mUnknown;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param pSource   The source of frame present times.
	 * @param pNanoTime The clock used to detect that no frame is being presented.
	 */
	public PresentedFrameTracker(final FrameTimestampSource pSource, final LongSupplier pNanoTime) {
		this.mSource = pSource;
		this.mNanoTime = pNanoTime;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Requests tracking to be turned on or off.
	 * <p>
	 * The GL thread applies the latest request on its next frame.
	 */
	public void setEnabled(final boolean pEnabled) {
		this.mRequestedEnabled = pEnabled;
	}

	/**
	 * Requests tracking to start over, which also retries after tracking gave up.
	 * <p>
	 * The GL thread applies it on its next frame.
	 */
	public void reset() {
		this.mResetGeneration.incrementAndGet();
	}

	/**
	 * Notes that a new surface is current.
	 */
	@GLThread
	void onSurfaceCreated() {
		this.mSurfaceExists = true;
		this.mSurfaceChanged = true;
	}

	/**
	 * Stops tracking without touching the source, as the surface it works on is going away.
	 */
	@GLThread
	void onSurfaceDestroyed() {
		this.mSurfaceExists = false;
		this.deactivate();
	}

	/**
	 * Applies the latest requests and collects the results of earlier frames.
	 * <p>
	 * This must be called just before each buffer swap, as it gets the ID of the frame that the swap submits.
	 */
	@GLThread
	void onBeforeSwap() {
		this.applyRequests();

		if (!this.mAppliedEnabled) {
			return;
		}

		this.mSwapped++;

		if (!this.mActive) {
			return;
		}

		final long nextFrameID = this.mSource.queryAndGetNextFrameId(this.mPendingFrameIDs, this.mPendingCount, this.mResults);
		final boolean presented = this.collectResults();

		if (nextFrameID != FrameTimestampSource.NO_FRAME_ID) {
			if (this.mPendingCount == MAX_PENDING_FRAMES) {
				removeFirst(this.mPendingFrameIDs, MAX_PENDING_FRAMES);
				this.mPendingCount--;
				this.mUnknown++;
			}

			this.mPendingFrameIDs[this.mPendingCount++] = nextFrameID;
		}

		if (!presented && this.mNanoTime.getAsLong() - this.mLastPresentNanos > NO_PRESENT_TIMEOUT_NANOS) {
			this.mSource.stop();
			this.deactivate();
		}
	}

	/**
	 * Copies the presented frames gathered since the last call into the given batch, then clears them.
	 * <p>
	 * Only called during the draw phase.
	 */
	@GLThread
	public void drainInto(final PresentedFrameBatch pOut) {
		System.arraycopy(this.mPresentTimes, 0, pOut.presentTimes, 0, this.mPresentTimeCount);
		pOut.count = this.mPresentTimeCount;
		pOut.isAvailable = this.mAvailable;
		pOut.wasReset = this.mWasReset;

		this.mPresentTimeCount = 0;
		this.mWasReset = false;
	}

	/**
	 * Copies the frame outcomes counted since the last call into the given stats, then clears them.
	 */
	@GLThread
	public void drainStatsInto(final PresentedFrameStats pOut) {
		pOut.swapped = this.mSwapped;
		pOut.presented = this.mPresented;
		pOut.dropped = this.mDropped;
		pOut.unknown = this.mUnknown;

		this.mSwapped = 0;
		this.mPresented = 0;
		this.mDropped = 0;
		this.mUnknown = 0;
	}

	/**
	 * Whether presented frames are being tracked and have been seen since tracking was last reset.
	 */
	@GLThread
	boolean isAvailable() {
		return this.mAvailable;
	}

	/**
	 * Whether the source is running and its results are being collected.
	 */
	@GLThread
	boolean isActive() {
		return this.mActive;
	}

	@GLThread
	private void applyRequests() {
		final boolean enabled = this.mRequestedEnabled;
		final int resetGeneration = this.mResetGeneration.get();

		if (enabled == this.mAppliedEnabled && resetGeneration == this.mAppliedResetGeneration && !this.mSurfaceChanged) {
			return;
		}

		this.mAppliedEnabled = enabled;
		this.mAppliedResetGeneration = resetGeneration;
		this.mSurfaceChanged = false;

		if (this.mActive && this.mSurfaceExists) {
			this.mSource.stop();
		}

		this.deactivate();

		if (!enabled || !this.mSurfaceExists) {
			return;
		}

		this.mActive = this.mSource.start();
		this.mLastPresentNanos = this.mNanoTime.getAsLong();
		this.mWasReset = true;
	}

	/**
	 * Takes the results of the pending frames, keeping the frames that are still pending.
	 *
	 * @return Whether any frame was presented.
	 */
	@GLThread
	private boolean collectResults() {
		final int count = this.mPendingCount;
		int remaining = 0;
		boolean presented = false;

		for (int i = 0; i < count; i++) {
			final long result = this.mResults[i];

			if (result >= 0) {
				this.addPresentTime(result);
				presented = true;
			} else if (result == FrameTimestampSource.RESULT_PENDING) {
				this.mPendingFrameIDs[remaining++] = this.mPendingFrameIDs[i];
			} else if (result == FrameTimestampSource.RESULT_DROPPED) {
				this.mDropped++;
			} else {
				this.mUnknown++;
			}
		}

		this.mPendingCount = remaining;

		if (presented) {
			this.mAvailable = true;
			this.mLastPresentNanos = this.mNanoTime.getAsLong();
		}

		return presented;
	}

	@GLThread
	private void addPresentTime(final long pPresentTime) {
		if (this.mPresentTimeCount == PresentedFrameBatch.CAPACITY) {
			removeFirst(this.mPresentTimes, PresentedFrameBatch.CAPACITY);
			this.mPresentTimeCount--;
			this.mWasReset = true;
		}

		this.mPresentTimes[this.mPresentTimeCount++] = pPresentTime;
		this.mPresented++;
	}

	/**
	 * Removes the first of the first <code>pCount</code> elements of an array, moving the others down by one.
	 */
	private static void removeFirst(final long[] pArray, final int pCount) {
		// The ranges overlap on purpose. System.arraycopy copies them as if through a temporary array.
		System.arraycopy(pArray, 1, pArray, 0, pCount - 1);
	}

	/**
	 * Stops collecting results and forgets the frames collected so far, without touching the source.
	 */
	@GLThread
	private void deactivate() {
		this.mActive = false;
		this.mAvailable = false;
		this.mPendingCount = 0;
		this.mPresentTimeCount = 0;
	}
}
