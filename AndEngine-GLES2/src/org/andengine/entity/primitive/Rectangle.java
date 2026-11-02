package org.andengine.entity.primitive;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.shape.RectangularShape;
import org.andengine.opengl.util.GLState;

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

	@Override
	protected void draw(final GLState pGLState, final Camera pCamera) {
		RectangleBatching.add(this, pGLState);
	}

	/** The box is taken from the rectangle's size when it is drawn. */
	@Override
	protected void onUpdateVertices() {
		/* Nothing. */
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
