package org.andengine.opengl.view;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.andengine.engine.Engine;
import org.andengine.engine.options.RenderOptions;
import org.andengine.graphics.atlas.TextureAtlasPages;
import org.andengine.graphics.debug.RenderStats;
import org.andengine.graphics.gles.GlesBackend;
import org.andengine.graphics.gles.GlesDebugOutput;
import org.andengine.opengl.exception.GLESVersionUnsupportedException;
import org.andengine.opengl.shader.exception.ShaderProgramCompileException;
import org.andengine.opengl.util.GLState;
import org.andengine.util.debug.Debug;

import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.os.Build;

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
			// New EGL context: detect capabilities, start with an empty state cache and replay the
			// resource registry (shader programs, AndEngine managers and the app's legacy restorers).
			// Must run before GLState.reset(), which sets its defaults through that new cache.
			// This also happens for the very first context, so the managers reload and the engine
			// reports one lagging frame (Engine.mIsLagging) right after startup; harmless.
			GlesBackend.onContextCreated();
			this.mGLState.reset(renderOptions, this.mConfigChooser, pEGLConfig);
			TextureAtlasPages.setDeviceMaxTextureSize(this.mGLState.getMaximumTextureSize());
			RenderStats.reset();

			this.mGLState.disableDepthTest();
			// Ensure initial depth clear value is 1.0 (maximum) so that GL_LESS tests
			// pass on first draw and per-entity ClearInfo.ClearDepthBuffer calls work correctly.
			GlesBackend.getState().clearDepth(1.0f);
			GlesBackend.getState().depthFunc(GLES32.GL_LESS);
			GlesBackend.getState().depthMask(true);
			this.mGLState.enableBlend();
			this.mGLState.setDitherEnabled(renderOptions.isDithering());

			/* Enabling culling doesn't really make sense, because triangles are never drawn 'backwards' on purpose. */
//			this.mGLState.enableCulling();
//			GLES32.glFrontFace(GLES32.GL_CCW);
//			GLES32.glCullFace(GLES32.GL_BACK);

			if(this.mRendererListener != null) {
				this.mRendererListener.onSurfaceCreated(this.mGLState);
			}
		}
	}

	@Override
	public void onSurfaceChanged(final GL10 pGL, final int pWidth, final int pHeight) {
		this.mEngine.setSurfaceSize(pWidth, pHeight);
		GlesBackend.surfaceWidth = pWidth;
		GlesBackend.surfaceHeight = pHeight;
		GlesBackend.getState().viewport(0, 0, pWidth, pHeight);
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
				GlesBackend.getState().clear(GL_COVERAGE_BUFFER_BIT_NV);
			}

			// Clear the depth buffer at the start of every frame so leftover depth values
			// from the previous frame (written by slider-body triangle meshes) never bleed
			// into the current frame and cause artifacts on sprites / circles.
			// glClear honours the depth mask, which a batch flush may have left off.
			GlesBackend.getState().depthMask(true);
			GlesBackend.getState().clear(GLES32.GL_DEPTH_BUFFER_BIT);

			try {
				this.mEngine.onDrawFrame(this.mGLState);
				GlesDebugOutput.poll();
			} catch (final InterruptedException e) {
				Debug.e("GLThread interrupted!", e);
			} catch (final ShaderProgramCompileException e) {
				// The EGL context reports a higher ES version than its GLSL compiler actually accepts
				throw this.wrapShaderCompileException(e);
			}
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private GLESVersionUnsupportedException wrapShaderCompileException(final ShaderProgramCompileException pException) {
		final String glVersion = GLES32.glGetString(GLES32.GL_VERSION);
		final String glslVersion = GLES32.glGetString(GLES32.GL_SHADING_LANGUAGE_VERSION);
		final String detected = (glslVersion != null ? glslVersion : "unknown") + (glVersion != null ? " (" + glVersion + ")" : "");

		return new GLESVersionUnsupportedException(
				"GLSL ES 3.00",
				detected,
				Build.MANUFACTURER + " " + Build.MODEL,
				Build.HARDWARE,
				Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")",
				pException
		);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}