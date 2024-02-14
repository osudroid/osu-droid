package org.anddev.andengine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 13:53:33 - 20.06.2010
 */
public class FileUtils {
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

	public static void copyFile(final File pIn, final File pOut) throws IOException {
		try (FileInputStream fis = new FileInputStream(pIn);
			 FileOutputStream fos = new FileOutputStream(pOut)) {
			StreamUtils.copy(fis, fos);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
