package org.andengine.opengl.view;

import org.andengine.engine.Engine;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 11:57:29 - 08.03.2010
 */
public class RenderSurfaceView extends GLSurfaceView {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = "RenderSurfaceView";

	// ===========================================================
	// Fields
	// ===========================================================

	private EngineRenderer mEngineRenderer;
	private ConfigChooser mConfigChooser;

	// ===========================================================
	// Constructors
	// ===========================================================

	public RenderSurfaceView(final Context pContext) {
		super(pContext);
		this.setEGLContextFactory(new GLES32ContextFactory());
	}

	public RenderSurfaceView(final Context pContext, final AttributeSet pAttrs) {
		super(pContext, pAttrs);
		this.setEGLContextFactory(new GLES32ContextFactory());
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public ConfigChooser getConfigChooser() throws IllegalStateException {
		if(this.mConfigChooser == null) {
			throw new IllegalStateException(ConfigChooser.class.getSimpleName() + " not yet set.");
		}
		return this.mConfigChooser;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	/**
	 * @see android.view.View#measure(int, int)
	 */
	@Override
	protected void onMeasure(final int pWidthMeasureSpec, final int pHeightMeasureSpec) {
		if(this.isInEditMode()) {
			super.onMeasure(pWidthMeasureSpec, pHeightMeasureSpec);
			return;
		}
		this.mEngineRenderer.mEngine.getEngineOptions().getResolutionPolicy().onMeasure(this, pWidthMeasureSpec, pHeightMeasureSpec);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void setMeasuredDimensionProxy(final int pMeasuredWidth, final int pMeasuredHeight) {
		this.setMeasuredDimension(pMeasuredWidth, pMeasuredHeight);
	}

	public void setRenderer(final Engine pEngine, final IRendererListener pRendererListener) {
		if(this.mConfigChooser == null) {
			final boolean multiSampling = pEngine.getEngineOptions().getRenderOptions().isMultiSampling();
			this.mConfigChooser = new ConfigChooser(multiSampling);
		}
		this.setEGLConfigChooser(this.mConfigChooser);

		// Keep the EGL context alive while the app is paused so that GPU resources
		// (VBOs, textures, shader programs) survive a normal background/foreground
		// transition. The system will still destroy the context under memory pressure,
		// but that case is handled separately in EngineRenderer.onSurfaceCreated.
		this.setPreserveEGLContextOnPause(true);

		this.setOnTouchListener(pEngine);
		this.mEngineRenderer = new EngineRenderer(pEngine, this.mConfigChooser, pRendererListener);
		this.setRenderer(this.mEngineRenderer);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * EGL context factory that requests an OpenGL ES 3.2 context and falls back
	 * to 3.0 on devices that do not support the minor version attribute.
	 */
	private static final class GLES32ContextFactory implements GLSurfaceView.EGLContextFactory {

		/** Standard EGL attribute for the context major version (same as EGL_CONTEXT_CLIENT_VERSION). */
		private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
		/** KHR extension attribute for minor version; equals EGL_CONTEXT_MINOR_VERSION_KHR = 0x30FB. */
		private static final int EGL_CONTEXT_MINOR_VERSION_KHR = 0x30FB;

		@Override
		public EGLContext createContext(final EGL10 egl, final EGLDisplay display, final EGLConfig eglConfig) {
			// Try OpenGL ES 3.2 first.
			final int[] attribs32 = {
				EGL_CONTEXT_CLIENT_VERSION, 3,
				EGL_CONTEXT_MINOR_VERSION_KHR, 2,
				EGL10.EGL_NONE
			};
			EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attribs32);
			if (context != null && !context.equals(EGL10.EGL_NO_CONTEXT)) {
				Log.i(TAG, "Created OpenGL ES 3.2 context.");
				return context;
			}

			// Fall back to OpenGL ES 3.0.
			Log.w(TAG, "OpenGL ES 3.2 not available, falling back to ES 3.0.");
			final int[] attribs30 = {
				EGL_CONTEXT_CLIENT_VERSION, 3,
				EGL10.EGL_NONE
			};
			context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attribs30);
			if (context != null && !context.equals(EGL10.EGL_NO_CONTEXT)) {
				Log.i(TAG, "Created OpenGL ES 3.0 context.");
				return context;
			}

			Log.e(TAG, "Failed to create OpenGL ES 3.0 or 3.2 context.");
			return null;
		}

		@Override
		public void destroyContext(final EGL10 egl, final EGLDisplay display, final EGLContext context) {
			egl.eglDestroyContext(display, context);
		}
	}
}