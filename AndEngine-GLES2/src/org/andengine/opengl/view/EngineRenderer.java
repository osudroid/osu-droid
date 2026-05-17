package org.andengine.opengl.view;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.andengine.engine.Engine;
import org.andengine.engine.options.RenderOptions;
import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.util.GLState;
import org.andengine.util.debug.Debug;

import android.opengl.GLES20;
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

	// ===========================================================
	// Fields
	// ===========================================================

	final Engine mEngine;
	final ConfigChooser mConfigChooser;
	final boolean mMultiSampling;
	final IRendererListener mRendererListener;
	final GLState mGLState;

	// ===========================================================
	// Constructors
	// ===========================================================

	public EngineRenderer(final Engine pEngine, final ConfigChooser pConfigChooser, final IRendererListener pRendererListener) {
		this.mEngine = pEngine;
		this.mConfigChooser = pConfigChooser;
		this.mRendererListener = pRendererListener;
		this.mGLState = new GLState();
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
		synchronized(GLState.class) {
			final RenderOptions renderOptions = this.mEngine.getEngineOptions().getRenderOptions();
			this.mGLState.reset(renderOptions, this.mConfigChooser, pEGLConfig);

			// EGL context loss recovery: reset all shader programs so they are re-linked
			// against the new GL context on next draw.
			// Note: Buffer.onContextLost() (VBO ID reset) is called from
			// MainActivity.onSurfaceCreated, which is in the app module.
			ShaderProgram.resetAllForContextLoss();

			// TODO Check if available and make available through EngineOptions-RenderOptions
//			GLES20.glEnable(GLES20.GL_POLYGON_SMOOTH);
//			GLES20.glHint(GLES20.GL_POLYGON_SMOOTH_HINT, GLES20.GL_NICEST);
//			GLES20.glEnable(GLES20.GL_LINE_SMOOTH);
//			GLES20.glHint(GLES20.GL_LINE_SMOOTH_HINT, GLES20.GL_NICEST);
//			GLES20.glEnable(GLES20.GL_POINT_SMOOTH);
//			GLES20.glHint(GLES20.GL_POINT_SMOOTH_HINT, GLES20.GL_NICEST);

			this.mGLState.disableDepthTest();
			// Ensure initial depth clear value is 1.0 (maximum) so that GL_LESS tests
			// pass on first draw and per-entity ClearInfo.ClearDepthBuffer calls work correctly.
			GLES20.glClearDepthf(1.0f);
			GLES20.glDepthFunc(GLES20.GL_LESS);
			GLES20.glDepthMask(true);
			this.mGLState.enableBlend();
			this.mGLState.setDitherEnabled(renderOptions.isDithering());

			/* Enabling culling doesn't really make sense, because triangles are never drawn 'backwards' on purpose. */
//			this.mGLState.enableCulling();
//			GLES20.glFrontFace(GLES20.GL_CCW);
//			GLES20.glCullFace(GLES20.GL_BACK);

			if(this.mRendererListener != null) {
				this.mRendererListener.onSurfaceCreated(this.mGLState);
			}
		}
	}

	@Override
	public void onSurfaceChanged(final GL10 pGL, final int pWidth, final int pHeight) {
		this.mEngine.setSurfaceSize(pWidth, pHeight);
		GLES20.glViewport(0, 0, pWidth, pHeight);
		this.mGLState.loadProjectionGLMatrixIdentity();

		if(this.mRendererListener != null) {
			this.mRendererListener.onSurfaceChanged(this.mGLState, pWidth, pHeight);
		}
	}

	@Override
	public void onDrawFrame(final GL10 pGL) {
		synchronized(GLState.class) {
			if (this.mMultiSampling && this.mConfigChooser.isCoverageMultiSampling()) {
				final int GL_COVERAGE_BUFFER_BIT_NV = 0x8000;
				GLES20.glClear(GL_COVERAGE_BUFFER_BIT_NV);
			}

			// Clear the depth buffer at the start of every frame so leftover depth values
			// from the previous frame (written by slider-body triangle meshes) never bleed
			// into the current frame and cause artifacts on sprites / circles.
			GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

			try {
				this.mEngine.onDrawFrame(this.mGLState);
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