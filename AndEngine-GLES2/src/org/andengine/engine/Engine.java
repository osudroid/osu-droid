package org.andengine.engine;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.andengine.BuildConfig;
import org.andengine.audio.music.MusicFactory;
import org.andengine.audio.music.MusicManager;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.audio.sound.SoundManager;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.DrawHandlerList;
import org.andengine.engine.handler.IDrawHandler;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.UpdateHandlerList;
import org.andengine.engine.handler.runnable.RunnableHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.entity.scene.Scene;
import org.andengine.graphics.DrawContext;
import org.andengine.graphics.batch.QuadBatcher;
import org.andengine.graphics.debug.RenderStats;
import org.andengine.graphics.gles.GlesLegacyRestorers;
import org.andengine.input.sensor.SensorDelay;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.AccelerationSensorOptions;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.sensor.location.ILocationListener;
import org.andengine.input.sensor.location.LocationProviderStatus;
import org.andengine.input.sensor.location.LocationSensorOptions;
import org.andengine.input.sensor.orientation.IOrientationListener;
import org.andengine.input.sensor.orientation.OrientationData;
import org.andengine.input.sensor.orientation.OrientationSensorOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.controller.ITouchController;
import org.andengine.input.touch.controller.ITouchEventCallback;
import org.andengine.input.touch.controller.MultiTouchController;
import org.andengine.input.touch.controller.SingleTouchController;
import org.andengine.graphics.text.FontFactory;
import org.andengine.graphics.text.FontManager;
import org.andengine.opengl.texture.TextureManager;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.util.debug.Debug;
import org.andengine.util.time.TimeConstants;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 12:21:31 - 08.03.2010
 */
public class Engine implements SensorEventListener, OnTouchListener, ITouchEventCallback, LocationListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final SensorDelay SENSORDELAY_DEFAULT = SensorDelay.GAME;
	private static final int UPDATEHANDLERS_CAPACITY_DEFAULT = 8;
	private static final int DRAWHANDLERS_CAPACITY_DEFAULT = 4;

	// BEGIN osu!droid modified - zero-latency touch: skip UI-thread sleep by default (matches old GLES1 engine behavior)
	/**
	 * How long (ms) to sleep the UI thread after each touch event.
	 * Default is 0 (no sleep) for minimum input latency.
	 * Matches the old AndEngine GLES1 behavior.
	 * <p>
	 * Marked {@code volatile} because it may be mutated from any thread (e.g. settings UI)
	 * and read by the UI thread inside {@link #onTouch}.
	 */
	public static volatile int INPUT_PAUSE_DURATION = 0;
	// END osu!droid modified

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean mRunning;
	private boolean mDestroyed;

	private long mLastTick;
	private float mSecondsElapsedTotal;

	private final EngineLock mEngineLock;

	/**
	 * Whether the update is released once the frame was swapped instead of right after drawing.
	 * Read once per frame.
	 */
	private volatile boolean mReleaseUpdateAfterSwap;
	/**
	 * Whether the last drawn frame still owes the update its release. Only accessed on the render thread,
	 * which calls both {@link #onDrawFrame(DrawContext)} and {@link #onFrameSwapped()}; the release itself
	 * happens under {@link #mEngineLock}.
	 */
	private boolean mUpdateReleasePending;

	private final UpdateThread mUpdateThread;
	private final RunnableHandler mUpdateThreadRunnableHandler = new RunnableHandler();

	private final EngineOptions mEngineOptions;
	protected final Camera mCamera;

	private ITouchController mTouchController;

	private final TextureManager mTextureManager = new TextureManager();
	private final FontManager mFontManager = new FontManager();
	private final DrawContext mDrawContext = new DrawContext();

	private final SoundManager mSoundManager;
	private final MusicManager mMusicManager;

	protected Scene mScene;

	private Vibrator mVibrator;

	private ILocationListener mLocationListener;
	private Location mLocation;

	private IAccelerationListener mAccelerationListener;
	private AccelerationData mAccelerationData;

	private IOrientationListener mOrientationListener;
	private OrientationData mOrientationData;

	private final UpdateHandlerList mUpdateHandlers = new UpdateHandlerList(Engine.UPDATEHANDLERS_CAPACITY_DEFAULT);
	private final DrawHandlerList mDrawHandlers = new DrawHandlerList(Engine.DRAWHANDLERS_CAPACITY_DEFAULT);

	protected int mSurfaceWidth = 1; // 1 to prevent accidental DIV/0
	private volatile boolean mIsLagging;
	protected int mSurfaceHeight = 1; // 1 to prevent accidental DIV/0

	// ===========================================================
	// Constructors
	// ===========================================================

	public Engine(final EngineOptions pEngineOptions) {
		/* Initialize Factory and Manager classes. */
		BitmapTextureAtlasTextureRegionFactory.reset();
		SoundFactory.onCreate();
		MusicFactory.onCreate();
		FontFactory.onCreate();
		this.mTextureManager.onCreate();
		this.mFontManager.onCreate();
		GlesLegacyRestorers.registerEngine(this);

		/* Apply EngineOptions. */
		this.mEngineOptions = pEngineOptions;
		if(this.mEngineOptions.hasEngineLock()) {
			this.mEngineLock = pEngineOptions.getEngineLock();
		} else {
			this.mEngineLock = new EngineLock(false);
		}
		this.mCamera = pEngineOptions.getCamera();

		/* Touch. */
		if(this.mEngineOptions.getTouchOptions().needsMultiTouch()) {
			this.setTouchController(new MultiTouchController());
		} else {
			this.setTouchController(new SingleTouchController());
		}

		/* Audio. */
		if(this.mEngineOptions.getAudioOptions().needsSound()) {
			this.mSoundManager = new SoundManager(this.mEngineOptions.getAudioOptions().getSoundOptions().getMaxSimultaneousStreams());
		} else {
			this.mSoundManager = null;
		}
		if(this.mEngineOptions.getAudioOptions().needsMusic()) {
			this.mMusicManager = new MusicManager();
		} else {
			this.mMusicManager = null;
		}

		/* Start the UpdateThread. */
		if(this.mEngineOptions.hasUpdateThread()) {
			this.mUpdateThread = this.mEngineOptions.getUpdateThread();
		} else {
			this.mUpdateThread = new UpdateThread();
		}
		this.mUpdateThread.setEngine(this);
	}

	public void startUpdateThread() throws IllegalThreadStateException {
		this.mUpdateThread.start();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public synchronized boolean isRunning() {
		return this.mRunning;
	}

	public synchronized void start() {
		if(!this.mRunning) {
			this.mLastTick = System.nanoTime();
			this.mRunning = true;
		}
	}

	public synchronized void stop() {
		if(this.mRunning) {
			this.mRunning = false;
		}
	}

	/**
	 * The {@link EngineLock} can be used to {@link EngineLock#lock()}/{@link EngineLock#unlock()} on, to ensure the code in between runs mutually exclusive to the {@link UpdateThread} and the GL{@link Thread}.
	 * When the caller already is on the {@link UpdateThread} or the GL-{@link Thread}, that code is executed immediately.
	 * @return the {@link EngineLock} the {@link Engine} locks on to ensure mutually exclusivity to the {@link UpdateThread} and the GL{@link Thread}.
	 */
	public EngineLock getEngineLock() {
		return this.mEngineLock;
	}

	public Scene getScene() {
		return this.mScene;
	}

	public void setScene(final Scene pScene) {
		this.mScene = pScene;
	}

	public EngineOptions getEngineOptions() {
		return this.mEngineOptions;
	}

	public Camera getCamera() {
		return this.mCamera;
	}

	public float getSecondsElapsedTotal() {
		return this.mSecondsElapsedTotal;
	}

	public void setSurfaceSize(final int pSurfaceWidth, final int pSurfaceHeight) {
		this.mSurfaceWidth = pSurfaceWidth;
		this.mSurfaceHeight = pSurfaceHeight;
		this.onUpdateCameraSurface();
	}

	protected void onUpdateCameraSurface() {
		this.mCamera.setSurfaceSize(0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
	}

	public int getSurfaceWidth() {
		return this.mSurfaceWidth;
	}

	public int getSurfaceHeight() {
		return this.mSurfaceHeight;
	}

	public ITouchController getTouchController() {
		return this.mTouchController;
	}

	public void setTouchController(final ITouchController pTouchController) {
		this.mTouchController = pTouchController;
		this.mTouchController.setTouchEventCallback(this);
	}

	public AccelerationData getAccelerationData() {
		return this.mAccelerationData;
	}

	public OrientationData getOrientationData() {
		return this.mOrientationData;
	}

	public TextureManager getTextureManager() {
		return this.mTextureManager;
	}

	public FontManager getFontManager() {
		return this.mFontManager;
	}

	/** The draw context the renderer passes to every frame. GL thread only. */
	public DrawContext getDrawContext() {
		return this.mDrawContext;
	}

	public SoundManager getSoundManager() throws IllegalStateException {
		if(this.mSoundManager != null) {
			return this.mSoundManager;
		} else {
			throw new IllegalStateException("To enable the SoundManager, check the EngineOptions!");
		}
	}

	public MusicManager getMusicManager() throws IllegalStateException {
		if(this.mMusicManager != null) {
			return this.mMusicManager;
		} else {
			throw new IllegalStateException("To enable the MusicManager, check the EngineOptions!");
		}
	}

	public void registerUpdateHandler(final IUpdateHandler pUpdateHandler) {
		this.mUpdateHandlers.add(pUpdateHandler);
	}

	public void unregisterUpdateHandler(final IUpdateHandler pUpdateHandler) {
		this.mUpdateHandlers.remove(pUpdateHandler);
	}

	public void clearUpdateHandlers() {
		this.mUpdateHandlers.clear();
	}

	public void registerDrawHandler(final IDrawHandler pDrawHandler) {
		this.mDrawHandlers.add(pDrawHandler);
	}

	public void unregisterDrawHandler(final IDrawHandler pDrawHandler) {
		this.mDrawHandlers.remove(pDrawHandler);
	}

	public void clearDrawHandlers() {
		this.mDrawHandlers.clear();
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onAccuracyChanged(final Sensor pSensor, final int pAccuracy) {
		if(this.mRunning) {
			switch(pSensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					if(this.mAccelerationData != null) {
						this.mAccelerationData.setAccuracy(pAccuracy);
						this.mAccelerationListener.onAccelerationAccuracyChanged(this.mAccelerationData);
					} else if(this.mOrientationData != null) {
						this.mOrientationData.setAccelerationAccuracy(pAccuracy);
						this.mOrientationListener.onOrientationAccuracyChanged(this.mOrientationData);
					}
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					this.mOrientationData.setMagneticFieldAccuracy(pAccuracy);
					this.mOrientationListener.onOrientationAccuracyChanged(this.mOrientationData);
					break;
			}
		}
	}

	@Override
	public void onSensorChanged(final SensorEvent pEvent) {
		if(this.mRunning) {
			switch(pEvent.sensor.getType()) {
				case Sensor.TYPE_ACCELEROMETER:
					if(this.mAccelerationData != null) {
						this.mAccelerationData.setValues(pEvent.values);
						this.mAccelerationListener.onAccelerationChanged(this.mAccelerationData);
					} else if(this.mOrientationData != null) {
						this.mOrientationData.setAccelerationValues(pEvent.values);
						this.mOrientationListener.onOrientationChanged(this.mOrientationData);
					}
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					this.mOrientationData.setMagneticFieldValues(pEvent.values);
					this.mOrientationListener.onOrientationChanged(this.mOrientationData);
					break;
			}
		}
	}

	@Override
	public void onLocationChanged(final Location pLocation) {
		if(this.mLocation == null) {
			this.mLocation = pLocation;
		} else {
			if(pLocation == null) {
				this.mLocationListener.onLocationLost();
			} else {
				this.mLocation = pLocation;
				this.mLocationListener.onLocationChanged(pLocation);
			}
		}
	}

	@Override
	public void onProviderDisabled(final String pProvider) {
		this.mLocationListener.onLocationProviderDisabled();
	}

	@Override
	public void onProviderEnabled(final String pProvider) {
		this.mLocationListener.onLocationProviderEnabled();
	}

	@Override
	public void onStatusChanged(final String pProvider, final int pStatus, final Bundle pExtras) {
		switch(pStatus) {
			case LocationProvider.AVAILABLE:
				this.mLocationListener.onLocationProviderStatusChanged(LocationProviderStatus.AVAILABLE, pExtras);
				break;
			case LocationProvider.OUT_OF_SERVICE:
				this.mLocationListener.onLocationProviderStatusChanged(LocationProviderStatus.OUT_OF_SERVICE, pExtras);
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				this.mLocationListener.onLocationProviderStatusChanged(LocationProviderStatus.TEMPORARILY_UNAVAILABLE, pExtras);
				break;
		}
	}

	// BEGIN osu!droid modified - zero-latency touch: only sleep if INPUT_PAUSE_DURATION is explicitly set non-zero
	@android.annotation.SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouch(final View pView, final MotionEvent pSurfaceMotionEvent) {
		if(this.mRunning) {
			this.mTouchController.onHandleMotionEvent(pSurfaceMotionEvent);
			try {
				/*
				 * As a human cannot interact 1000x per second, we pause the
				 * UI-Thread for a little.
				 * Default INPUT_PAUSE_DURATION is 0, so no sleep occurs (minimum latency).
				 */
				if (INPUT_PAUSE_DURATION != 0) {
					Thread.sleep(INPUT_PAUSE_DURATION);
				}
			} catch (final InterruptedException e) {
				Debug.e(e);
			}
			return true;
		} else {
			return false;
		}
	}
	// END osu!droid modified

	@Override
	public boolean onTouchEvent(final TouchEvent pSurfaceTouchEvent) {
		/*
		 * Let the engine determine which scene and camera this event should be
		 * handled by.
		 */
		final Scene scene = this.getSceneFromSurfaceTouchEvent(pSurfaceTouchEvent);
		final Camera camera = this.getCameraFromSurfaceTouchEvent(pSurfaceTouchEvent);

		this.convertSurfaceToSceneTouchEvent(camera, pSurfaceTouchEvent);

		if(this.onTouchHUD(camera, pSurfaceTouchEvent)) {
			return true;
		} else {
			/* If HUD didn't handle it, Scene may handle it. */
			return this.onTouchScene(scene, pSurfaceTouchEvent);
		}
	}

	protected boolean onTouchHUD(final Camera pCamera, final TouchEvent pSceneTouchEvent) {
		if(pCamera.hasHUD()) {
			return pCamera.getHUD().onSceneTouchEvent(pSceneTouchEvent);
		} else {
			return false;
		}
	}

	protected boolean onTouchScene(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		if(pScene != null) {
			return pScene.onSceneTouchEvent(pSceneTouchEvent);
		} else {
			return false;
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void runOnUpdateThread(final Runnable pRunnable) {
		this.runOnUpdateThread(pRunnable, true);
	}

	/**
	 * This method is useful when you want to execute code on the {@link UpdateThread}, even though the Engine is paused.
	 *
	 * @param pRunnable the {@link Runnable} to be run on the {@link UpdateThread}.
	 * @param pOnlyWhenEngineRunning if <code>true</code>, the execution of the {@link Runnable} will be delayed until the next time {@link Engine#onUpdateUpdateHandlers(float)} is picked up, which is when {@link Engine#isRunning()} is <code>true</code>.
	 * 								 if <code>false</code>, the execution of the {@link Runnable} will happen as soon as possible on the {@link UpdateThread}, no matter what {@link Engine#isRunning()} is.
	 */
	public void runOnUpdateThread(final Runnable pRunnable, final boolean pOnlyWhenEngineRunning) {
		if(pOnlyWhenEngineRunning) {
			this.mUpdateThreadRunnableHandler.postRunnable(pRunnable);
		} else {
			this.mUpdateThread.postRunnable(pRunnable);
		}
	}

	/**
	 * @param pRunnable the {@link Runnable} to run mutually exclusive to the {@link UpdateThread} and the GL-{@link Thread}.
	 * When the caller already is on the {@link UpdateThread} or the GL-{@link Thread}, the {@link Runnable} is executed immediately.
	 * @see {@link Engine#getEngineLock()} to manually {@link EngineLock#lock()}/{@link EngineLock#unlock()} on, while avoiding creating a {@link Runnable}.
	 */
	public void runSafely(final Runnable pRunnable) {
		this.mEngineLock.lock();
		try {
			pRunnable.run();
		} finally {
			this.mEngineLock.unlock();
		}
	}

	public void onDestroy() {
		this.mEngineLock.lock();
		try {
			this.mDestroyed = true;
			this.mEngineLock.notifyCanUpdate();
		} finally {
			this.mEngineLock.unlock();
		}
		try {
			this.mUpdateThread.join();
		} catch (final InterruptedException e) {
			Debug.e("Could not join UpdateThread.", e);
			Debug.w("Trying to manually interrupt UpdateThread.");
			this.mUpdateThread.interrupt();
		}

		this.mTextureManager.onDestroy();
		this.mFontManager.onDestroy();
	}

	/**
	 * Explicitly reloads the textures while the GL context stays alive (e.g. after a skin change); the
	 * old texture names are deleted first. Buffers, shaders and the glyph atlas (not managed by the
	 * TextureManager) stay valid in a live context and are left alone.
	 */
	public void onReloadResources() {
		this.mTextureManager.reloadLoadedTextures();
	}

	protected Camera getCameraFromSurfaceTouchEvent(final TouchEvent pTouchEvent) {
		return this.getCamera();
	}

	protected Scene getSceneFromSurfaceTouchEvent(final TouchEvent pTouchEvent) {
		return this.mScene;
	}

	protected void convertSurfaceToSceneTouchEvent(final Camera pCamera, final TouchEvent pSurfaceTouchEvent) {
		pCamera.convertSurfaceToSceneTouchEvent(pSurfaceTouchEvent, this.mSurfaceWidth, this.mSurfaceHeight);
	}

	protected void convertSceneToSurfaceTouchEvent(final Camera pCamera, final TouchEvent pSurfaceTouchEvent) {
		pCamera.convertSceneToSurfaceTouchEvent(pSurfaceTouchEvent, this.mSurfaceWidth, this.mSurfaceHeight);
	}

	void onTickUpdate() throws InterruptedException {
		if(this.mRunning) {
			final long secondsElapsed = this.getNanosecondsElapsed();

			this.mEngineLock.lock();
			try {
				this.throwOnDestroyed();

				final long updateStart = System.nanoTime();
				this.onUpdate(secondsElapsed);
				RenderStats.onUpdate(System.nanoTime() - updateStart);

				this.throwOnDestroyed();

				this.mEngineLock.notifyCanDraw();
				this.mEngineLock.waitUntilCanUpdate();
			} finally {
				this.mEngineLock.unlock();
			}
		} else {
			this.mEngineLock.lock();
			try {
				this.throwOnDestroyed();

				this.mEngineLock.notifyCanDraw();
				this.mEngineLock.waitUntilCanUpdate();
			} finally {
				this.mEngineLock.unlock();
			}

			Thread.sleep(16);
		}
	}

	private void throwOnDestroyed() throws EngineDestroyedException {
		if(this.mDestroyed) {
			throw new EngineDestroyedException();
		}
	}

	public void onUpdate(final long pNanosecondsElapsed) throws InterruptedException {
		float pSecondsElapsed = pNanosecondsElapsed * TimeConstants.SECONDS_PER_NANOSECOND;

		this.mSecondsElapsedTotal += pSecondsElapsed;
		this.mLastTick += pNanosecondsElapsed;

		if (this.mIsLagging) {
			pSecondsElapsed = 16666666 * TimeConstants.SECONDS_PER_NANOSECOND;
		}

		this.mTouchController.onUpdate(pSecondsElapsed);
		this.onUpdateUpdateHandlers(pSecondsElapsed);
		this.onUpdateScene(pSecondsElapsed);
	}

	protected void onUpdateScene(final float pSecondsElapsed) {
		if(this.mScene != null) {
			this.mScene.onUpdate(pSecondsElapsed);
		}
	}

	protected void onUpdateUpdateHandlers(final float pSecondsElapsed) {
		this.mUpdateThreadRunnableHandler.onUpdate(pSecondsElapsed);
		this.mUpdateHandlers.onUpdate(pSecondsElapsed);
		this.getCamera().onUpdate(pSecondsElapsed);
	}

	protected void onUpdateDrawHandlers(final DrawContext pDrawContext, final Camera pCamera) {
		this.mDrawHandlers.onDraw(pDrawContext, pCamera);
	}

	public void onDrawFrame(final DrawContext pDrawContext) throws InterruptedException {
		final EngineLock engineLock = this.mEngineLock;

		engineLock.lock();
		try {
			engineLock.waitUntilCanDraw();
			this.mUpdateReleasePending = false;
			final boolean releaseAfterSwap = this.mReleaseUpdateAfterSwap;
			RenderStats.beginDraw();

			this.mIsLagging = false;
			this.mIsLagging |= this.mTextureManager.updateTextures();
			this.mIsLagging |= this.mFontManager.updateFonts();

			this.onUpdateDrawHandlers(pDrawContext, this.mCamera);
			this.onDrawScene(pDrawContext, this.mCamera);
			QuadBatcher.flushFrame();
			RenderStats.endDraw();

			if(releaseAfterSwap) {
				this.mUpdateReleasePending = true;
			} else {
				engineLock.notifyCanUpdate();
			}
		} finally {
			engineLock.unlock();
		}
	}

	/**
	 * Tells the {@link Engine} that the frame it just drew was swapped. Releases the update if
	 * {@link #onDrawFrame(DrawContext)} deferred it. A frame whose swap is never reported keeps the update
	 * parked until the next drawn frame. Must be called on the thread that calls
	 * {@link #onDrawFrame(DrawContext)}.
	 */
	public void onFrameSwapped() {
		if(!this.mUpdateReleasePending) {
			return;
		}

		final EngineLock engineLock = this.mEngineLock;

		engineLock.lock();
		try {
			this.mUpdateReleasePending = false;
			engineLock.notifyCanUpdate();
		} finally {
			engineLock.unlock();
		}
	}

	/**
	 * With <code>true</code> the update only runs after {@link #onFrameSwapped()}, so it cannot race the
	 * buffer swap. The value is read once at the start of each frame. A frame whose swap is never reported
	 * keeps the update parked until the next drawn frame.
	 */
	public void setReleaseUpdateAfterSwap(final boolean pReleaseUpdateAfterSwap) {
		this.mReleaseUpdateAfterSwap = pReleaseUpdateAfterSwap;
	}

	protected void onDrawScene(final DrawContext pDrawContext, final Camera pCamera) {
		if(this.mScene != null) {
			this.mScene.onDraw(pDrawContext, pCamera);
		}

		pCamera.onDrawHUD(pDrawContext);
	}

	private long getNanosecondsElapsed() {
		final long now = System.nanoTime();

		return now - this.mLastTick;
	}

	public boolean enableVibrator(final Context pContext) {
		this.mVibrator = (Vibrator) pContext.getSystemService(Context.VIBRATOR_SERVICE);
		return this.mVibrator != null;
	}

	public void vibrate(final long pMilliseconds) throws IllegalStateException {
		if(this.mVibrator != null) {
			this.mVibrator.vibrate(pMilliseconds);
		} else {
			throw new IllegalStateException("You need to enable the Vibrator before you can use it!");
		}
	}

	public void vibrate(final long[] pPattern, final int pRepeat) throws IllegalStateException {
		if(this.mVibrator != null) {
			this.mVibrator.vibrate(pPattern, pRepeat);
		} else {
			throw new IllegalStateException("You need to enable the Vibrator before you can use it!");
		}
	}

	public void enableLocationSensor(final Context pContext, final ILocationListener pLocationListener, final LocationSensorOptions pLocationSensorOptions) {
		this.mLocationListener = pLocationListener;

		final LocationManager locationManager = (LocationManager) pContext.getSystemService(Context.LOCATION_SERVICE);
		final String locationProvider = locationManager.getBestProvider(pLocationSensorOptions, pLocationSensorOptions.isEnabledOnly());
		// TODO locationProvider can be null, in that case return false. Successful case should return true.
		locationManager.requestLocationUpdates(locationProvider, pLocationSensorOptions.getMinimumTriggerTime(), pLocationSensorOptions.getMinimumTriggerDistance(), this);

		this.onLocationChanged(locationManager.getLastKnownLocation(locationProvider));
	}

	public void disableLocationSensor(final Context pContext) {
		final LocationManager locationManager = (LocationManager) pContext.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(this);
	}

	/**
	 * @see {@link Engine#enableAccelerationSensor(Context, IAccelerationListener, AccelerationSensorOptions)}
	 */
	public boolean enableAccelerationSensor(final Context pContext, final IAccelerationListener pAccelerationListener) {
		return this.enableAccelerationSensor(pContext, pAccelerationListener, new AccelerationSensorOptions(Engine.SENSORDELAY_DEFAULT));
	}

	/**
	 * @return <code>true</code> when the sensor was successfully enabled, <code>false</code> otherwise.
	 */
	public boolean enableAccelerationSensor(final Context pContext, final IAccelerationListener pAccelerationListener, final AccelerationSensorOptions pAccelerationSensorOptions) {
		final SensorManager sensorManager = (SensorManager) pContext.getSystemService(Context.SENSOR_SERVICE);
		if(Engine.isSensorSupported(sensorManager, Sensor.TYPE_ACCELEROMETER)) {
			this.mAccelerationListener = pAccelerationListener;

			if(this.mAccelerationData == null) {
				final Display display = ((WindowManager) pContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				final int displayRotation = display.getOrientation();
				this.mAccelerationData = new AccelerationData(displayRotation);
			}

			this.registerSelfAsSensorListener(sensorManager, Sensor.TYPE_ACCELEROMETER, pAccelerationSensorOptions.getSensorDelay());

			return true;
		} else {
			return false;
		}
	}


	/**
	 * @return <code>true</code> when the sensor was successfully disabled, <code>false</code> otherwise.
	 */
	public boolean disableAccelerationSensor(final Context pContext) {
		final SensorManager sensorManager = (SensorManager) pContext.getSystemService(Context.SENSOR_SERVICE);
		if(Engine.isSensorSupported(sensorManager, Sensor.TYPE_ACCELEROMETER)) {
			this.unregisterSelfAsSensorListener(sensorManager, Sensor.TYPE_ACCELEROMETER);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @see {@link Engine#enableOrientationSensor(Context, IOrientationListener, OrientationSensorOptions)}
	 */
	public boolean enableOrientationSensor(final Context pContext, final IOrientationListener pOrientationListener) {
		return this.enableOrientationSensor(pContext, pOrientationListener, new OrientationSensorOptions(Engine.SENSORDELAY_DEFAULT));
	}

	/**
	 * @return <code>true</code> when the sensor was successfully enabled, <code>false</code> otherwise.
	 */
	public boolean enableOrientationSensor(final Context pContext, final IOrientationListener pOrientationListener, final OrientationSensorOptions pOrientationSensorOptions) {
		final SensorManager sensorManager = (SensorManager) pContext.getSystemService(Context.SENSOR_SERVICE);
		if(Engine.isSensorSupported(sensorManager, Sensor.TYPE_ACCELEROMETER) && Engine.isSensorSupported(sensorManager, Sensor.TYPE_MAGNETIC_FIELD)) {
			this.mOrientationListener = pOrientationListener;

			if(this.mOrientationData == null) {
				final Display display = ((WindowManager) pContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
				final int displayRotation = display.getOrientation();
				this.mOrientationData = new OrientationData(displayRotation);
			}

			this.registerSelfAsSensorListener(sensorManager, Sensor.TYPE_ACCELEROMETER, pOrientationSensorOptions.getSensorDelay());
			this.registerSelfAsSensorListener(sensorManager, Sensor.TYPE_MAGNETIC_FIELD, pOrientationSensorOptions.getSensorDelay());

			return true;
		} else {
			return false;
		}
	}


	/**
	 * @return <code>true</code> when the sensor was successfully disabled, <code>false</code> otherwise.
	 */
	public boolean disableOrientationSensor(final Context pContext) {
		final SensorManager sensorManager = (SensorManager) pContext.getSystemService(Context.SENSOR_SERVICE);
		if(Engine.isSensorSupported(sensorManager, Sensor.TYPE_ACCELEROMETER) && Engine.isSensorSupported(sensorManager, Sensor.TYPE_MAGNETIC_FIELD)) {
			this.unregisterSelfAsSensorListener(sensorManager, Sensor.TYPE_ACCELEROMETER);
			this.unregisterSelfAsSensorListener(sensorManager, Sensor.TYPE_MAGNETIC_FIELD);
			return true;
		} else {
			return false;
		}
	}

	private static boolean isSensorSupported(final SensorManager pSensorManager, final int pType) {
		return pSensorManager.getSensorList(pType).size() > 0;
	}

	private void registerSelfAsSensorListener(final SensorManager pSensorManager, final int pType, final SensorDelay pSensorDelay) {
		final Sensor sensor = pSensorManager.getSensorList(pType).get(0);
		pSensorManager.registerListener(this, sensor, pSensorDelay.getDelay());
	}

	private void unregisterSelfAsSensorListener(final SensorManager pSensorManager, final int pType) {
		final Sensor sensor = pSensorManager.getSensorList(pType).get(0);
		pSensorManager.unregisterListener(this, sensor);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public static class UpdateThread extends Thread {
		// ===========================================================
		// Constants
		// ===========================================================

		// ===========================================================
		// Fields
		// ===========================================================

		private Engine mEngine;
		private final RunnableHandler mRunnableHandler = new RunnableHandler();

		// ===========================================================
		// Constructors
		// ===========================================================

		public UpdateThread() {
			super(UpdateThread.class.getSimpleName());
		}

		// ===========================================================
		// Getter & Setter
		// ===========================================================

		public void setEngine(final Engine pEngine) {
			this.mEngine = pEngine;
		}

		// ===========================================================
		// Methods for/from SuperClass/Interfaces
		// ===========================================================

		@Override
		public void run() {
			android.os.Process.setThreadPriority(this.mEngine.getEngineOptions().getUpdateThreadPriority());
			try {
				while(true) {
					this.mRunnableHandler.onUpdate(0);
					this.mEngine.onTickUpdate();
				}
			} catch (final InterruptedException e) {
				if(BuildConfig.DEBUG) {
					Debug.d(this.getClass().getSimpleName() + " interrupted. Don't worry - this " + e.getClass().getSimpleName() + " is most likely expected!", e);
				}
				this.interrupt();
			}
		}

		// ===========================================================
		// Methods
		// ===========================================================

		public void postRunnable(final Runnable pRunnable) {
			this.mRunnableHandler.postRunnable(pRunnable);
		}

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================
	}

	public class EngineDestroyedException extends InterruptedException {
		// ===========================================================
		// Constants
		// ===========================================================

		private static final long serialVersionUID = -4691263961728972560L;

		// ===========================================================
		// Fields
		// ===========================================================

		// ===========================================================
		// Constructors
		// ===========================================================

		// ===========================================================
		// Getter & Setter
		// ===========================================================

		// ===========================================================
		// Methods for/from SuperClass/Interfaces
		// ===========================================================

		// ===========================================================
		// Methods
		// ===========================================================

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================
	}

	public static class EngineLock extends ReentrantLock {
		// ===========================================================
		// Constants
		// ===========================================================

		private static final long serialVersionUID = 671220941302523934L;

		// ===========================================================
		// Fields
		// ===========================================================

		final Condition mDrawingCondition = this.newCondition();
		final AtomicBoolean mDrawing = new AtomicBoolean(false);

		// ===========================================================
		// Constructors
		// ===========================================================

		public EngineLock(final boolean pFair) {
			super(pFair);
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

		void notifyCanDraw() {
			this.mDrawing.set(true);
			this.mDrawingCondition.signalAll();
		}

		void notifyCanUpdate() {
			this.mDrawing.set(false);
			this.mDrawingCondition.signalAll();
		}

		void waitUntilCanDraw() throws InterruptedException {
			while(!this.mDrawing.get()) {
				this.mDrawingCondition.await();
			}
		}

		void waitUntilCanUpdate() throws InterruptedException {
			while(this.mDrawing.get()) {
				this.mDrawingCondition.await();
			}
		}

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================
	}
}
