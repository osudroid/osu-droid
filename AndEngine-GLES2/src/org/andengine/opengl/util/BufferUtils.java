package org.andengine.opengl.util;

import java.nio.ByteBuffer;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 23:06:51 - 11.08.2011
 *
 * Originally contained JNI workarounds for Android issues 11078 and 16941
 * (Froyo / Honeycomb ByteBuffer bugs). minSdkVersion is now 24, so all
 * workaround paths were always skipped and the native library load was dead
 * overhead.  All three JNI methods are removed; plain Java equivalents are
 * used unconditionally.
 */
public class BufferUtils {
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

	/**
	 * @param pCapacity the capacity of the returned {@link ByteBuffer} in bytes.
	 * @return a new direct {@link ByteBuffer} with native byte order.
	 */
	public static ByteBuffer allocateDirectByteBuffer(final int pCapacity) {
		return ByteBuffer.allocateDirect(pCapacity);
	}

	/**
	 * No-op on API 24+. Direct {@link ByteBuffer}s allocated via
	 * {@link ByteBuffer#allocateDirect} are reclaimed by the GC; explicit
	 * freeing is not required.
	 */
	@SuppressWarnings("unused")
	public static void freeDirectByteBuffer(final ByteBuffer pByteBuffer) {
		// no-op
	}

	/**
	 * @param pByteBuffer must be a direct Buffer.
	 * @param pSource
	 * @param pLength to copy in pSource.
	 * @param pOffset in pSource.
	 */
	public static void put(final ByteBuffer pByteBuffer, final float[] pSource, final int pLength, final int pOffset) {
		for(int i = pOffset; i < (pOffset + pLength); i++) {
			pByteBuffer.putFloat(pSource[i]);
		}
		pByteBuffer.position(0);
		pByteBuffer.limit(pLength << 2);
	}


	public static short getUnsignedByte(final ByteBuffer pByteBuffer) {
		return (short) (pByteBuffer.get() & 0xFF);
	}

	public static void putUnsignedByte(final ByteBuffer pByteBuffer, final int pValue) {
		pByteBuffer.put((byte) (pValue & 0xFF));
	}

	public static short getUnsignedByte(final ByteBuffer pByteBuffer, final int pPosition) {
		return (short) (pByteBuffer.get(pPosition) & (short) 0xFF);
	}

	public static void putUnsignedByte(final ByteBuffer pByteBuffer, final int pPosition, final int pValue) {
		pByteBuffer.put(pPosition, (byte) (pValue & 0xFF));
	}

	public static int getUnsignedShort(final ByteBuffer pByteBuffer) {
		return pByteBuffer.getShort() & 0xFFFF;
	}

	public static void putUnsignedShort(final ByteBuffer pByteBuffer, final int pValue) {
		pByteBuffer.putShort((short) (pValue & 0xFFFF));
	}

	public static int getUnsignedShort(final ByteBuffer pByteBuffer, final int pPosition) {
		return pByteBuffer.getShort(pPosition) & 0xFFFF;
	}

	public static void putUnsignedShort(final ByteBuffer pByteBuffer, final int pPosition, final int pValue) {
		pByteBuffer.putShort(pPosition, (short) (pValue & 0xFFFF));
	}

	public static long getUnsignedInt(final ByteBuffer pByteBuffer) {
		return pByteBuffer.getInt() & 0xFFFFFFFFL;
	}

	public static void putUnsignedInt(final ByteBuffer pByteBuffer, final long pValue) {
		pByteBuffer.putInt((int) (pValue & 0xFFFFFFFFL));
	}

	public static long getUnsignedInt(final ByteBuffer pByteBuffer, final int pPosition) {
		return pByteBuffer.getInt(pPosition) & 0xFFFFFFFFL;
	}

	public static void putUnsignedInt(final ByteBuffer pByteBuffer, final int pPosition, final long pValue) {
		pByteBuffer.putInt(pPosition, (int) (pValue & 0xFFFFFFFFL));
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
