package org.andengine.opengl.texture;

import java.io.IOException;

import android.opengl.GLES32;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 15:01:03 - 11.07.2011
 */
public interface ITexture {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public int getWidth();
	public int getHeight();

	public int getHardwareTextureID();

	public boolean isLoadedToHardware();
	public void setNotLoadedToHardware();

	public boolean isUpdateOnHardwareNeeded();
	public void setUpdateOnHardwareNeeded(final boolean pUpdateOnHardwareNeeded);

	/**
	 * @see {@link TextureManager#loadTexture(ITexture)}.
	 */
	public void load();
	/**
	 * @see {@link TextureManager#loadTextureNow(ITexture)}.
	 */
	public void loadNow() throws IOException;
	/**
	 * @see {@link TextureManager#unloadTexture(ITexture)}.
	 */
	public void unload();
	/**
	 * @see {@link TextureManager#unloadTextureNow(ITexture)}.
	 */
	public void unloadNow();

	public void loadToHardware() throws IOException;
	public void unloadFromHardware();
	public void reloadToHardware() throws IOException;

	/**
	 * Called when the {@link TextureManager} drops this texture without {@link #unloadFromHardware()}: by
	 * {@link TextureManager#onReload()} for a texture that was queued for unloading when the GL context was
	 * lost (its texture names went with the old context), and by {@link TextureManager#unloadTexture(ITexture)}
	 * for a texture still waiting for its upload. Release the resources {@link #unloadFromHardware()} would have
	 * released apart from GL here. Must not make GL calls. The texture is not loaded to hardware.
	 * <p>
	 * A dropped texture is finished: it is not managed any more and must not be passed to
	 * {@link TextureManager#loadTexture(ITexture)} again, since implementations may release resources here
	 * that cannot be recreated.
	 */
	public default void onDroppedWithoutUnload() {
	}

	public void bind();
	/**
	 * @param pGLActiveTexture from {@link GLES20#GL_TEXTURE0} to {@link GLES20#GL_TEXTURE31}. 
	 */
	public void bind(final int pGLActiveTexture);

	public PixelFormat getPixelFormat();
	public TextureOptions getTextureOptions();

	public boolean hasTextureStateListener();
	public ITextureStateListener getTextureStateListener();
	public void setTextureStateListener(final ITextureStateListener pTextureStateListener);

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
}