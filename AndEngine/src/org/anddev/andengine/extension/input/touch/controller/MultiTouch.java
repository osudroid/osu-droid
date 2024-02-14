package org.anddev.andengine.extension.input.touch.controller;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 16:00:38 - 14.07.2010
 */
public class MultiTouch {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private static Boolean SUPPORTED = null;

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

	public static boolean isSupportedByAndroidVersion() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;
	}

	public static boolean isSupported(final Context pContext) {
		if(SUPPORTED == null) {
			SUPPORTED = isSupportedByAndroidVersion() && pContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);
		}

		return SUPPORTED;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
