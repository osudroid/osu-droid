package org.andengine.opengl.util;

import java.nio.Buffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;

import org.andengine.BuildConfig;
import org.andengine.engine.options.RenderOptions;
import org.andengine.graphics.gles.GlesBackend;
import org.andengine.graphics.gles.GlesStateCache;
import org.andengine.opengl.exception.GLException;
import org.andengine.opengl.exception.GLFrameBufferException;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.texture.PixelFormat;
import org.andengine.opengl.texture.render.RenderTexture;
import org.andengine.opengl.view.ConfigChooser;
import org.andengine.util.debug.Debug;

import android.graphics.Bitmap;
import android.opengl.GLES32;
import android.opengl.GLUtils;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 18:00:43 - 08.03.2010
 */
public class GLState {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int GL_UNPACK_ALIGNMENT_DEFAULT = 4;

	// ===========================================================
	// Fields
	// ===========================================================

	private final int[] mHardwareIDContainer = new int[1];

	private String mVersion;
	private String mRenderer;
	private String mExtensions;

	private int mMaximumVertexAttributeCount;
	private int mMaximumVertexShaderUniformVectorCount;
	private int mMaximumFragmentShaderUniformVectorCount;
	private int mMaximumTextureSize;
	private int mMaximumTextureUnits;

	// The binding and capability state lives in GlesBackend.state, shared with the new GL core. It is
	// read on every call because GlesBackend replaces the cache on every new context.

	private final GLMatrixStack mModelViewGLMatrixStack = new GLMatrixStack();
	private final GLMatrixStack mProjectionGLMatrixStack = new GLMatrixStack();

	private final float[] mModelViewGLMatrix = new float[GLMatrixStack.GLMATRIX_SIZE];
	private final float[] mProjectionGLMatrix = new float[GLMatrixStack.GLMATRIX_SIZE];
	private final float[] mModelViewProjectionGLMatrix = new float[GLMatrixStack.GLMATRIX_SIZE];

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public String getVersion() {
		return this.mVersion;
	}

	public String getRenderer() {
		return this.mRenderer;
	}

	public String getExtensions() {
		return this.mExtensions;
	}

	public int getMaximumVertexAttributeCount() {
		return this.mMaximumVertexAttributeCount;
	}

	public int getMaximumVertexShaderUniformVectorCount() {
		return this.mMaximumVertexShaderUniformVectorCount;
	}

	public int getMaximumFragmentShaderUniformVectorCount() {
		return this.mMaximumFragmentShaderUniformVectorCount;
	}

	public int getMaximumTextureUnits() {
		return this.mMaximumTextureUnits;
	}

	public int getMaximumTextureSize() {
		return this.mMaximumTextureSize;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void reset(final RenderOptions pRenderOptions, final ConfigChooser pConfigChooser, final EGLConfig pEGLConfig) {
		this.mVersion = GLES32.glGetString(GLES32.GL_VERSION);
		this.mRenderer = GLES32.glGetString(GLES32.GL_RENDERER);
		this.mExtensions = GLES32.glGetString(GLES32.GL_EXTENSIONS);

		this.mMaximumVertexAttributeCount = this.getInteger(GLES32.GL_MAX_VERTEX_ATTRIBS);
		this.mMaximumVertexShaderUniformVectorCount = this.getInteger(GLES32.GL_MAX_VERTEX_UNIFORM_VECTORS);
		this.mMaximumFragmentShaderUniformVectorCount = this.getInteger(GLES32.GL_MAX_FRAGMENT_UNIFORM_VECTORS);
		this.mMaximumTextureUnits = this.getInteger(GLES32.GL_MAX_TEXTURE_IMAGE_UNITS);
		this.mMaximumTextureSize = this.getInteger(GLES32.GL_MAX_TEXTURE_SIZE);

		if(BuildConfig.DEBUG) {
			Debug.d("VERSION: " + this.mVersion);
			Debug.d("RENDERER: " + this.mRenderer);
			Debug.d("EGLCONFIG: " + EGLConfig.class.getSimpleName() + "(Red=" + pConfigChooser.getRedSize() + ", Green=" + pConfigChooser.getGreenSize() + ", Blue=" + pConfigChooser.getBlueSize() + ", Alpha=" + pConfigChooser.getAlphaSize() + ", Depth=" + pConfigChooser.getDepthSize() + ", Stencil=" + pConfigChooser.getStencilSize() + ")");
			Debug.d("EXTENSIONS: " + this.mExtensions);
			Debug.d("MAX_VERTEX_ATTRIBS: " + this.mMaximumVertexAttributeCount);
			Debug.d("MAX_VERTEX_UNIFORM_VECTORS: " + this.mMaximumVertexShaderUniformVectorCount);
			Debug.d("MAX_FRAGMENT_UNIFORM_VECTORS: " + this.mMaximumFragmentShaderUniformVectorCount);
			Debug.d("MAX_TEXTURE_IMAGE_UNITS: " + this.mMaximumTextureUnits);
			Debug.d("MAX_TEXTURE_SIZE: " + this.mMaximumTextureSize);
		}

		this.mModelViewGLMatrixStack.reset();
		this.mProjectionGLMatrixStack.reset();

		// GlesBackend.onContextCreated() has just installed a fresh cache for this context (everything
		// unknown, so the next bind of any id and the next scissor/line width change reach GL). Only
		// the defaults are set here. Bind VAO 0 first: the restore may have bound another one, and the
		// legacy renderer's attribute arrays below belong to VAO 0.
		final GlesStateCache state = GlesBackend.getState();
		state.bindVertexArray(0);

		this.enableDither();
		this.enableDepthTest();

		this.disableBlend();
		this.disableCulling();

		state.enableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION);
		state.enableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
		state.enableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION);
	}

	public boolean isScissorTestEnabled() {
		return GlesBackend.getState().isScissorEnabled();
	}
	/**
	 * @return the previous state.
	 */
	public boolean enableScissorTest() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isScissorEnabled();
		state.setScissorEnabled(true);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean disableScissorTest() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isScissorEnabled();
		state.setScissorEnabled(false);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean setScissorTestEnabled(final boolean pEnabled) {
		if(pEnabled) {
			return this.enableScissorTest();
		} else {
			return this.disableScissorTest();
		}
	}

	public boolean isBlendEnabled() {
		return GlesBackend.getState().isBlendEnabled();
	}
	/**
	 * @return the previous state.
	 */
	public boolean enableBlend() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isBlendEnabled();
		state.setBlendEnabled(true);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean disableBlend() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isBlendEnabled();
		state.setBlendEnabled(false);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean setBlendEnabled(final boolean pEnabled) {
		if(pEnabled) {
			return this.enableBlend();
		} else {
			return this.disableBlend();
		}
	}

	public boolean isCullingEnabled() {
		return GlesBackend.getState().isCullFaceEnabled();
	}
	/**
	 * @return the previous state.
	 */
	public boolean enableCulling() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isCullFaceEnabled();
		state.setCullFaceEnabled(true);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean disableCulling() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isCullFaceEnabled();
		state.setCullFaceEnabled(false);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean setCullingEnabled(final boolean pEnabled) {
		if(pEnabled) {
			return this.enableCulling();
		} else {
			return this.disableCulling();
		}
	}

	public boolean isDitherEnabled() {
		return GlesBackend.getState().isDitherEnabled();
	}
	/**
	 * @return the previous state.
	 */
	public boolean enableDither() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isDitherEnabled();
		state.setDitherEnabled(true);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean disableDither() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isDitherEnabled();
		state.setDitherEnabled(false);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean setDitherEnabled(final boolean pEnabled) {
		if(pEnabled) {
			return this.enableDither();
		} else {
			return this.disableDither();
		}
	}

	public boolean isDepthTestEnabled() {
		return GlesBackend.getState().isDepthTestEnabled();
	}
	/**
	 * @return the previous state.
	 */
	public boolean enableDepthTest() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isDepthTestEnabled();
		state.setDepthTestEnabled(true);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean disableDepthTest() {
		final GlesStateCache state = GlesBackend.getState();
		final boolean previous = state.isDepthTestEnabled();
		state.setDepthTestEnabled(false);
		return previous;
	}
	/**
	 * @return the previous state.
	 */
	public boolean setDepthTestEnabled(final boolean pEnabled) {
		if(pEnabled) {
			return this.enableDepthTest();
		} else {
			return this.disableDepthTest();
		}
	}

	public int generateBuffer() {
		GLES32.glGenBuffers(1, this.mHardwareIDContainer, 0);
		return this.mHardwareIDContainer[0];
	}

	public int generateArrayBuffer(final int pSize, final int pUsage) {
		GLES32.glGenBuffers(1, this.mHardwareIDContainer, 0);
		final int hardwareBufferID = this.mHardwareIDContainer[0];

		this.bindArrayBuffer(hardwareBufferID);
		GLES32.glBufferData(GLES32.GL_ARRAY_BUFFER, pSize, null, pUsage);
		this.bindArrayBuffer(0);

		return hardwareBufferID;
	}

	public void bindArrayBuffer(final int pHardwareBufferID) {
		GlesBackend.getState().bindArrayBuffer(pHardwareBufferID);
	}

	public void deleteArrayBuffer(final int pHardwareBufferID) {
		this.deleteBuffer(pHardwareBufferID);
	}

	public int generateIndexBuffer(final int pSize, final int pUsage) {
		GLES32.glGenBuffers(1, this.mHardwareIDContainer, 0);
		final int hardwareBufferID = this.mHardwareIDContainer[0];
		
		this.bindIndexBuffer(hardwareBufferID);
		GLES32.glBufferData(GLES32.GL_ELEMENT_ARRAY_BUFFER, pSize, null, pUsage);
		this.bindIndexBuffer(0);
		
		return hardwareBufferID;
	}

	public void bindIndexBuffer(final int pHardwareBufferID) {
		GlesBackend.getState().bindElementBuffer(pHardwareBufferID);
	}

	public void deleteIndexBuffer(final int pHardwareBufferID) {
		this.deleteBuffer(pHardwareBufferID);
	}

	private void deleteBuffer(final int pHardwareBufferID) {
		// The cache first: queued drawing is drawn while the object still exists.
		GlesBackend.getState().onBufferDeleted(pHardwareBufferID);
		this.mHardwareIDContainer[0] = pHardwareBufferID;
		GlesBackend.getGl().glDeleteBuffers(1, this.mHardwareIDContainer, 0);
	}

	public int generateFramebuffer() {
		GLES32.glGenFramebuffers(1, this.mHardwareIDContainer, 0);
		return this.mHardwareIDContainer[0];
	}

	public void bindFramebuffer(final int pFramebufferID) {
		GlesBackend.getState().bindFramebuffer(pFramebufferID);
	}

	public int getFramebufferStatus() {
		return GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER);
	}

	public void checkFramebufferStatus() throws GLFrameBufferException, GLException {
		final int framebufferStatus = this.getFramebufferStatus();
		switch(framebufferStatus) {
			case GLES32.GL_FRAMEBUFFER_COMPLETE:
				return;
			case GLES32.GL_FRAMEBUFFER_UNSUPPORTED:
				throw new GLFrameBufferException(framebufferStatus, "GL_FRAMEBUFFER_UNSUPPORTED");
			case GLES32.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
				throw new GLFrameBufferException(framebufferStatus, "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
			case GLES32.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
				throw new GLFrameBufferException(framebufferStatus, "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
			case GLES32.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
				throw new GLFrameBufferException(framebufferStatus, "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
			case 0:
				this.checkError();
			default:
				throw new GLFrameBufferException(framebufferStatus);
		}
	}

	public int getActiveFramebuffer() {
		return this.getInteger(GLES32.GL_FRAMEBUFFER_BINDING);
	}

	public void deleteFramebuffer(final int pHardwareFramebufferID) {
		GlesBackend.getState().onFramebufferDeleted(pHardwareFramebufferID);
		this.mHardwareIDContainer[0] = pHardwareFramebufferID;
		GlesBackend.getGl().glDeleteFramebuffers(1, this.mHardwareIDContainer, 0);
	}

	public void useProgram(final int pShaderProgramID) {
		GlesBackend.getState().useProgram(pShaderProgramID);
	}

	public void deleteProgram(final int pShaderProgramID) {
		GlesBackend.getState().onProgramDeleted(pShaderProgramID);
		GlesBackend.getGl().glDeleteProgram(pShaderProgramID);
	}

	public int generateTexture() {
		GLES32.glGenTextures(1, this.mHardwareIDContainer, 0);
		return this.mHardwareIDContainer[0];
	}

	public boolean isTexture(final int pHardwareTextureID) {
		return GLES32.glIsTexture(pHardwareTextureID);
	}

	/**
	 * @return {@link GLES20#GL_TEXTURE0} to {@link GLES20#GL_TEXTURE31}
	 */
	public int getActiveTexture() {
		final int unit = GlesBackend.getState().getCurrentActiveTextureUnit();
		// Unknown (fresh context) is unit 0, the GL default.
		return (unit < 0 ? 0 : unit) + GLES32.GL_TEXTURE0;
	}

	/**
	 * @param pGLActiveTexture from {@link GLES20#GL_TEXTURE0} to {@link GLES20#GL_TEXTURE31}.
	 */
	public void activeTexture(final int pGLActiveTexture) {
		GlesBackend.getState().activeTexture(pGLActiveTexture - GLES32.GL_TEXTURE0);
	}

	/**
	 * Binds the given texture to {@link GLES20#GL_TEXTURE_2D} on the currently active texture unit,
	 * skipping the GL call if the texture is already bound (cached per texture unit).
	 *
	 * @param pHardwareTextureID the hardware texture ID to bind.
	 */
	public void bindTexture(final int pHardwareTextureID) {
		GlesBackend.getState().bindTexture(GLES32.GL_TEXTURE_2D, pHardwareTextureID);
	}

	public void deleteTexture(final int pHardwareTextureID) {
		GlesBackend.getState().onTextureDeleted(pHardwareTextureID);
		this.mHardwareIDContainer[0] = pHardwareTextureID;
		GlesBackend.getGl().glDeleteTextures(1, this.mHardwareIDContainer, 0);
	}

	public void blendFunction(final int pSourceBlendMode, final int pDestinationBlendMode) {
		GlesBackend.getState().blendFunc(pSourceBlendMode, pDestinationBlendMode);
	}

	public void lineWidth(final float pLineWidth) {
		GlesBackend.getState().lineWidth(pLineWidth);
	}

	public void pushModelViewGLMatrix() {
		this.mModelViewGLMatrixStack.glPushMatrix();
	}

	public void popModelViewGLMatrix() {
		this.mModelViewGLMatrixStack.glPopMatrix();
	}

	public void loadModelViewGLMatrixIdentity() {
		this.mModelViewGLMatrixStack.glLoadIdentity();
	}

	public void translateModelViewGLMatrixf(final float pX, final float pY, final float pZ) {
		this.mModelViewGLMatrixStack.glTranslatef(pX, pY, pZ);
	}

	public void rotateModelViewGLMatrixf(final float pAngle, final float pX, final float pY, final float pZ) {
		this.mModelViewGLMatrixStack.glRotatef(pAngle, pX, pY, pZ);
	}

	public void scaleModelViewGLMatrixf(final float pScaleX, final float pScaleY, final int pScaleZ) {
		this.mModelViewGLMatrixStack.glScalef(pScaleX, pScaleY, pScaleZ);
	}

	public void skewModelViewGLMatrixf(final float pSkewX, final float pSkewY) {
		this.mModelViewGLMatrixStack.glSkewf(pSkewX, pSkewY);
	}

	public void orthoModelViewGLMatrixf(final float pLeft, final float pRight, final float pBottom, final float pTop, final float pZNear, final float pZFar) {
		this.mModelViewGLMatrixStack.glOrthof(pLeft, pRight, pBottom, pTop, pZNear, pZFar);
	}

	public void pushProjectionGLMatrix() {
		this.mProjectionGLMatrixStack.glPushMatrix();
	}

	public void popProjectionGLMatrix() {
		this.mProjectionGLMatrixStack.glPopMatrix();
	}

	public void loadProjectionGLMatrixIdentity() {
		this.mProjectionGLMatrixStack.glLoadIdentity();
	}

	public void translateProjectionGLMatrixf(final float pX, final float pY, final float pZ) {
		this.mProjectionGLMatrixStack.glTranslatef(pX, pY, pZ);
	}

	public void rotateProjectionGLMatrixf(final float pAngle, final float pX, final float pY, final float pZ) {
		this.mProjectionGLMatrixStack.glRotatef(pAngle, pX, pY, pZ);
	}

	public void scaleProjectionGLMatrixf(final float pScaleX, final float pScaleY, final float pScaleZ) {
		this.mProjectionGLMatrixStack.glScalef(pScaleX, pScaleY, pScaleZ);
	}

	public void skewProjectionGLMatrixf(final float pSkewX, final float pSkewY) {
		this.mProjectionGLMatrixStack.glSkewf(pSkewX, pSkewY);
	}

	public void orthoProjectionGLMatrixf(final float pLeft, final float pRight, final float pBottom, final float pTop, final float pZNear, final float pZFar) {
		this.mProjectionGLMatrixStack.glOrthof(pLeft, pRight, pBottom, pTop, pZNear, pZFar);
	}

	public float[] getModelViewGLMatrix() {
		this.mModelViewGLMatrixStack.getMatrix(this.mModelViewGLMatrix);
		return this.mModelViewGLMatrix;
	}

	public float[] getProjectionGLMatrix() {
		this.mProjectionGLMatrixStack.getMatrix(this.mProjectionGLMatrix);
		return this.mProjectionGLMatrix;
	}

	public float[] getModelViewProjectionGLMatrix() {
		// Multiplied in Java instead of android.opengl.Matrix.multiplyMM, this runs for every draw call and the JNI
		// overhead of multiplyMM is far larger than the multiplication itself.
		final float[] result = this.mModelViewProjectionGLMatrix;
		final float[] lhs = this.mProjectionGLMatrixStack.mMatrixStack;
		final int lhsOffset = this.mProjectionGLMatrixStack.mMatrixStackOffset;
		final float[] rhs = this.mModelViewGLMatrixStack.mMatrixStack;
		final int rhsOffset = this.mModelViewGLMatrixStack.mMatrixStackOffset;

		// Column-major, same as android.opengl.Matrix.multiplyMM: result = lhs * rhs.
		for (int column = 0; column < 4; column++) {
			final float r0 = rhs[rhsOffset + column * 4];
			final float r1 = rhs[rhsOffset + column * 4 + 1];
			final float r2 = rhs[rhsOffset + column * 4 + 2];
			final float r3 = rhs[rhsOffset + column * 4 + 3];

			for (int row = 0; row < 4; row++) {
				result[column * 4 + row] = lhs[lhsOffset + row] * r0
					+ lhs[lhsOffset + 4 + row] * r1
					+ lhs[lhsOffset + 8 + row] * r2
					+ lhs[lhsOffset + 12 + row] * r3;
			}
		}

		return result;
	}

	public void resetModelViewGLMatrixStack() {
		this.mModelViewGLMatrixStack.reset();
	}

	public void resetProjectionGLMatrixStack() {
		this.mProjectionGLMatrixStack.reset();
	}

	public void resetGLMatrixStacks() {
		this.mModelViewGLMatrixStack.reset();
		this.mProjectionGLMatrixStack.reset();
	}

	/**
	 * <b>Note:</b> does not pre-multiply the alpha channel!</br>
	 * Except that difference, same as: {@link GLUtils#texSubImage2D(int, int, int, int, Bitmap, int, int)}</br>
	 * </br>
	 * See topic: '<a href="http://groups.google.com/group/android-developers/browse_thread/thread/baa6c33e63f82fca">PNG loading that doesn't premultiply alpha?</a>'
	 * @param pBorder
	 */
	public void glTexImage2D(final int pTarget, final int pLevel, final Bitmap pBitmap, final int pBorder, final PixelFormat pPixelFormat) {
		final Buffer pixelBuffer = GLHelper.getPixels(pBitmap, pPixelFormat, ByteOrder.BIG_ENDIAN);

		GLES32.glTexImage2D(pTarget, pLevel, pPixelFormat.getGLInternalFormat(), pBitmap.getWidth(), pBitmap.getHeight(), pBorder, pPixelFormat.getGLFormat(), pPixelFormat.getGLType(), pixelBuffer);
	}

	/**
	 * <b>Note:</b> does not pre-multiply the alpha channel!</br>
	 * Except that difference, same as: {@link GLUtils#texSubImage2D(int, int, int, int, Bitmap, int, int)}</br>
	 * </br>
	 * See topic: '<a href="http://groups.google.com/group/android-developers/browse_thread/thread/baa6c33e63f82fca">PNG loading that doesn't premultiply alpha?</a>'
	 */
	public void glTexSubImage2D(final int pTarget, final int pLevel, final int pX, final int pY, final Bitmap pBitmap, final PixelFormat pPixelFormat) {
		final Buffer pixelBuffer = GLHelper.getPixels(pBitmap, pPixelFormat, ByteOrder.BIG_ENDIAN);

		GLES32.glTexSubImage2D(pTarget, pLevel, pX, pY, pBitmap.getWidth(), pBitmap.getHeight(), pPixelFormat.getGLFormat(), pPixelFormat.getGLType(), pixelBuffer);
	}

	/**
	 * Tells the OpenGL driver to send all pending commands to the GPU immediately.
	 *
	 * @see {@link GLState#finish()},
	 * 		{@link RenderTexture#end(GLState, boolean, boolean)}.
	 */
	public void flush() {
		GLES32.glFlush();
	}

	/**
	 * Tells the OpenGL driver to send all pending commands to the GPU immediately,
	 * and then blocks until the effects of those commands have been completed on the GPU.
	 * Since this is a costly method it should be only called when really needed.
	 *
	 * @see {@link GLState#flush()},
	 * 		{@link RenderTexture#end(GLState, boolean, boolean)}.
	 */
	public void finish() {
		GLES32.glFinish();
	}

	public int getInteger(final int pAttribute) {
		GLES32.glGetIntegerv(pAttribute, this.mHardwareIDContainer, 0);
		return this.mHardwareIDContainer[0];
	}

	public int getError() {
		return GLES32.glGetError();
	}

	public void checkError() throws GLException {
		final int error = GLES32.glGetError();
		if(error != GLES32.GL_NO_ERROR) {
			throw new GLException(error);
		}
	}

	public void clearError() {
		GLES32.glGetError();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
