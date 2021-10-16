package org.anddev.andengine.ui.activity;

import org.anddev.andengine.audio.music.MusicManager;
import org.anddev.andengine.audio.sound.SoundManager;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.opengl.font.FontManager;
import org.anddev.andengine.opengl.texture.TextureManager;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.ui.IGameInterface;
import org.anddev.andengine.util.ActivityUtils;
import org.anddev.andengine.util.Debug;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Gravity;
import android.widget.FrameLayout.LayoutParams;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:27:06 - 08.03.2010
 */
public abstract class BaseGameActivity extends BaseActivity implements IGameInterface {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	protected Engine mEngine;
	private WakeLock mWakeLock;
	protected RenderSurfaceView mRenderSurfaceView;
	protected boolean mHasWindowFocused;
	private boolean mPaused;
	private boolean mGameLoaded;

	// ===========================================================
	// Constructors
	// ===========================================================

	@Override
	protected void onCreate(final Bundle pSavedInstanceState) {
		super.onCreate(pSavedInstanceState);
		this.mPaused = true;

		this.mEngine = this.onLoadEngine();
		if (this.mEngine == null) {
			return;
		}

		this.applyEngineOptions(this.mEngine.getEngineOptions());

		this.onSetContentView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (this.mEngine == null) {
			return;
		}
		if(this.mPaused && this.mHasWindowFocused) {
			this.doResume();
		}
	}

	@Override
	public void onWindowFocusChanged(final boolean pHasWindowFocus) {
		super.onWindowFocusChanged(pHasWindowFocus);
		if (this.mEngine == null) {
			return;
		}
		if(pHasWindowFocus) {
			if(this.mPaused) {
				this.doResume();
			}
			this.mHasWindowFocused = true;
		} else {
			/* if(!this.mPaused) {
				this.doPause();
			} */
			this.mHasWindowFocused = false;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (this.mEngine == null) {
			return;
		}
		if(!this.mPaused) {
			this.doPause();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
        if (this.mEngine == null) {
            return;
        }
		android.os.Process.killProcess(android.os.Process.myPid());

		this.mEngine.interruptUpdateThread();

		this.onUnloadResources();
	}

	@Override
	public void onUnloadResources() {
		if (this.mEngine == null) {
			return;
		}
		if(this.mEngine.getEngineOptions().needsMusic()) {
			this.getMusicManager().releaseAll();
		}
		if(this.mEngine.getEngineOptions().needsSound()) {
			this.getSoundManager().releaseAll();
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public Engine getEngine() {
		return this.mEngine;
	}

	public TextureManager getTextureManager() {
		return this.mEngine.getTextureManager();
	}

	public FontManager getFontManager() {
		return this.mEngine.getFontManager();
	}

	public SoundManager getSoundManager() {
		return this.mEngine.getSoundManager();
	}

	public MusicManager getMusicManager() {
		return this.mEngine.getMusicManager();
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onResumeGame() {

	}

	@Override
	public void onPauseGame() {

	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void doResume() {
		if (this.mEngine == null) {
			return;
		}
		if(!this.mGameLoaded) {
			this.onLoadResources();
			final Scene scene = this.onLoadScene();
			this.mEngine.onLoadComplete(scene);
			this.onLoadComplete();
			this.mGameLoaded = true;
		}

		this.mPaused = false;
		this.acquireWakeLock(this.mEngine.getEngineOptions().getWakeLockOptions());
		this.mEngine.onResume();

		this.mRenderSurfaceView.onResume();
		this.mEngine.start();
		this.onResumeGame();
	}

	private void doPause() {
		if (this.mEngine == null) {
			return;
		}
		this.mPaused = true;
		this.releaseWakeLock();

		this.mEngine.onPause();
		this.mEngine.stop();
		this.mRenderSurfaceView.onPause();
		this.onPauseGame();
	}

	public void runOnUpdateThread(final Runnable pRunnable) {
		if (this.mEngine == null) {
			return;
		}
		this.mEngine.runOnUpdateThread(pRunnable);
	}

	protected void onSetContentView() {
		if (this.mEngine == null) {
			return;
		}
		this.mRenderSurfaceView = new RenderSurfaceView(this);
		this.mRenderSurfaceView.setEGLConfigChooser(false);
		this.mRenderSurfaceView.setRenderer(this.mEngine);

		this.setContentView(this.mRenderSurfaceView, this.createSurfaceViewLayoutParams());
	}

	private void acquireWakeLock(final WakeLockOptions pWakeLockOptions) {
		if(pWakeLockOptions == WakeLockOptions.SCREEN_ON) {
			ActivityUtils.keepScreenOn(this);
		} else {
			final PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
			this.mWakeLock = pm.newWakeLock(pWakeLockOptions.getFlag() | PowerManager.ON_AFTER_RELEASE, "andengine:AndEngine");
			try {
				this.mWakeLock.acquire();
			} catch (final SecurityException e) {
				Debug.e("You have to add\n\t<uses-permission android:name=\"android.permission.WAKE_LOCK\"/>\nto your AndroidManifest.xml !", e);
			}
		}
	}

	private void releaseWakeLock() {
		if(this.mWakeLock != null && this.mWakeLock.isHeld()) {
			this.mWakeLock.release();
		}
	}

	private void applyEngineOptions(final EngineOptions pEngineOptions) {
		if(pEngineOptions.isFullscreen()) {
			ActivityUtils.requestFullscreen(this);
		}

		if(pEngineOptions.needsMusic() || pEngineOptions.needsSound()) {
			this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}

		if (pEngineOptions.getScreenOrientation() != null) {
			switch (pEngineOptions.getScreenOrientation()) {

				case LANDSCAPE:
					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					break;
				case PORTRAIT:
					this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					break;
			}
		}
	}

	protected LayoutParams createSurfaceViewLayoutParams() {
		final LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		return layoutParams;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
