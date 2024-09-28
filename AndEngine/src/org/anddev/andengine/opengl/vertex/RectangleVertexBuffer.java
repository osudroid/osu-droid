package org.anddev.andengine.opengl.vertex;

import java.nio.FloatBuffer;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 13:07:25 - 13.03.2010
 */
public class RectangleVertexBuffer extends VertexBuffer {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int VERTICES_PER_RECTANGLE = 4;

	private static final int FLOAT_TO_RAW_INT_BITS_ZERO = Float.floatToRawIntBits(0);

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public RectangleVertexBuffer(final int pDrawType, final boolean pManaged) {
		super(2 * VERTICES_PER_RECTANGLE, pDrawType, pManaged);
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

	public synchronized void update(final float pWidth, final float pHeight) {

        final FloatBuffer floatBuffer = this.mFloatBuffer;

		floatBuffer.position(0);

		floatBuffer.put(0, 0);
		floatBuffer.put(1, 0);

		floatBuffer.put(2, 0);
		floatBuffer.put(3, pHeight);

		floatBuffer.put(4, pWidth);
		floatBuffer.put(5, 0);

		floatBuffer.put(6, pWidth);
		floatBuffer.put(7, pHeight);

		floatBuffer.position(0);

		super.setHardwareBufferNeedsUpdate();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
