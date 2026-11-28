// osu!droid ONLY
package org.anddev.andengine.opengl.view;

import android.os.Build;

import org.anddev.andengine.util.annotation.GLThread;

/**
 * A {@link FrameTimestampSource} backed by the <code>EGL_ANDROID_get_frame_timestamps</code> extension.
 * <p>
 * It is available on Android 8.0 (API 26) and above, on devices whose driver supports the extension.
 */
@GLThread
final class NativeFrameTimestampSource implements FrameTimestampSource {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final String LIBRARY_NAME = "osudroid_frame_timestamps";

	private static final int MINIMUM_SDK_INT = 26;

	/**
	 * The native frame timestamps are enabled.
	 */
	private static final int START_OK = 0;

	/**
	 * The device cannot provide frame timestamps, now or later.
	 */
	private static final int START_UNSUPPORTED = 1;

	/**
	 * Frame timestamps could not be enabled this time, but a later attempt may succeed.
	 */
	private static final int START_FAILED = 2;

	// ===========================================================
	// Fields
	// ===========================================================

	/**
	 * Whether frame timestamps are known to be unavailable for the rest of the process lifetime, which makes every
	 * later {@link #start()} return <code>false</code> at once.
	 */
	private static boolean sUnsupported;

	/**
	 * Whether the native library is loaded. Native methods may only be called while this is <code>true</code>.
	 */
	private static boolean sLibraryLoaded;

	private final int mSdkInt;

	private boolean mStarted;

	// ===========================================================
	// Constructors
	// ===========================================================

	NativeFrameTimestampSource() {
		this(Build.VERSION.SDK_INT);
	}

	/**
	 * @param pSdkInt The API level of the device.
	 */
	NativeFrameTimestampSource(final int pSdkInt) {
		this.mSdkInt = pSdkInt;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	@GLThread
	public boolean start() {
		if (this.mSdkInt < MINIMUM_SDK_INT || NativeFrameTimestampSource.sUnsupported) {
			return false;
		}

		if (!NativeFrameTimestampSource.sLibraryLoaded) {
			try {
				System.loadLibrary(LIBRARY_NAME);
			} catch (final UnsatisfiedLinkError e) {
				NativeFrameTimestampSource.sUnsupported = true;
				return false;
			}

			NativeFrameTimestampSource.sLibraryLoaded = true;
		}

		return switch (NativeFrameTimestampSource.nativeStart()) {
			case START_OK -> {
				this.mStarted = true;
				yield true;
			}

			case START_UNSUPPORTED -> {
				NativeFrameTimestampSource.sUnsupported = true;
				yield false;
			}

			// Cannot combine the switch together since Java fails.
			case START_FAILED -> //noinspection DuplicateBranchesInSwitch
				false;

			default -> false;
		};
	}

	@Override
	@GLThread
	public void stop() {
		if (!this.mStarted) {
			return;
		}

		this.mStarted = false;
		NativeFrameTimestampSource.nativeStop();
	}

	@Override
	@GLThread
	public long queryAndGetNextFrameId(final long[] pFrameIDs, final int pCount, final long[] pResults) {
		if (!this.mStarted) {
			for (int i = 0; i < pCount; i++) {
				pResults[i] = RESULT_UNKNOWN;
			}

			return NO_FRAME_ID;
		}

		return NativeFrameTimestampSource.nativeQueryAndGetNextFrameId(pFrameIDs, pCount, pResults);
	}

	// ===========================================================
	// Native methods
	// ===========================================================

	/**
	 * @return {@link #START_OK}, {@link #START_UNSUPPORTED} or {@link #START_FAILED}.
	 */
	private static native int nativeStart();

	private static native void nativeStop();

	/**
	 * Looks up the present times of the given frames, in order.
	 * <p>
	 * If a frame is still pending, the frames after it are reported as pending without being looked up.
	 */
	private static native long nativeQueryAndGetNextFrameId(long[] pFrameIDs, int pCount, long[] pResults);
}
