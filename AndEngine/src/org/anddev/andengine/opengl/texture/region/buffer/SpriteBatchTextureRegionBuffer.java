package org.anddev.andengine.opengl.texture.region.buffer;

import org.anddev.andengine.opengl.buffer.BufferObject;
import org.anddev.andengine.opengl.texture.ITexture;
import org.anddev.andengine.opengl.texture.region.BaseTextureRegion;
import org.anddev.andengine.opengl.vertex.SpriteBatchVertexBuffer;

import java.nio.FloatBuffer;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 12:32:14 - 14.06.2011
 */
public class SpriteBatchTextureRegionBuffer extends BufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected int mIndex;

	// ===========================================================
	// Constructors
	// ===========================================================

	public SpriteBatchTextureRegionBuffer(final int pCapacity, final int pDrawType, final boolean pManaged) {
		super(pCapacity * 2 * SpriteBatchVertexBuffer.VERTICES_PER_RECTANGLE, pDrawType, pManaged);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getIndex() {
		return this.mIndex;
	}

	public void setIndex(final int pIndex) {
		this.mIndex = pIndex;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void add(final BaseTextureRegion pTextureRegion) {
		final ITexture texture = pTextureRegion.getTexture();

		if(texture == null) { // TODO Check really needed?
			return;
		}

		final float x1 = pTextureRegion.getTextureCoordinateX1();
		final float y1 = pTextureRegion.getTextureCoordinateY1();
		final float x2 = pTextureRegion.getTextureCoordinateX2();
		final float y2 = pTextureRegion.getTextureCoordinateY2();

		final FloatBuffer floatBuffer = this.mFloatBuffer;

		int index = this.mIndex;
		floatBuffer.position(0);

		floatBuffer.put(index++, x1);
		floatBuffer.put(index++, y1);

		floatBuffer.put(index++, x1);
		floatBuffer.put(index++, y2);

		floatBuffer.put(index++, x2);
		floatBuffer.put(index++, y1);

		floatBuffer.put(index++, x2);
		floatBuffer.put(index++, y1);

		floatBuffer.put(index++, x1);
		floatBuffer.put(index++, y2);

		floatBuffer.put(index++, x2);
		floatBuffer.put(index++, y2);

		floatBuffer.position(0);
		this.mIndex = index;
	}

	public void submit() {
		this.mFloatBuffer.position(0);

		super.setHardwareBufferNeedsUpdate();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
