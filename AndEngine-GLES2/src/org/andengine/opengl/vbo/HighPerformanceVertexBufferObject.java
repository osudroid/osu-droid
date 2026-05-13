package org.andengine.opengl.vbo;

import java.nio.FloatBuffer;

import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES20;

/**
 * Compared to a {@link LowMemoryVertexBufferObject}, the {@link HighPerformanceVertexBufferObject} uses <b><u>2x</u> the heap memory</b>, 
 * at the benefit of significantly faster data buffering (<b>up to <u>5x</u> faster!</b>).
 * 
 * @see {@link LowMemoryVertexBufferObject} when to prefer a {@link LowMemoryVertexBufferObject} instead of a {@link HighPerformanceVertexBufferObject}
 *
 * <p>(c) Zynga 2011</p>
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 14:42:18 - 15.11.2011
 */
public class HighPerformanceVertexBufferObject extends VertexBufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final float[] mBufferData;
	// osu!droid: mFloatBuffer is always non-null — the old SDK_VERSION_HONEYCOMB_OR_LATER (API ≥ 11)
	// guard that conditionally initialized it to null is dead code on minSdk 24.
	protected final FloatBuffer mFloatBuffer;

	// ===========================================================
	// Constructors
	// ===========================================================

	public HighPerformanceVertexBufferObject(final VertexBufferObjectManager pVertexBufferObjectManager, final int pCapacity, final DrawType pDrawType, final boolean pAutoDispose, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		super(pVertexBufferObjectManager, pCapacity, pDrawType, pAutoDispose, pVertexBufferObjectAttributes);

		this.mBufferData = new float[pCapacity];
		this.mFloatBuffer = this.mByteBuffer.asFloatBuffer();
	}

	public HighPerformanceVertexBufferObject(final VertexBufferObjectManager pVertexBufferObjectManager, final float[] pBufferData, final DrawType pDrawType, final boolean pAutoDispose, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		super(pVertexBufferObjectManager, pBufferData.length, pDrawType, pAutoDispose, pVertexBufferObjectAttributes);
		this.mBufferData = pBufferData;
		this.mFloatBuffer = this.mByteBuffer.asFloatBuffer();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public float[] getBufferData() {
		return this.mBufferData;
	}

	@Override
	public int getHeapMemoryByteSize() {
		return this.getByteCapacity();
	}

	@Override
	public int getNativeHeapMemoryByteSize() {
		return this.getByteCapacity();
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onBufferData() {
		this.mFloatBuffer.position(0);
		this.mFloatBuffer.put(this.mBufferData);

		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, this.mByteBuffer.capacity(), this.mByteBuffer, this.mUsage);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
