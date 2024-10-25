package org.anddev.andengine.opengl.vertex;

import java.nio.FloatBuffer;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 13:07:25 - 13.03.2010
 */
public class LineVertexBuffer extends VertexBuffer {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int VERTICES_PER_LINE = 2;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public LineVertexBuffer(final int pDrawType, final boolean pManaged) {
		super(2 * VERTICES_PER_LINE, pDrawType, pManaged);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void update(final float pX1, final float pY1, final float pX2, final float pY2) {
		final FloatBuffer floatBuffer = this.mFloatBuffer;

		floatBuffer.position(0);

		floatBuffer.put(0, pX1);
		floatBuffer.put(1, pY1);

		floatBuffer.put(2, pX2);
		floatBuffer.put(3, pY2);

		floatBuffer.position(0);

		super.setHardwareBufferNeedsUpdate();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
