package org.andengine;

import org.andengine.graphics.gles.GlesConfigChooser;
import org.andengine.util.exception.DeviceNotSupportedException;
import org.andengine.util.exception.DeviceNotSupportedException.DeviceNotSupportedCause;

/**
 * (c) Zynga 2012
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 14:03:59 - 19.03.2012
 */
public class AndEngine {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static boolean isDeviceSupported() {
		try {
			AndEngine.checkDeviceSupported();
			return true;
		} catch (final DeviceNotSupportedException e) {
			return false;
		}
	}

	public static void checkDeviceSupported() throws DeviceNotSupportedException {
		AndEngine.checkCodePathSupport();

		AndEngine.checkOpenGLSupport();
	}

	private static void checkCodePathSupport() {
		// osu!droid: System.loadLibrary("andengine") was here behind an isAndroidVersionOrLower(FROYO)
		// guard. minSdk is 24 so the guard is always false, and libandengine.so was removed from the
		// APK when dead JNI workarounds were cleaned up (Issues 7+8). Nothing to do.
	}

	private static void checkOpenGLSupport() throws DeviceNotSupportedException {
		AndEngine.checkEGLConfigChooserSupport();
	}

	private static void checkEGLConfigChooserSupport() throws DeviceNotSupportedException {
		final GlesConfigChooser configChooser = new GlesConfigChooser(false); // TODO Doesn't correlate to possible multisampling request in EngineOptions...

		try {
			configChooser.probeDefaultDisplay();
		} catch (final IllegalArgumentException e) {
			throw new DeviceNotSupportedException(DeviceNotSupportedCause.EGLCONFIG_NOT_FOUND, e);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
