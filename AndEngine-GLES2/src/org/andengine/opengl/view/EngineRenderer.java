package org.andengine.opengl.view;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.andengine.BuildConfig;
import org.andengine.engine.Engine;
import org.andengine.engine.options.RenderOptions;
import org.andengine.graphics.DrawContext;
import org.andengine.graphics.gles.GlesBackend;
import org.andengine.graphics.gles.GlesDebugOutput;
import org.andengine.util.debug.Debug;

import android.opengl.GLSurfaceView;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:57:29 - 08.03.2010
 */
public class EngineRenderer implements GLSurfaceView.Renderer {
	// ===========================================================
	// Constants
	// ===========================================================

	/** Held while a new context is set up and while a frame is drawn, by every renderer. */
	private static final Object DRAW_LOCK = new Object();

	// ===========================================================
	// Fields
	// ===========================================================

	final Engine mEngine;
	final ConfigChooser mConfigChooser;
	final boolean mMultiSampling;
	final IRendererListener mRendererListener;
	final DrawContext mDrawContext;

	// ===========================================================
	// Constructors
	// ===========================================================

	public EngineRenderer(final Engine pEngine, final ConfigChooser pConfigChooser, final IRendererListener pRendererListener) {
		this.mEngine = pEngine;
		this.mConfigChooser = pConfigChooser;
		this.mRendererListener = pRendererListener;
		this.mDrawContext = pEngine.getDrawContext();
		this.mMultiSampling = this.mEngine.getEngineOptions().getRenderOptions().isMultiSampling();
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onSurfaceCreated(final GL10 pGL, final EGLConfig pEGLConfig) {
		synchronized(EngineRenderer.DRAW_LOCK) {
			final RenderOptions renderOptions = this.mEngine.getEngineOptions().getRenderOptions();
			// New EGL context: detect capabilities, start with an empty state cache and replay the
			// resource registry (shader programs, AndEngine managers and the app's legacy restorers).
			// Must run before GlesBackend.resetDrawState(), which sets its defaults through that new cache.
			// This also happens for the very first context, so the managers reload and the engine
			// reports one lagging frame (Engine.mIsLagging) right after startup; harmless.
			GlesBackend.onContextCreated();
			if(BuildConfig.DEBUG) {
				Debug.d("EGLCONFIG: " + EGLConfig.class.getSimpleName() + "(Red=" + this.mConfigChooser.getRedSize() + ", Green=" + this.mConfigChooser.getGreenSize() + ", Blue=" + this.mConfigChooser.getBlueSize() + ", Alpha=" + this.mConfigChooser.getAlphaSize() + ", Depth=" + this.mConfigChooser.getDepthSize() + ", Stencil=" + this.mConfigChooser.getStencilSize() + ")");
				Debug.d("MAX_TEXTURE_SIZE: " + GlesBackend.getCaps().getMaxTextureSize());
			}
			this.mDrawContext.resetModelViewGLMatrixStack();
			this.mDrawContext.resetProjectionGLMatrixStack();
			GlesBackend.resetDrawState(renderOptions.isDithering());

			if(this.mRendererListener != null) {
				this.mRendererListener.onSurfaceCreated(this.mDrawContext);
			}
		}
	}

	@Override
	public void onSurfaceChanged(final GL10 pGL, final int pWidth, final int pHeight) {
		this.mEngine.setSurfaceSize(pWidth, pHeight);
		GlesBackend.surfaceWidth = pWidth;
		GlesBackend.surfaceHeight = pHeight;
		GlesBackend.getState().viewport(0, 0, pWidth, pHeight);
		this.mDrawContext.loadProjectionGLMatrixIdentity();

		if(this.mRendererListener != null) {
			this.mRendererListener.onSurfaceChanged(this.mDrawContext, pWidth, pHeight);
		}
	}

	@Override
	public void onDrawFrame(final GL10 pGL) {
		synchronized(EngineRenderer.DRAW_LOCK) {
			GlesBackend.beginFrame(this.mMultiSampling && this.mConfigChooser.isCoverageMultiSampling());

			try {
				this.mEngine.onDrawFrame(this.mDrawContext);
				GlesDebugOutput.poll();
			} catch (final InterruptedException e) {
				Debug.e("GLThread interrupted!", e);
			}
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}