package org.andengine.opengl.texture.atlas.bitmap;

import java.util.ArrayList;

import org.andengine.graphics.gles.GlesBackend;
import org.andengine.graphics.gles.GlesTextures;
import org.andengine.graphics.texture.Pixels;
import org.andengine.opengl.texture.PixelFormat;
import org.andengine.opengl.texture.TextureManager;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.TextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.EmptyBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource;
import org.andengine.opengl.texture.atlas.source.ITextureAtlasSource;
import org.andengine.opengl.texture.bitmap.BitmapTextureFormat;
import org.andengine.util.exception.NullBitmapException;
import org.andengine.util.math.MathUtils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLES32;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 14:55:02 - 08.03.2010
 */
public class BitmapTextureAtlas extends TextureAtlas<IBitmapTextureAtlasSource> {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final BitmapTextureFormat mBitmapTextureFormat;

	/** Pixels loaded in the background for the next {@link #writeTextureToHardware()}; GL thread only. */
	private Pixels[] mPreparedPixels;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * Uses {@link BitmapTextureFormat#RGBA_8888}.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight) {
		this(pTextureManager, pWidth, pHeight, BitmapTextureFormat.RGBA_8888);
	}

	/**
	 * @param pBitmapTextureFormat use {@link BitmapTextureFormat#RGBA_8888} or {@link BitmapTextureFormat#RGBA_4444} for a {@link BitmapTextureAtlas} with transparency and {@link BitmapTextureFormat#RGB_565} for a {@link BitmapTextureAtlas} without transparency.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final BitmapTextureFormat pBitmapTextureFormat) {
		this(pTextureManager, pWidth, pHeight, pBitmapTextureFormat, TextureOptions.DEFAULT, null);
	}

	/**
	 * Uses {@link BitmapTextureFormat#RGBA_8888}.
	 *
	 * @param pTextureStateListener to be informed when this {@link BitmapTextureAtlas} is loaded, unloaded or a {@link ITextureAtlasSource} failed to load.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final ITextureAtlasStateListener<IBitmapTextureAtlasSource> pTextureAtlasStateListener) {
		this(pTextureManager, pWidth, pHeight, BitmapTextureFormat.RGBA_8888, TextureOptions.DEFAULT, pTextureAtlasStateListener);
	}

	/**
	 * @param pBitmapTextureFormat use {@link BitmapTextureFormat#RGBA_8888} or {@link BitmapTextureFormat#RGBA_4444} for a {@link BitmapTextureAtlas} with transparency and {@link BitmapTextureFormat#RGB_565} for a {@link BitmapTextureAtlas} without transparency.
	 * @param pTextureAtlasStateListener to be informed when this {@link BitmapTextureAtlas} is loaded, unloaded or a {@link ITextureAtlasSource} failed to load.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final BitmapTextureFormat pBitmapTextureFormat, final ITextureAtlasStateListener<IBitmapTextureAtlasSource> pTextureAtlasStateListener) {
		this(pTextureManager, pWidth, pHeight, pBitmapTextureFormat, TextureOptions.DEFAULT, pTextureAtlasStateListener);
	}

	/**
	 * Uses {@link BitmapTextureFormat#RGBA_8888}.
	 *
	 * @param pTextureOptions the (quality) settings of this {@link BitmapTextureAtlas}.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final TextureOptions pTextureOptions) throws IllegalArgumentException {
		this(pTextureManager, pWidth, pHeight, BitmapTextureFormat.RGBA_8888, pTextureOptions, null);
	}

	/**
	 * @param pBitmapTextureFormat use {@link BitmapTextureFormat#RGBA_8888} or {@link BitmapTextureFormat#RGBA_4444} for a {@link BitmapTextureAtlas} with transparency and {@link BitmapTextureFormat#RGB_565} for a {@link BitmapTextureAtlas} without transparency.
	 * @param pTextureOptions the (quality) settings of this {@link BitmapTextureAtlas}.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final BitmapTextureFormat pBitmapTextureFormat, final TextureOptions pTextureOptions) throws IllegalArgumentException {
		this(pTextureManager, pWidth, pHeight, pBitmapTextureFormat, pTextureOptions, null);
	}

	/**
	 * Uses {@link BitmapTextureFormat#RGBA_8888}.
	 *
	 * @param pTextureOptions the (quality) settings of this {@link BitmapTextureAtlas}.
	 * @param pTextureAtlasStateListener to be informed when this {@link BitmapTextureAtlas} is loaded, unloaded or a {@link ITextureAtlasSource} failed to load.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final TextureOptions pTextureOptions, final ITextureAtlasStateListener<IBitmapTextureAtlasSource> pTextureAtlasStateListener) throws IllegalArgumentException {
		this(pTextureManager, pWidth, pHeight, BitmapTextureFormat.RGBA_8888, pTextureOptions, pTextureAtlasStateListener);
	}

	/**
	 * @param pBitmapTextureFormat use {@link BitmapTextureFormat#RGBA_8888} or {@link BitmapTextureFormat#RGBA_4444} for a {@link BitmapTextureAtlas} with transparency and {@link BitmapTextureFormat#RGB_565} for a {@link BitmapTextureAtlas} without transparency.
	 * @param pTextureOptions the (quality) settings of this {@link BitmapTextureAtlas}.
	 * @param pTextureAtlasStateListener to be informed when this {@link BitmapTextureAtlas} is loaded, unloaded or a {@link ITextureAtlasSource} failed to load.
	 */
	public BitmapTextureAtlas(final TextureManager pTextureManager, final int pWidth, final int pHeight, final BitmapTextureFormat pBitmapTextureFormat, final TextureOptions pTextureOptions, final ITextureAtlasStateListener<IBitmapTextureAtlasSource> pTextureAtlasStateListener) throws IllegalArgumentException {
		super(pTextureManager, pWidth, pHeight, pBitmapTextureFormat.getPixelFormat(), pTextureOptions, pTextureAtlasStateListener);

		this.mBitmapTextureFormat = pBitmapTextureFormat;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public BitmapTextureFormat getBitmapTextureFormat() {
		return this.mBitmapTextureFormat;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	
	@Override
	public void addEmptyTextureAtlasSource(final int pTextureX, final int pTextureY, final int pWidth, final int pHeight) {
		this.addTextureAtlasSource(new EmptyBitmapTextureAtlasSource(pWidth, pHeight), pTextureX, pTextureY);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Whether the sources are uploaded as RGBA bytes ({@link IPixelTextureAtlasSource#onLoadPixels(boolean)}),
	 * premultiplied or straight as {@link TextureOptions#mPreMultiplyAlpha} says.
	 */
	private boolean uploadsPixelsDirectly() {
		return this.mBitmapTextureFormat.getPixelFormat() == PixelFormat.RGBA_8888;
	}

	/** Whether {@code pSource} delivers the bytes of this atlas' variant (see {@link IPixelTextureAtlasSource#canLoadPixels(boolean)}). */
	private boolean deliversPixels(final IBitmapTextureAtlasSource pSource) {
		return pSource instanceof IPixelTextureAtlasSource && ((IPixelTextureAtlasSource) pSource).canLoadPixels(this.mTextureOptions.mPreMultiplyAlpha);
	}

	/**
	 * Called on the GL thread. Whether the pixels of all sources can be loaded on a background thread: this
	 * atlas has sources, uploads RGBA bytes and every source can deliver them in this atlas' variant
	 * (straight or premultiplied). Otherwise it is loaded on the GL thread as before.
	 */
	public boolean canPreparePixels() {
		final ArrayList<IBitmapTextureAtlasSource> textureSources = this.mTextureAtlasSources;
		final int textureSourceCount = textureSources.size();
		if(textureSourceCount == 0 || !this.uploadsPixelsDirectly()) {
			return false;
		}
		for(int i = 0; i < textureSourceCount; i++) {
			if(!this.deliversPixels(textureSources.get(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Called on the GL thread. Returns a preparation that loads the pixels of all sources on a background
	 * thread, or {@code null} unless {@link #canPreparePixels()}.
	 */
	public PixelPreparation createPixelPreparation() {
		return this.canPreparePixels() ? this.createPixelPreparationUnchecked() : null;
	}

	/**
	 * Like {@link #createPixelPreparation()}, for a caller that has just checked {@link #canPreparePixels()}
	 * on the GL thread (the sources cannot have changed in between).
	 */
	public PixelPreparation createPixelPreparationUnchecked() {

		final ArrayList<IBitmapTextureAtlasSource> textureSources = this.mTextureAtlasSources;
		final int textureSourceCount = textureSources.size();
		final ArrayList<IPixelTextureAtlasSource> pixelSources = new ArrayList<IPixelTextureAtlasSource>(textureSourceCount);
		for(int i = 0; i < textureSourceCount; i++) {
			pixelSources.add((IPixelTextureAtlasSource) textureSources.get(i));
		}
		return new PixelPreparation(pixelSources, this.mWidth, this.mHeight, this.mTextureOptions.mPreMultiplyAlpha);
	}

	/**
	 * Called on the GL thread right before loading: the next write uploads the pixels of {@code pPreparation}
	 * instead of loading them, as long as they still belong to the current sources. {@code null} clears it.
	 */
	public void usePreparedPixels(final PixelPreparation pPreparation) {
		this.mPreparedPixels = (pPreparation == null) ? null : pPreparation.take(this.mTextureAtlasSources);
	}

	/**
	 * Called on the GL thread. Whether {@code pPreparation} was made from this atlas' current sources (none
	 * added or moved since), so its pixels would still be used by the next write.
	 */
	public boolean isPreparationCurrent(final PixelPreparation pPreparation) {
		return pPreparation.matches(this.mTextureAtlasSources);
	}

	@Override
	protected void writeTextureToHardware() {
		final PixelFormat pixelFormat = this.mBitmapTextureFormat.getPixelFormat();
		final int glFormat = pixelFormat.getGLFormat();
		final int glType = pixelFormat.getGLType();

		if(pixelFormat == PixelFormat.RGBA_8888) {
			/* Texture.loadToHardware generates a fresh texture name before every write (also on a reload), so immutable storage is always allowed here. */
			GlesBackend.getGl().glTexStorage2D(GLES32.GL_TEXTURE_2D, 1, GLES32.GL_RGBA8, this.mWidth, this.mHeight);
		} else {
			GlesTextures.allocate(this.mWidth, this.mHeight, pixelFormat);
		}

		final boolean preMultipyAlpha = this.mTextureOptions.mPreMultiplyAlpha;
		/* RGBA sources that deliver their bytes (in this atlas' variant) directly skip the bitmap conversion. */
		final boolean uploadPixelsDirectly = this.uploadsPixelsDirectly();
		/* Pixels loaded in the background are used once; a later reload loads anew. */
		final Pixels[] preparedPixels = this.mPreparedPixels;
		this.mPreparedPixels = null;
		/* Non alpha premultiplied bitmaps are loaded with ARGB_8888 and converted down manually. */
		final Config bitmapConfig = (preMultipyAlpha) ? this.mBitmapTextureFormat.getBitmapConfig() : Config.ARGB_8888;

		final ArrayList<IBitmapTextureAtlasSource> textureSources = this.mTextureAtlasSources;
		final int textureSourceCount = textureSources.size();

		final ITextureAtlasStateListener<IBitmapTextureAtlasSource> textureStateListener = this.getTextureAtlasStateListener();
		for(int i = 0; i < textureSourceCount; i++) {
			final IBitmapTextureAtlasSource bitmapTextureAtlasSource = textureSources.get(i);
			if(uploadPixelsDirectly && this.deliversPixels(bitmapTextureAtlasSource) && this.uploadPixels((IPixelTextureAtlasSource) bitmapTextureAtlasSource, preparedPixels, i)) {
				if(textureStateListener != null) {
					textureStateListener.onTextureAtlasSourceLoaded(this, bitmapTextureAtlasSource);
				}
				continue;
			}

			try {
				final Bitmap bitmap = bitmapTextureAtlasSource.onLoadBitmap(bitmapConfig);
				if(bitmap == null) {
					throw new NullBitmapException("Caused by: " + bitmapTextureAtlasSource.getClass().toString() + " --> " + bitmapTextureAtlasSource.toString() + " returned a null Bitmap.");
				}

				final boolean useDefaultAlignment = MathUtils.isPowerOfTwo(bitmap.getWidth()) && MathUtils.isPowerOfTwo(bitmap.getHeight()) && pixelFormat == PixelFormat.RGBA_8888;
				if(!useDefaultAlignment) {
					/* Adjust unpack alignment. */
					GlesTextures.unpackAlignment(1);
				}

				if(preMultipyAlpha) {
					GlesTextures.texSubImage2DPremultiplied(bitmapTextureAtlasSource.getTextureX(), bitmapTextureAtlasSource.getTextureY(), bitmap, glFormat, glType);
				} else {
					GlesTextures.texSubImage2D(bitmapTextureAtlasSource.getTextureX(), bitmapTextureAtlasSource.getTextureY(), bitmap, this.mPixelFormat);
				}

				if(!useDefaultAlignment) {
					/* Restore default unpack alignment. */
					GlesTextures.unpackAlignment(GlesTextures.UNPACK_ALIGNMENT_DEFAULT);
				}

				bitmap.recycle();

				if(textureStateListener != null) {
					textureStateListener.onTextureAtlasSourceLoaded(this, bitmapTextureAtlasSource);
				}
			} catch (final NullBitmapException e) {
				if(textureStateListener != null) {
					textureStateListener.onTextureAtlasSourceLoadExeption(this, bitmapTextureAtlasSource, e);
				} else {
					throw e;
				}
			}
		}
	}

	/**
	 * Uploads the RGBA bytes of {@code pSource} as they are: the prepared ones at {@code pIndex} if
	 * {@code pPreparedPixels} is given (a {@code null} entry means the source had none), else loaded now.
	 * Premultiplied atlases take premultiplied bytes (the memory the bitmap upload sent before), all others
	 * straight ones.
	 *
	 * @return {@code false} if the source has no pixels of this atlas' variant, so the bitmap path loads it instead.
	 */
	private boolean uploadPixels(final IPixelTextureAtlasSource pSource, final Pixels[] pPreparedPixels, final int pIndex) {
		final boolean premultiplied = this.mTextureOptions.mPreMultiplyAlpha;
		final Pixels pixels = (pPreparedPixels != null && pIndex < pPreparedPixels.length) ? pPreparedPixels[pIndex] : pSource.onLoadPixels(premultiplied);
		if(pixels == null || pixels.getPremultiplied() != premultiplied) {
			return false;
		}

		/* Rows of 4-byte pixels are always 4-byte aligned, so the default unpack alignment fits. */
		GlesBackend.getGl().glTexSubImage2D(GLES32.GL_TEXTURE_2D, 0, pSource.getTextureX(), pSource.getTextureY(), pixels.getWidth(), pixels.getHeight(), GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, pixels.getData());
		return true;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
