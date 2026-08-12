package org.andengine.opengl;

import android.opengl.GLES32;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 17:44:43 - 04.09.2011
 *
 * Originally contained a Froyo (Android 2.2 / API 8) workaround that loaded a
 * native JNI library to supply missing GLES20 entry points (Android issue 8931).
 * minSdkVersion is now 24, so the workaround path was always skipped and the
 * native library load always failed with an UnsatisfiedLinkError at startup.
 * Both methods now delegate directly to GLES20 with no branching overhead.
 */
public class GLES20Fix {

	private GLES20Fix() {}

	public static void glVertexAttribPointerFix(final int pIndex, final int pSize, final int pType, final boolean pNormalized, final int pStride, final int pOffset) {
		GLES32.glVertexAttribPointer(pIndex, pSize, pType, pNormalized, pStride, pOffset);
	}

	public static void glDrawElementsFix(final int pMode, final int pCount, final int pType, final int pOffset) {
		GLES32.glDrawElements(pMode, pCount, pType, pOffset);
	}
}
