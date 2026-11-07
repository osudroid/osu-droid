package org.andengine.opengl.texture.atlas.bitmap.source;

import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
 * A bitmap of one color.
 */
public class SolidColorBitmapTextureAtlasSource extends BaseTextureAtlasSource implements IBitmapTextureAtlasSource {

	private final int mColor;

	/**
	 * @param pColor ARGB, as {@link android.graphics.Color} packs it.
	 */
	public SolidColorBitmapTextureAtlasSource(final int pTextureWidth, final int pTextureHeight, final int pColor) {
		this(0, 0, pTextureWidth, pTextureHeight, pColor);
	}

	public SolidColorBitmapTextureAtlasSource(final int pTextureX, final int pTextureY, final int pTextureWidth, final int pTextureHeight, final int pColor) {
		super(pTextureX, pTextureY, pTextureWidth, pTextureHeight);
		this.mColor = pColor;
	}

	@Override
	public SolidColorBitmapTextureAtlasSource deepCopy() {
		return new SolidColorBitmapTextureAtlasSource(this.mTextureX, this.mTextureY, this.mTextureWidth, this.mTextureHeight, this.mColor);
	}

	@Override
	public Bitmap onLoadBitmap(final Config pBitmapConfig) {
		final Bitmap bitmap = Bitmap.createBitmap(this.mTextureWidth, this.mTextureHeight, pBitmapConfig);
		bitmap.eraseColor(this.mColor);
		return bitmap;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.mTextureWidth + " x " + this.mTextureHeight + ", #" + Integer.toHexString(this.mColor) + ")";
	}
}
