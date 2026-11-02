package org.andengine.entity.sprite;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.shape.RectangularShape;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.graphics.DrawContext;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 19:22:38 - 09.03.2010
 */
public class Sprite extends RectangularShape {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int VERTICES_PER_SPRITE = 4;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final ITextureRegion mTextureRegion;

	protected boolean mFlippedVertical;
	protected boolean mFlippedHorizontal;

	/** Local corners as x,y pairs, in order around the quad: (0,0), (0,h), (w,h), (w,0). */
	private final float[] mCorners = new float[2 * Sprite.VERTICES_PER_SPRITE];
	/** Texture coordinates as u,v pairs, one per corner of {@link #mCorners}. */
	private final float[] mTextureCoordinates = new float[2 * Sprite.VERTICES_PER_SPRITE];

	// ===========================================================
	// Constructors
	// ===========================================================

	public Sprite(final float pX, final float pY, final ITextureRegion pTextureRegion) {
		this(pX, pY, pTextureRegion.getWidth(), pTextureRegion.getHeight(), pTextureRegion);
	}

	public Sprite(final float pX, final float pY, final float pWidth, final float pHeight, final ITextureRegion pTextureRegion) {
		super(pX, pY, pWidth, pHeight);

		this.mTextureRegion = pTextureRegion;

		this.setBlendingEnabled(true);
		this.initBlendFunction(pTextureRegion);

		this.onUpdateVertices();
		this.onUpdateTextureCoordinates();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public ITextureRegion getTextureRegion() {
		return this.mTextureRegion;
	}

	/**
	 * @return the local corners as x,y pairs, in order around the quad. The array is the sprite's
	 * own, read only.
	 */
	public float[] getCorners() {
		return this.mCorners;
	}

	/**
	 * @return the texture coordinates as u,v pairs, one per corner of {@link #getCorners()}. The array
	 * is the sprite's own, read only.
	 */
	public float[] getTextureCoordinates() {
		return this.mTextureCoordinates;
	}

	public boolean isFlippedHorizontal() {
		return this.mFlippedHorizontal;
	}

	public void setFlippedHorizontal(final boolean pFlippedHorizontal) {
		if(this.mFlippedHorizontal != pFlippedHorizontal) {
			this.mFlippedHorizontal = pFlippedHorizontal;

			this.onUpdateTextureCoordinates();
		}
	}

	public boolean isFlippedVertical() {
		return this.mFlippedVertical;
	}

	public void setFlippedVertical(final boolean pFlippedVertical) {
		if(this.mFlippedVertical != pFlippedVertical) {
			this.mFlippedVertical = pFlippedVertical;

			this.onUpdateTextureCoordinates();
		}
	}

	public void setFlipped(final boolean pFlippedHorizontal, final boolean pFlippedVertical) {
		if((this.mFlippedHorizontal != pFlippedHorizontal) || (this.mFlippedVertical != pFlippedVertical)) {
			this.mFlippedHorizontal = pFlippedHorizontal;
			this.mFlippedVertical = pFlippedVertical;

			this.onUpdateTextureCoordinates();
		}
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void reset() {
		super.reset();

		this.initBlendFunction(this.getTextureRegion().getTexture());
	}

	/**
	 * A texture that is not on the GPU yet (e.g. an atlas page still waiting for upload) would bind
	 * the invalid id and sample whatever texture is still bound, so the sprite skips the frame.
	 */
	@Override
	protected boolean isSelfDrawable() {
		return this.getTextureRegion().getTexture().isLoadedToHardware();
	}

	@Override
	protected void draw(final DrawContext pGLState, final Camera pCamera) {
		SpriteBatching.add(this, pGLState);
	}

	@Override
	protected void onUpdateVertices() {
		final float[] corners = this.mCorners;

		final float x2 = this.getWidth();
		final float y2 = this.getHeight();

		corners[0] = 0;
		corners[1] = 0;

		corners[2] = 0;
		corners[3] = y2;

		corners[4] = x2;
		corners[5] = y2;

		corners[6] = x2;
		corners[7] = 0;
	}

	protected void onUpdateTextureCoordinates() {
		final float[] uvs = this.mTextureCoordinates;

		final ITextureRegion textureRegion = this.mTextureRegion;

		final float u;
		final float v;
		final float u2;
		final float v2;

		if(this.mFlippedHorizontal) {
			u = textureRegion.getU2();
			u2 = textureRegion.getU();
		} else {
			u = textureRegion.getU();
			u2 = textureRegion.getU2();
		}

		if(this.mFlippedVertical) {
			v = textureRegion.getV2();
			v2 = textureRegion.getV();
		} else {
			v = textureRegion.getV();
			v2 = textureRegion.getV2();
		}

		if(textureRegion.isRotated()) {
			uvs[0] = u2;
			uvs[1] = v;

			uvs[2] = u;
			uvs[3] = v;

			uvs[4] = u;
			uvs[5] = v2;

			uvs[6] = u2;
			uvs[7] = v2;
		} else {
			uvs[0] = u;
			uvs[1] = v;

			uvs[2] = u;
			uvs[3] = v2;

			uvs[4] = u2;
			uvs[5] = v2;

			uvs[6] = u2;
			uvs[7] = v;
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
