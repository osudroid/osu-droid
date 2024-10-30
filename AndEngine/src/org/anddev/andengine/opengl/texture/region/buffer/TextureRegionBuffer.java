package org.anddev.andengine.opengl.texture.region.buffer;

import static org.anddev.andengine.opengl.vertex.RectangleVertexBuffer.VERTICES_PER_RECTANGLE;

import org.anddev.andengine.opengl.buffer.BufferObject;
import org.anddev.andengine.opengl.texture.region.BaseTextureRegion;

import java.nio.FloatBuffer;


/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 19:05:50 - 09.03.2010
 */
public class TextureRegionBuffer extends BufferObject {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected final BaseTextureRegion mTextureRegion;
	private boolean mFlippedVertical;
	private boolean mFlippedHorizontal;

	// ===========================================================
	// Constructors
	// ===========================================================

	public TextureRegionBuffer(final BaseTextureRegion pBaseTextureRegion, final int pDrawType, final boolean pManaged) {
		super(2 * VERTICES_PER_RECTANGLE, pDrawType, pManaged);
		this.mTextureRegion = pBaseTextureRegion;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public BaseTextureRegion getTextureRegion() {
		return this.mTextureRegion;
	}

	public boolean isFlippedHorizontal() {
		return this.mFlippedHorizontal;
	}

	public void setFlippedHorizontal(final boolean pFlippedHorizontal) {
		if(this.mFlippedHorizontal != pFlippedHorizontal) {
			this.mFlippedHorizontal = pFlippedHorizontal;
			this.update();
		}
	}

	public boolean isFlippedVertical() {
		return this.mFlippedVertical;
	}

	public void setFlippedVertical(final boolean pFlippedVertical) {
		if(this.mFlippedVertical != pFlippedVertical) {
			this.mFlippedVertical = pFlippedVertical;
			this.update();
		}
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void update() {
		final BaseTextureRegion textureRegion = this.mTextureRegion;

		final float x1 = textureRegion.getTextureCoordinateX1();
		final float y1 = textureRegion.getTextureCoordinateY1();
		final float x2 = textureRegion.getTextureCoordinateX2();
		final float y2 = textureRegion.getTextureCoordinateY2();

		final FloatBuffer floatBuffer = this.mFloatBuffer;

		floatBuffer.position(0);

		if(this.mFlippedVertical) {
			if(this.mFlippedHorizontal) {

				floatBuffer.put(0, x2);
				floatBuffer.put(1, y2);

				floatBuffer.put(2, x2);
				floatBuffer.put(3, y1);

				floatBuffer.put(4, x1);
				floatBuffer.put(5, y2);

				floatBuffer.put(6, x1);
				floatBuffer.put(7, y1);
			} else {

				floatBuffer.put(0, x1);
				floatBuffer.put(1, y2);

				floatBuffer.put(2, x1);
				floatBuffer.put(3, y1);

				floatBuffer.put(4, x2);
				floatBuffer.put(5, y2);

				floatBuffer.put(6, x2);
				floatBuffer.put(7, y1);
			}
		} else {
			if(this.mFlippedHorizontal) {

				floatBuffer.put(0, x2);
				floatBuffer.put(1, y1);

				floatBuffer.put(2, x2);
				floatBuffer.put(3, y2);

				floatBuffer.put(4, x1);
				floatBuffer.put(5, y1);

				floatBuffer.put(6, x1);
				floatBuffer.put(7, y2);
			} else {
				floatBuffer.put(0, x1);
				floatBuffer.put(1, y1);

				floatBuffer.put(2, x1);
				floatBuffer.put(3, y2);

				floatBuffer.put(4, x2);
				floatBuffer.put(5, y1);

				floatBuffer.put(6, x2);
				floatBuffer.put(7, y2);
			}
		}

		floatBuffer.position(0);

		super.setHardwareBufferNeedsUpdate();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
