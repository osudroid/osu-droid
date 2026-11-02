package org.andengine.entity.primitive;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.shape.RectangularShape;
import org.andengine.entity.sprite.SpriteBatching;
import org.andengine.graphics.DrawContext;
import org.andengine.graphics.batch.QuadBatcher;
import org.andengine.graphics.shape.ShapeBatching;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 12:18:49 - 13.03.2010
 */
public class Rectangle extends RectangularShape {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public Rectangle(final float pX, final float pY, final float pWidth, final float pHeight) {
		super(pX, pY, pWidth, pHeight);

		this.setBlendingEnabled(true);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	/**
	 * Adds the rectangle's box to the batch with the rectangle's color (straight alpha) and blend
	 * function. Alpha 0 adds nothing.
	 */
	@Override
	protected void draw(final DrawContext pGLState, final Camera pCamera) {
		QuadBatcher.setProjection(pGLState.getProjectionGLMatrix());
		ShapeBatching.rect(pGLState.getModelViewGLMatrix(), 0, 0, this.getWidth(), this.getHeight(), this.getColor().getABGRPackedInt(), SpriteBatching.batchBlendOf(this));
	}

	/** The box is taken from the rectangle's size when it is drawn. */
	@Override
	protected void onUpdateVertices() {
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
