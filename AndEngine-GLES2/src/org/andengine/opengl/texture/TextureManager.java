package org.andengine.opengl.texture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

import org.andengine.graphics.texture.AtlasPixelLoader;
import org.andengine.graphics.texture.RawPixelSource;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.bitmap.BitmapTexture;
import org.andengine.opengl.texture.bitmap.BitmapTextureFormat;
import org.andengine.util.adt.io.in.AssetInputStreamOpener;
import org.andengine.util.adt.io.in.IInputStreamOpener;
import org.andengine.util.debug.Debug;

import android.content.res.AssetManager;
import android.os.Process;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 17:48:46 - 08.03.2010
 */
public class TextureManager {
	// ===========================================================
	// Constants
	// ===========================================================

	/**
	 * Bytes of prepared pixels uploaded per frame. A full 2048x2048 page (16 MiB) goes alone; small
	 * textures share a frame up to this amount. At least one texture is uploaded per frame, whatever its size.
	 */
	public static final long UPLOAD_BYTES_PER_FRAME_DEFAULT = 8L * 1024 * 1024;

	/**
	 * Memory of pixels being prepared or waiting for upload, estimated by atlas size: four 2048x2048 pages.
	 * At least one preparation runs, whatever its size.
	 */
	public static final long PREPARED_BYTES_IN_FLIGHT_DEFAULT = 64L * 1024 * 1024;

	/** Returned by {@link #startPreparation(ITexture, HashMap)} while the in-flight limit is reached; compared by identity, never run. */
	private static final AtlasPixelLoader WAITING = new AtlasPixelLoader(new ArrayList<RawPixelSource>(), 0, 0);

	// ===========================================================
	// Fields
	// ===========================================================

	private final HashSet<ITexture> mTexturesManaged = new HashSet<ITexture>();
	private final HashMap<String, ITexture> mTexturesMapped = new HashMap<String, ITexture>();

	private final ArrayList<ITexture> mTexturesLoaded = new ArrayList<ITexture>();

	private final ArrayList<ITexture> mTexturesToBeLoaded = new ArrayList<ITexture>();
	private final ArrayList<ITexture> mTexturesToBeUnloaded = new ArrayList<ITexture>();

	private boolean mReloading;

	/* Background preparation of atlas pixels, see updateTextures. */
	private Executor mPreparationExecutor;
	/** Whether mPreparationExecutor was created here (and so is shut down in onDestroy) rather than injected. */
	private boolean mPreparationExecutorOwned;
	private final long mUploadBytesPerFrame;
	private final long mPreparedBytesInFlight;
	/** The preparation of each texture waiting in mTexturesToBeLoaded. */
	private final HashMap<ITexture, AtlasPixelLoader> mPreparations = new HashMap<ITexture, AtlasPixelLoader>();
	/** Loaded textures whose reload {@link #reloadLoadedTextures()} requested and that are not reloaded yet. */
	private final HashSet<ITexture> mTexturesToBeReloaded = new HashSet<ITexture>();
	/** The running preparation of each texture in mTexturesToBeReloaded that is reloaded with prepared pixels. */
	private final HashMap<ITexture, AtlasPixelLoader> mReloadPreparations = new HashMap<ITexture, AtlasPixelLoader>();
	/** Every submitted preparation that holds or may still produce pixels, including cancelled ones still running. */
	private final ArrayList<AtlasPixelLoader> mPreparationsInFlight = new ArrayList<AtlasPixelLoader>();

	// ===========================================================
	// Constructors
	// ===========================================================

	/**
	 * Prepares atlas pixels on an own low-priority worker thread, created on first use.
	 */
	public TextureManager() {
		this(null, TextureManager.UPLOAD_BYTES_PER_FRAME_DEFAULT, TextureManager.PREPARED_BYTES_IN_FLIGHT_DEFAULT);
	}

	/**
	 * @param pPreparationExecutor runs the pixel preparations (one at a time is enough), or {@code null} for an own worker thread.
	 * @param pUploadBytesPerFrame bytes of prepared pixels uploaded per frame (at least one texture always is).
	 * @param pPreparedBytesInFlight estimated bytes of pixels being prepared or waiting for upload (at least one preparation always runs).
	 */
	public TextureManager(final Executor pPreparationExecutor, final long pUploadBytesPerFrame, final long pPreparedBytesInFlight) {
		this.mPreparationExecutor = pPreparationExecutor;
		this.mUploadBytesPerFrame = pUploadBytesPerFrame;
		this.mPreparedBytesInFlight = pPreparedBytesInFlight;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public synchronized void onCreate() {
	}

	public synchronized void onReload() {
		this.mReloading = true;
		/* Every texture queued below prepares its pixels anew. */
		this.cancelPreparations();
		final HashSet<ITexture> managedTextures = this.mTexturesManaged;
		if(!managedTextures.isEmpty()) {
			for(final ITexture texture : managedTextures) { // TODO Can the use of the iterator be avoided somehow?
				texture.setNotLoadedToHardware();
			}
		}

		// BEGIN osu!droid modified: exclude textures scheduled for unloading. Re-adding them would cancel their unload.
		if(!this.mTexturesLoaded.isEmpty()) {
			for(final ITexture texture : this.mTexturesLoaded) {
				if(!this.mTexturesToBeUnloaded.contains(texture)) {
					this.mTexturesToBeLoaded.add(texture);
				}
			}
			this.mTexturesLoaded.clear();
		}
		// END osu!droid modified

		if(!this.mTexturesToBeUnloaded.isEmpty()) {
			this.mTexturesManaged.removeAll(this.mTexturesToBeUnloaded); // TODO Check if removeAll uses iterator internally!
			/* Their names are gone with the old context; they only release what is not GL. */
			for(int i = 0; i < this.mTexturesToBeUnloaded.size(); i++) {
				this.mTexturesToBeUnloaded.get(i).onDroppedWithoutUnload();
			}
			this.mTexturesToBeUnloaded.clear();
		}
	}

	/**
	 * Uploads every loaded texture again while the GL context stays alive (e.g. after a skin
	 * change). Unlike {@link #onReload()}, which is for a lost context, the old texture names are
	 * deleted by {@link #updateTextures()} before the upload, and textures already queued
	 * for unloading stay queued.
	 * <p>
	 * An atlas whose pixels can be prepared in the background keeps its old texture loaded and drawn
	 * until the new pixels are ready; they are then uploaded into a new texture name within the upload
	 * budget of a frame. Everything else is reloaded in the next frame. A request while a
	 * reload is still being prepared restarts that preparation (the cancelled one stops at its next
	 * source, or is skipped if it has not started yet), so the reload always shows the sources'
	 * content at the time of the latest request.
	 */
	public synchronized void reloadLoadedTextures() {
		this.mReloading = true;
		final ArrayList<ITexture> texturesLoaded = this.mTexturesLoaded;
		for(int i = texturesLoaded.size() - 1; i >= 0; i--) {
			final ITexture texture = texturesLoaded.get(i);
			if(!this.mTexturesToBeUnloaded.contains(texture)) {
				texture.setUpdateOnHardwareNeeded(true);
				this.cancelReloadPreparation(texture);
				this.mTexturesToBeReloaded.add(texture);
			}
		}
	}

	public synchronized void onDestroy() {
		this.cancelPreparations();
		final HashSet<ITexture> managedTextures = this.mTexturesManaged;
		for(final ITexture texture : managedTextures) { // TODO Can the use of the iterator be avoided somehow?
			texture.setNotLoadedToHardware();
		}

		this.mTexturesToBeLoaded.clear();
		this.notifyIfNoLoadPending();
		if(this.mPreparationExecutorOwned) {
			/* Injected executors belong to the caller; only the own worker is stopped. It is recreated on demand. */
			((ExecutorService)this.mPreparationExecutor).shutdown();
			this.mPreparationExecutor = null;
			this.mPreparationExecutorOwned = false;
		}
		this.mTexturesLoaded.clear();
		this.mTexturesManaged.clear();
		this.mTexturesMapped.clear();
	}

	public synchronized boolean hasMappedTexture(final String pID) {
		if(pID == null) {
			throw new IllegalArgumentException("pID must not be null!");
		}
		return this.mTexturesMapped.containsKey(pID);
	}

	public synchronized ITexture getMappedTexture(final String pID) {
		if(pID == null) {
			throw new IllegalArgumentException("pID must not be null!");
		}
		return this.mTexturesMapped.get(pID);
	}

	public synchronized void addMappedTexture(final String pID, final ITexture pTexture) throws IllegalArgumentException {
		if(pID == null) {
			throw new IllegalArgumentException("pID must not be null!");
		} else if(pTexture == null) {
			throw new IllegalArgumentException("pTexture must not be null!");
		} else if(this.mTexturesMapped.containsKey(pID)) {
			throw new IllegalArgumentException("Collision for pID: '" + pID + "'.");
		}
		this.mTexturesMapped.put(pID, pTexture);
	}

	public synchronized ITexture removedMappedTexture(final String pID) {
		if(pID == null) {
			throw new IllegalArgumentException("pID must not be null!");
		}
		return this.mTexturesMapped.remove(pID);
	}

	/**
	 * @param pTexture the {@link ITexture} to be loaded before the very next frame is drawn (Or prevent it from being unloaded then).
	 * @return <code>true</code> when the {@link ITexture} was previously not managed by this {@link TextureManager}, <code>false</code> if it was already managed.
	 */
	public synchronized boolean loadTexture(final ITexture pTexture) {
		if(pTexture == null) {
			throw new IllegalArgumentException("pTexture must not be null!");
		}

		if(this.mTexturesManaged.contains(pTexture)) {
			/* Just make sure it doesn't get deleted. */
			this.mTexturesToBeUnloaded.remove(pTexture);
			return false;
		} else {
			this.mTexturesManaged.add(pTexture);
			this.mTexturesToBeLoaded.add(pTexture);
			return true;
		}
	}

	/**
	 * Must be called from the GL-{@link Thread}.
	 *
	 * @param pTexture the {@link ITexture} to be loaded right now, if it is not loaded.
	 * @return <code>true</code> when the {@link ITexture} was previously not managed by this {@link TextureManager}, <code>false</code> if it was already managed.
	 */
	public synchronized boolean loadTextureNow(final ITexture pTexture) throws IOException {
		if(pTexture == null) {
			throw new IllegalArgumentException("pTexture must not be null!");
		}

		this.cancelPreparation(pTexture);
		if(!pTexture.isLoadedToHardware()) {
			pTexture.loadToHardware();
		} else if(pTexture.isUpdateOnHardwareNeeded()) {
			pTexture.reloadToHardware();
		}

		if(this.mTexturesManaged.contains(pTexture)) {
			/* Just make sure it doesn't get deleted. */
			this.mTexturesToBeUnloaded.remove(pTexture);
			if(this.mTexturesToBeLoaded.remove(pTexture)) {
				this.mTexturesLoaded.add(pTexture);
				this.notifyIfNoLoadPending();
			}
			return false;
		} else {
			this.mTexturesManaged.add(pTexture);
			this.mTexturesLoaded.add(pTexture);
			return true;
		}
	}

	/**
	 * @param pTexture the {@link ITexture} to be unloaded before the very next frame is drawn (Or prevent it from being loaded then).
	 * @return <code>true</code> when the {@link ITexture} was already managed by this {@link TextureManager}, <code>false</code> if it was not managed.
	 */
	public synchronized boolean unloadTexture(final ITexture pTexture) {
		if(pTexture == null) {
			throw new IllegalArgumentException("pTexture must not be null!");
		}

		if(this.mTexturesManaged.contains(pTexture)) {
			/* If the Texture is loaded, unload it.
			 * If the Texture is about to be loaded, stop it from being loaded. */
			if(this.mTexturesLoaded.contains(pTexture)) {
				/* Queued once, however often it is unloaded before the queue runs. */
				if(!this.mTexturesToBeUnloaded.contains(pTexture)) {
					this.mTexturesToBeUnloaded.add(pTexture);
				}
				/* Its pending reload is not prepared any more; the request itself stays until the unload happens,
				 * so loadTexture rescuing it from the unload still reloads it the requested way. */
				this.cancelReloadPreparation(pTexture);
			} else if(this.mTexturesToBeLoaded.remove(pTexture)) {
				this.mTexturesManaged.remove(pTexture);
				/* Never uploaded on this context, so there is nothing to unload from hardware. */
				pTexture.onDroppedWithoutUnload();
				this.cancelPreparation(pTexture);
				this.notifyIfNoLoadPending();
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Must be called from the GL-{@link Thread}.
	 *
	 * @param pTexture the {@link ITexture} to be unloaded right now, if it is loaded.
	 * @return <code>true</code> when the {@link ITexture} was already managed by this {@link TextureManager}, <code>false</code> if it was not managed.
	 */
	public synchronized boolean unloadTextureNow(final ITexture pTexture) {
		if(pTexture == null) {
			throw new IllegalArgumentException("pTexture must not be null!");
		} else if(pTexture.isLoadedToHardware()) {
			pTexture.unloadFromHardware();
		}

		if(this.mTexturesManaged.contains(pTexture)) {
			/* Just make sure it doesn't get loaded. */
			this.mTexturesLoaded.remove(pTexture);
			this.mTexturesToBeLoaded.remove(pTexture);
			this.cancelPreparation(pTexture);
			this.notifyIfNoLoadPending();

			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean updateTextures() {
		final HashSet<ITexture> texturesManaged = this.mTexturesManaged;
		final ArrayList<ITexture> texturesLoaded = this.mTexturesLoaded;
		final ArrayList<ITexture> texturesToBeLoaded = this.mTexturesToBeLoaded;
		final ArrayList<ITexture> texturesToBeUnloaded = this.mTexturesToBeUnloaded;

		this.purgeFinishedPreparations();
		long uploadedBytes = 0;

		/* First load pending Textures, oldest first. An atlas whose pixels can be prepared in the background
		 * stays queued (not loaded, so it is not drawn) until they are ready; then only its upload runs here,
		 * within the upload budget of the frame. Everything else is loaded right away. Loads go
		 * before reloads: a reloaded texture is still drawn meanwhile, a queued one is missing until loaded. */
		boolean loadsWaiting = false;
		int texturesLoadedCount = 0;
		for(int i = 0; i < texturesToBeLoaded.size(); ) {
			final ITexture textureToBeLoaded = texturesToBeLoaded.get(i);
			AtlasPixelLoader preparation = null;
			if(!textureToBeLoaded.isLoadedToHardware()) {
				preparation = this.mPreparations.get(textureToBeLoaded);
				if(preparation == null) {
					preparation = this.startPreparation(textureToBeLoaded, this.mPreparations);
				}
				if(preparation == TextureManager.WAITING) {
					loadsWaiting = true;
					i++;
					continue;
				}
				if(preparation != null) {
					if(preparation.isReady()) {
						final long bytes = preparation.getBytes();
						if(uploadedBytes > 0 && uploadedBytes + bytes > this.mUploadBytesPerFrame) {
							/* Over the budget of this frame: uploaded in one of the next. */
							i++;
							continue;
						}
						uploadedBytes += bytes;
					} else if(!preparation.isFailed()) {
						/* Still being prepared. */
						i++;
						continue;
					}
					/* Ready, or failed: then it is loaded on the synchronous path. */
					this.mPreparations.remove(textureToBeLoaded);
					this.mPreparationsInFlight.remove(preparation);
				}
			}

			texturesToBeLoaded.remove(i);
			texturesLoadedCount++;
			if(!textureToBeLoaded.isLoadedToHardware()) {
				final boolean usesPreparation = preparation != null && preparation.isReady();
				try {
					if(usesPreparation) {
						((BitmapTextureAtlas) textureToBeLoaded).usePreparedPixels(preparation);
					}
					textureToBeLoaded.loadToHardware();
				} catch (final IOException e) {
					Debug.e(e);
				} finally {
					if(usesPreparation) {
						((BitmapTextureAtlas) textureToBeLoaded).usePreparedPixels(null);
					}
				}
			}
			texturesLoaded.add(textureToBeLoaded);
		}

		this.notifyIfNoLoadPending();

		/* Then reload Textures that need to be updated. A reload requested by reloadLoadedTextures of an atlas
		 * whose pixels can be prepared in the background keeps the old texture until they are ready, then uploads
		 * them within what is left of the upload budget of the frame. No re-preparation starts while a queued load
		 * waits for room in flight. Everything else (also an atlas whose sources changed) is reloaded right away. */
		for(int i = texturesLoaded.size() - 1; i >= 0; i--) {
			final ITexture textureToBeReloaded = texturesLoaded.get(i);
			if(!textureToBeReloaded.isUpdateOnHardwareNeeded()) {
				/* Written anew outside this manager meanwhile: the requested reload is done. */
				if(!this.mTexturesToBeReloaded.isEmpty() && this.mTexturesToBeReloaded.remove(textureToBeReloaded)) {
					this.cancelReloadPreparation(textureToBeReloaded);
				}
				continue;
			}
			if(texturesToBeUnloaded.contains(textureToBeReloaded)) {
				/* A texture unloaded below in this frame is not uploaded again first; loading it later writes it anew. */
				continue;
			}
			AtlasPixelLoader preparation = null;
			if(this.mTexturesToBeReloaded.contains(textureToBeReloaded)) {
				preparation = this.getReloadPreparation(textureToBeReloaded, !loadsWaiting);
				if(preparation == TextureManager.WAITING) {
					/* The old texture is drawn meanwhile. */
					continue;
				}
				if(preparation != null) {
					if(preparation.isReady()) {
						final long bytes = preparation.getBytes();
						if(uploadedBytes > 0 && uploadedBytes + bytes > this.mUploadBytesPerFrame) {
							/* Over the budget of this frame: reloaded in one of the next. */
							continue;
						}
						uploadedBytes += bytes;
					}
					/* Ready, or failed: then it is reloaded on the synchronous path. */
					this.mReloadPreparations.remove(textureToBeReloaded);
					this.mPreparationsInFlight.remove(preparation);
				}
				this.mTexturesToBeReloaded.remove(textureToBeReloaded);
			}

			final boolean usesPreparation = preparation != null && preparation.isReady();
			try {
				if(usesPreparation) {
					((BitmapTextureAtlas) textureToBeReloaded).usePreparedPixels(preparation);
				}
				textureToBeReloaded.reloadToHardware();
			} catch (final IOException e) {
				Debug.e(e);
			} finally {
				if(usesPreparation) {
					((BitmapTextureAtlas) textureToBeReloaded).usePreparedPixels(null);
				}
			}
		}

		/* Then unload pending Textures. */
		final int texturesToBeUnloadedCount = texturesToBeUnloaded.size();

		if(texturesToBeUnloadedCount > 0) {
			for(int i = texturesToBeUnloadedCount - 1; i >= 0; i--) {
				final ITexture textureToBeUnloaded = texturesToBeUnloaded.remove(i);
				if(textureToBeUnloaded.isLoadedToHardware()) {
					textureToBeUnloaded.unloadFromHardware();
				}
				texturesLoaded.remove(textureToBeUnloaded);
				texturesManaged.remove(textureToBeUnloaded);
				this.mTexturesToBeReloaded.remove(textureToBeUnloaded);
			}
		}

		/* Finally invoke the GC if anything has changed. */
		if((texturesLoadedCount > 0) || (texturesToBeUnloadedCount > 0)) {
			System.gc();
		}

		/* A frame that uploaded prepared pixels took longer than usual, too. */
		final boolean reloading = this.mReloading;
		this.mReloading = false;
		return reloading || uploadedBytes > 0;
	}

	/**
	 * @return whether {@code pTexture} is queued for loading and not on the GPU yet, e.g. while its pixels are prepared in the background.
	 */
	public synchronized boolean isTextureLoadPending(final ITexture pTexture) {
		return this.mTexturesToBeLoaded.contains(pTexture);
	}

	/**
	 * @return whether any texture is queued for loading, e.g. while its pixels are prepared in the background.
	 */
	public synchronized boolean hasPendingTextureLoads() {
		return !this.mTexturesToBeLoaded.isEmpty();
	}

	/**
	 * Nothing needs to wait for reloads; tests use this to observe them.
	 *
	 * @return whether a reload requested by {@link #reloadLoadedTextures()} has not reached the GPU yet. The
	 * old texture stays loaded and drawn meanwhile, so unlike {@link #hasPendingTextureLoads()} nothing is missing.
	 */
	public synchronized boolean hasPendingTextureReloads() {
		return !this.mTexturesToBeReloaded.isEmpty();
	}

	/**
	 * Blocks until no texture is queued for loading any more, or {@code pTimeoutMillis} have passed. Must not
	 * be called from the GL thread, which does the loading.
	 *
	 * @return {@code true} if nothing is pending, {@code false} if the timeout was hit first.
	 */
	public synchronized boolean awaitPendingTextureLoads(final long pTimeoutMillis) throws InterruptedException {
		final long deadline = System.nanoTime() + pTimeoutMillis * 1000000L;
		while(!this.mTexturesToBeLoaded.isEmpty()) {
			final long remainingMillis = (deadline - System.nanoTime()) / 1000000L;
			if(remainingMillis <= 0) {
				return false;
			}
			this.wait(remainingMillis);
		}
		return true;
	}

	/** Wakes {@link #awaitPendingTextureLoads(long)} once the load queue is empty. */
	private void notifyIfNoLoadPending() {
		if(this.mTexturesToBeLoaded.isEmpty()) {
			this.notifyAll();
		}
	}

	/**
	 * @return the number of pixel preparations running, queued or waiting for upload, including cancelled
	 * ones that have not finished running yet.
	 */
	public synchronized int getPreparationsInFlight() {
		this.purgeFinishedPreparations();
		return this.mPreparationsInFlight.size();
	}

	/**
	 * Submits the background preparation of {@code pTexture}'s pixels and records it in {@code pPreparations}.
	 *
	 * @return the preparation (failed if it could not be submitted); {@code null} if the texture cannot be
	 * prepared in the background, so it loads synchronously; {@link #WAITING} while too many pixels are in flight.
	 */
	private AtlasPixelLoader startPreparation(final ITexture pTexture, final HashMap<ITexture, AtlasPixelLoader> pPreparations) {
		if(!(pTexture instanceof BitmapTextureAtlas)) {
			return null;
		}
		final BitmapTextureAtlas atlas = (BitmapTextureAtlas) pTexture;
		if(!atlas.canPreparePixels()) {
			return null;
		}

		/* Checked before anything is allocated: a waiting texture asks again every frame. */
		final ArrayList<AtlasPixelLoader> inFlight = this.mPreparationsInFlight;
		if(!inFlight.isEmpty()) {
			long bytesInFlight = AtlasPixelLoader.estimateBytes(atlas.getWidth(), atlas.getHeight());
			for(int i = inFlight.size() - 1; i >= 0; i--) {
				bytesInFlight += inFlight.get(i).getEstimatedBytes();
			}
			if(bytesInFlight > this.mPreparedBytesInFlight) {
				return TextureManager.WAITING;
			}
		}

		final AtlasPixelLoader preparation = atlas.createPixelLoaderUnchecked();
		try {
			this.getPreparationExecutor().execute(preparation);
		} catch (final RejectedExecutionException e) {
			preparation.fail();
			return preparation;
		}
		pPreparations.put(pTexture, preparation);
		inFlight.add(preparation);
		return preparation;
	}

	/**
	 * The preparation a requested reload of {@code pTexture} uses in this frame.
	 *
	 * @param pMayStart whether a new preparation may be submitted now (not while queued loads wait for room in flight).
	 * @return {@link #WAITING} to keep the old texture for now; {@code null} to reload synchronously; otherwise a
	 * ready or failed preparation.
	 */
	private AtlasPixelLoader getReloadPreparation(final ITexture pTexture, final boolean pMayStart) {
		AtlasPixelLoader preparation = this.mReloadPreparations.get(pTexture);
		if(preparation != null && !((BitmapTextureAtlas) pTexture).isPreparationCurrent(preparation)) {
			/* Sources were added or moved meanwhile: they must not wait for the preparation. */
			this.cancelReloadPreparation(pTexture);
			return null;
		}
		if(preparation == null) {
			if(!pMayStart && pTexture instanceof BitmapTextureAtlas && ((BitmapTextureAtlas) pTexture).canPreparePixels()) {
				return TextureManager.WAITING;
			}
			preparation = this.startPreparation(pTexture, this.mReloadPreparations);
			if(preparation == null || preparation == TextureManager.WAITING) {
				return preparation;
			}
		}
		return (preparation.isReady() || preparation.isFailed()) ? preparation : TextureManager.WAITING;
	}

	/** Cancels the load or reload preparation of {@code pTexture} and forgets a requested reload. */
	private void cancelPreparation(final ITexture pTexture) {
		this.cancelPreparation(this.mPreparations.remove(pTexture));
		this.cancelReloadPreparation(pTexture);
		this.mTexturesToBeReloaded.remove(pTexture);
	}

	/** Cancels the reload preparation of {@code pTexture}; the reload itself stays requested. */
	private void cancelReloadPreparation(final ITexture pTexture) {
		this.cancelPreparation(this.mReloadPreparations.remove(pTexture));
	}

	private void cancelPreparation(final AtlasPixelLoader preparation) {
		if(preparation != null) {
			preparation.cancel();
			/* Stays in flight until it has finished running, as it may still be loading pixels. */
			if(preparation.isFinished()) {
				this.mPreparationsInFlight.remove(preparation);
			}
		}
	}

	private void cancelPreparations() {
		for(final AtlasPixelLoader preparation : this.mPreparations.values()) {
			preparation.cancel();
		}
		this.mPreparations.clear();
		for(final AtlasPixelLoader preparation : this.mReloadPreparations.values()) {
			preparation.cancel();
		}
		this.mReloadPreparations.clear();
		/* After a context loss every texture is loaded anew from its sources; after destroy none is. */
		this.mTexturesToBeReloaded.clear();
		this.purgeFinishedPreparations();
	}

	/** Forgets cancelled preparations that have finished running. */
	private void purgeFinishedPreparations() {
		final ArrayList<AtlasPixelLoader> inFlight = this.mPreparationsInFlight;
		for(int i = inFlight.size() - 1; i >= 0; i--) {
			final AtlasPixelLoader preparation = inFlight.get(i);
			if(preparation.isCancelled() && preparation.isFinished()) {
				inFlight.remove(i);
			}
		}
	}

	private Executor getPreparationExecutor() {
		if(this.mPreparationExecutor == null) {
			this.mPreparationExecutor = TextureManager.createPreparationExecutor();
			this.mPreparationExecutorOwned = true;
		}
		return this.mPreparationExecutor;
	}

	/** One low-priority daemon thread: preparations run one after another, in submission order. */
	private static ExecutorService createPreparationExecutor() {
		return Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable pRunnable) {
				final Thread thread = new Thread(new Runnable() {
					@Override
					public void run() {
						Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
						pRunnable.run();
					}
				}, "TexturePreparation");
				thread.setDaemon(true);
				return thread;
			}
		});
	}

	public synchronized ITexture getTexture(final String pID, final AssetManager pAssetManager, final String pAssetPath) throws IOException {
		return this.getTexture(pID, pAssetManager, pAssetPath, TextureOptions.DEFAULT);
	}

	public synchronized ITexture getTexture(final String pID, final AssetManager pAssetManager, final String pAssetPath, final TextureOptions pTextureOptions) throws IOException {
		if(this.hasMappedTexture(pID)) {
			return this.getMappedTexture(pID);
		} else {
			final ITexture texture = new BitmapTexture(this, new AssetInputStreamOpener(pAssetManager, pAssetPath), pTextureOptions);
			this.loadTexture(texture);
			this.addMappedTexture(pID, texture);

			return texture;
		}
	}

	public synchronized ITexture getTexture(final String pID, final IInputStreamOpener pInputStreamOpener) throws IOException {
		return this.getTexture(pID, pInputStreamOpener, TextureOptions.DEFAULT);
	}

	public synchronized ITexture getTexture(final String pID, final IInputStreamOpener pInputStreamOpener, final TextureOptions pTextureOptions) throws IOException {
		return this.getTexture(pID, pInputStreamOpener, BitmapTextureFormat.RGBA_8888, pTextureOptions);
	}

	public synchronized ITexture getTexture(final String pID, final IInputStreamOpener pInputStreamOpener, final BitmapTextureFormat pBitmapTextureFormat, final TextureOptions pTextureOptions) throws IOException {
		return this.getTexture(pID, pInputStreamOpener, pBitmapTextureFormat, pTextureOptions, true);
	}

	public synchronized ITexture getTexture(final String pID, final IInputStreamOpener pInputStreamOpener, final BitmapTextureFormat pBitmapTextureFormat, final TextureOptions pTextureOptions, final boolean pLoadToHardware) throws IOException {
		if(this.hasMappedTexture(pID)) {
			return this.getMappedTexture(pID);
		} else {
			final ITexture texture = new BitmapTexture(this, pInputStreamOpener, pBitmapTextureFormat, pTextureOptions);
			if(pLoadToHardware) {
				this.loadTexture(texture);
			}
			this.addMappedTexture(pID, texture);

			return texture;
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
