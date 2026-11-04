package org.andengine.opengl.texture;

import java.io.IOException;

import org.andengine.graphics.gles.GlesTextures;
import org.andengine.opengl.texture.atlas.source.ITextureAtlasSource;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 14:55:02 - 08.03.2010
 */
public abstract class Texture implements ITexture {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int HARDWARE_TEXTURE_ID_INVALID = -1;

	// ===========================================================
	// Fields
	// ===========================================================

	protected final TextureManager mTextureManager;
	protected final PixelFormat mPixelFormat;
	protected final TextureOptions mTextureOptions;

	protected int mHardwareTextureID = Texture.HARDWARE_TEXTURE_ID_INVALID;
	protected boolean mUpdateOnHardwareNeeded = false;

	protected ITextureStateListener mTextureStateListener;

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * @param pPixelFormat
	 * @param pTextureOptions the (quality) settings of the Texture.
	 * @param pTextureStateListener to be informed when this {@link Texture} is loaded, unloaded or a {@link ITextureAtlasSource} failed to load.
	 */
	public Texture(final TextureManager pTextureManager, final PixelFormat pPixelFormat, final TextureOptions pTextureOptions, final ITextureStateListener pTextureStateListener) throws IllegalArgumentException {
		this.mTextureManager = pTextureManager;
		this.mPixelFormat = pPixelFormat;
		this.mTextureOptions = pTextureOptions;
		this.mTextureStateListener = pTextureStateListener;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public TextureManager getTextureManager() {
		return this.mTextureManager;
	}

	@Override
	public int getHardwareTextureID() {
		return this.mHardwareTextureID;
	}

	@Override
	public boolean isLoadedToHardware() {
		return this.mHardwareTextureID != Texture.HARDWARE_TEXTURE_ID_INVALID;
	}

	@Override
	public void setNotLoadedToHardware() {
		this.mHardwareTextureID = Texture.HARDWARE_TEXTURE_ID_INVALID;
	}

	@Override
	public boolean isUpdateOnHardwareNeeded() {
		return this.mUpdateOnHardwareNeeded;
	}

	@Override
	public void setUpdateOnHardwareNeeded(final boolean pUpdateOnHardwareNeeded) {
		this.mUpdateOnHardwareNeeded = pUpdateOnHardwareNeeded;
	}

	@Override
	public PixelFormat getPixelFormat() {
		return this.mPixelFormat;
	}

	@Override
	public TextureOptions getTextureOptions() {
		return this.mTextureOptions;
	}

	@Override
	public ITextureStateListener getTextureStateListener() {
		return this.mTextureStateListener;
	}

	@Override
	public void setTextureStateListener(final ITextureStateListener pTextureStateListener) {
		this.mTextureStateListener = pTextureStateListener;
	}

	@Override
	public boolean hasTextureStateListener() {
		return this.mTextureStateListener != null;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract void writeTextureToHardware() throws IOException;

	@Override
	public void load() {
		this.mTextureManager.loadTexture(this);
	}

	@Override
	public void loadNow() throws IOException {
		this.mTextureManager.loadTextureNow(this);
	}

	@Override
	public void unload() {
		this.mTextureManager.unloadTexture(this);
	}

	@Override
	public void unloadNow() {
		this.mTextureManager.unloadTextureNow(this);
	}

	@Override
	public void loadToHardware() throws IOException {
		this.mHardwareTextureID = GlesTextures.generate();

		GlesTextures.bind(this.mHardwareTextureID);

		this.writeTextureToHardware();

		this.mTextureOptions.apply();

		this.mUpdateOnHardwareNeeded = false;

		if(this.mTextureStateListener != null) {
			this.mTextureStateListener.onLoadedToHardware(this);
		}
	}

	@Override
	public void unloadFromHardware() {
		GlesTextures.delete(this.mHardwareTextureID);

		this.mHardwareTextureID = Texture.HARDWARE_TEXTURE_ID_INVALID;

		if(this.mTextureStateListener != null) {
			this.mTextureStateListener.onUnloadedFromHardware(this);
		}
	}

	@Override
	public void reloadToHardware() throws IOException {
		this.unloadFromHardware();
		this.loadToHardware();
	}

	@Override
	public void bind() {
		GlesTextures.bind(this.mHardwareTextureID);
	}

	@Override
	public void bind(final int pGLActiveTexture) {
		GlesTextures.bind(this.mHardwareTextureID, pGLActiveTexture);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}