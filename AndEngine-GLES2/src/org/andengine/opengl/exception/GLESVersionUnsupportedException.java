package org.andengine.opengl.exception;

/**
 * Thrown when the device's EGL driver cannot create a GL context satisfying the
 * minimum OpenGL ES version the engine requires (see {@code RenderSurfaceView.GLES32ContextFactory}).
 * Carries enough device diagnostics for a user facing dialog and bug reports, since the
 * generic stack trace is meaningless to players hitting a hardware limitation.
 */
public class GLESVersionUnsupportedException extends RuntimeException {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final long serialVersionUID = 1L;

	// ===========================================================
	// Fields
	// ===========================================================

	private final String mRequiredVersion;
	private final String mDetectedVersion;
	private final String mDevice;
	private final String mChipset;
	private final String mAndroidVersion;

	// ===========================================================
	// Constructors
	// ===========================================================

	public GLESVersionUnsupportedException(final String pRequiredVersion, final String pDetectedVersion,
			final String pDevice, final String pChipset, final String pAndroidVersion) {
		this(pRequiredVersion, pDetectedVersion, pDevice, pChipset, pAndroidVersion, null);
	}

	public GLESVersionUnsupportedException(final String pRequiredVersion, final String pDetectedVersion,
			final String pDevice, final String pChipset, final String pAndroidVersion, final Throwable pCause) {
		super("Device does not support " + pRequiredVersion + " (detected: " + pDetectedVersion + ", device: " + pDevice + ")", pCause);

		this.mRequiredVersion = pRequiredVersion;
		this.mDetectedVersion = pDetectedVersion;
		this.mDevice = pDevice;
		this.mChipset = pChipset;
		this.mAndroidVersion = pAndroidVersion;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public String getRequiredVersion() {
		return this.mRequiredVersion;
	}

	public String getDetectedVersion() {
		return this.mDetectedVersion;
	}

	public String getDevice() {
		return this.mDevice;
	}

	public String getChipset() {
		return this.mChipset;
	}

	public String getAndroidVersion() {
		return this.mAndroidVersion;
	}
}
