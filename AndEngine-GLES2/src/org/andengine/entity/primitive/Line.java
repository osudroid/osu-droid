package org.andengine.entity.primitive;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.shape.IShape;
import org.andengine.entity.shape.RectangularShape;
import org.andengine.entity.shape.Shape;
import org.andengine.entity.sprite.SpriteBatching;
import org.andengine.graphics.batch.QuadBatcher;
import org.andengine.graphics.shape.ShapeBatching;
import org.andengine.graphics.DrawContext;
import org.andengine.util.algorithm.collision.LineCollisionChecker;
import org.andengine.util.algorithm.collision.RectangularShapeCollisionChecker;
import org.andengine.util.exception.MethodNotSupportedException;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 09:50:36 - 04.04.2010
 */
public class Line extends Shape {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final float LINE_WIDTH_DEFAULT = 1.0f;

	// ===========================================================
	// Fields
	// ===========================================================

	protected float mX2;
	protected float mY2;

	protected float mLineWidth;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Line(final float pX1, final float pY1, final float pX2, final float pY2) {
		this(pX1, pY1, pX2, pY2, Line.LINE_WIDTH_DEFAULT);
	}

	public Line(final float pX1, final float pY1, final float pX2, final float pY2, final float pLineWidth) {
		super(pX1, pY1);

		this.mX2 = pX2;
		this.mY2 = pY2;

		this.mLineWidth = pLineWidth;

		final float centerX = (this.mX2 - this.mX) * 0.5f;
		final float centerY = (this.mY2 - this.mY) * 0.5f;

		this.mRotationCenterX = centerX;
		this.mRotationCenterY = centerY;

		this.mScaleCenterX = this.mRotationCenterX;
		this.mScaleCenterY = this.mRotationCenterY;

		this.setBlendingEnabled(true);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * @deprecated Instead use {@link Line#getX1()} or {@link Line#getX2()}.
	 */
	@Deprecated
	@Override
	public float getX() {
		return super.getX();
	}

	/**
	 * @deprecatedInstead use {@link Line#getY1()} or {@link Line#getY2()}.
	 */
	@Deprecated
	@Override
	public float getY() {
		return super.getY();
	}

	public float getX1() {
		return super.getX();
	}

	public float getY1() {
		return super.getY();
	}

	public float getX2() {
		return this.mX2;
	}

	public float getY2() {
		return this.mY2;
	}

	public float getLineWidth() {
		return this.mLineWidth;
	}

	public void setLineWidth(final float pLineWidth) {
		this.mLineWidth = pLineWidth;
	}

	/**
	 * Instead use {@link Line#setPosition(float, float, float, float)}.
	 */
	@Deprecated
	@Override
	public void setX(final float pX) {
		final float dX = this.mX - pX;

		super.setX(pX);

		this.mX2 += dX;
	}

	/**
	 * Instead use {@link Line#setPosition(float, float, float, float)}.
	 */
	@Deprecated
	@Override
	public void setY(final float pY) {
		final float dY = this.mY - pY;
		
		super.setY(pY);
		
		this.mY2 += dY;
	}

	/**
	 * Instead use {@link Line#setPosition(float, float, float, float)}.
	 */
	@Deprecated
	@Override
	public void setPosition(final float pX, final float pY) {
		final float dX = this.mX - pX;
		final float dY = this.mY - pY;

		super.setPosition(pX, pY);

		this.mX2 += dX;
		this.mY2 += dY;
	}

	public void setPosition(final float pX1, final float pY1, final float pX2, final float pY2) {
		this.mX2 = pX2;
		this.mY2 = pY2;

		super.setPosition(pX1, pY1);

		this.onUpdateVertices();
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public boolean isCulled(final Camera pCamera) {
		return pCamera.isLineVisible(this);
	}

	/**
	 * Adds the line from (x1, y1) to (x2, y2) to the batch as a quad of {@link #getLineWidth()},
	 * centered on the segment, with the line's color and blend function.
	 */
	@Override
	protected void draw(final DrawContext pGLState, final Camera pCamera) {
		QuadBatcher.setProjection(pGLState.getProjectionGLMatrix());
		ShapeBatching.line(pGLState.getModelViewGLMatrix(), 0, 0, this.mX2 - this.mX, this.mY2 - this.mY, this.mLineWidth, this.getColor().getABGRPackedInt(), SpriteBatching.batchBlendOf(this));
	}

	/** The end points are read from the line when it is drawn. */
	@Override
	protected void onUpdateVertices() {
		/* Nothing. */
	}

	@Override
	public float[] getSceneCenterCoordinates() {
		throw new MethodNotSupportedException();
	}

	@Override
	public float[] getSceneCenterCoordinates(final float[] pReuse) {
		throw new MethodNotSupportedException();
	}

	@Override
	@Deprecated
	public boolean contains(final float pX, final float pY) {
		throw new MethodNotSupportedException();
	}

	@Override
	public boolean collidesWith(final IShape pOtherShape) {
		if(pOtherShape instanceof Line) {
			final Line otherLine = (Line) pOtherShape;
			return LineCollisionChecker.checkLineCollision(this.mX, this.mY, this.mX2, this.mY2, otherLine.mX, otherLine.mY, otherLine.mX2, otherLine.mY2);
		} else if(pOtherShape instanceof RectangularShape) {
			return RectangularShapeCollisionChecker.checkCollision((RectangularShape) pOtherShape, this);
		} else {
			return false;
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}