package org.andengine.entity.shape;

import org.andengine.entity.Entity;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.region.ITextureRegion;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:51:27 - 13.03.2010
 */
public abstract class Shape extends Entity implements IShape {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected int mBlendFunctionSource = IShape.BLENDFUNCTION_SOURCE_DEFAULT;
	protected int mBlendFunctionDestination = IShape.BLENDFUNCTION_DESTINATION_DEFAULT;

	protected boolean mBlendingEnabled = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Shape(final float pX, final float pY) {
		super(pX, pY);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	@Override
	public boolean isBlendingEnabled() {
		return this.mBlendingEnabled;
	}

	@Override
	public void setBlendingEnabled(final boolean pBlendingEnabled) {
		this.mBlendingEnabled = pBlendingEnabled;
	}

	@Override
	public int getBlendFunctionSource() {
		return this.mBlendFunctionSource;
	}

	@Override
	public void setBlendFunctionSource(final int pBlendFunctionSource) {
		this.mBlendFunctionSource = pBlendFunctionSource;
	}

	@Override
	public int getBlendFunctionDestination() {
		return this.mBlendFunctionDestination;
	}

	@Override
	public void setBlendFunctionDestination(final int pBlendFunctionDestination) {
		this.mBlendFunctionDestination = pBlendFunctionDestination;
	}

	@Override
	public void setBlendFunction(final int pBlendFunctionSource, final int pBlendFunctionDestination) {
		this.mBlendFunctionSource = pBlendFunctionSource;
		this.mBlendFunctionDestination = pBlendFunctionDestination;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract void onUpdateVertices();

	@Override
	public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
		return false;
	}

	@Override
	public void reset() {
		super.reset();

		this.mBlendFunctionSource = IShape.BLENDFUNCTION_SOURCE_DEFAULT;
		this.mBlendFunctionDestination = IShape.BLENDFUNCTION_DESTINATION_DEFAULT;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	protected void initBlendFunction(final ITextureRegion pTextureRegion) {
		this.initBlendFunction(pTextureRegion.getTexture());
	}

	protected void initBlendFunction(final ITexture pTexture) {
		this.initBlendFunction(pTexture.getTextureOptions());
	}

	protected void initBlendFunction(final TextureOptions pTextureOptions) {
		if(pTextureOptions.mPreMultiplyAlpha) {
			this.setBlendFunction(IShape.BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, IShape.BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
