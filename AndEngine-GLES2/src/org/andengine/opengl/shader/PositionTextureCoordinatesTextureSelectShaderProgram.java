package org.andengine.opengl.shader;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES32;

/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 13:56:44 - 25.08.2011
 */
public class PositionTextureCoordinatesTextureSelectShaderProgram extends ShaderProgram {
	// ===========================================================
	// Constants
	// ===========================================================

	private static PositionTextureCoordinatesTextureSelectShaderProgram INSTANCE;

	public static final String VERTEXSHADER = PositionTextureCoordinatesShaderProgram.VERTEXSHADER;

	public static final String FRAGMENTSHADER =
			"#version 320 es\n" +
			"precision lowp float;\n" +
			"uniform sampler2D " + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ";\n" +
			"uniform sampler2D " + ShaderProgramConstants.UNIFORM_TEXTURE_1 + ";\n" +
			"uniform bool " + ShaderProgramConstants.UNIFORM_TEXTURESELECT_TEXTURE_0 + ";\n" +
			"in mediump vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
			"out vec4 fragColor;\n" +
			"void main() {\n" +
			"	if(" + ShaderProgramConstants.UNIFORM_TEXTURESELECT_TEXTURE_0 + ") {\n" +
			"		fragColor = texture(" + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ", " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ");\n" +
			"	} else {\n" +
			"		fragColor = texture(" + ShaderProgramConstants.UNIFORM_TEXTURE_1 + ", " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ");\n" +
			"	}\n" +
			"}";

	// ===========================================================
	// Fields
	// ===========================================================

	private int mUniformMVPMatrixLocation         = ShaderProgramConstants.LOCATION_INVALID;
	private int mUniformTexture0Location          = ShaderProgramConstants.LOCATION_INVALID;
	private int mUniformTexture1Location          = ShaderProgramConstants.LOCATION_INVALID;
	private int mUniformTextureSelectTex0Location = ShaderProgramConstants.LOCATION_INVALID;

	public int getUniformMVPMatrixLocation()         { return mUniformMVPMatrixLocation; }
	public int getUniformTexture0Location()          { return mUniformTexture0Location; }
	public int getUniformTexture1Location()          { return mUniformTexture1Location; }
	public int getUniformTextureSelectTex0Location() { return mUniformTextureSelectTex0Location; }

	// ===========================================================
	// Constructors
	// ===========================================================

	private PositionTextureCoordinatesTextureSelectShaderProgram() {
		super(PositionTextureCoordinatesTextureSelectShaderProgram.VERTEXSHADER, PositionTextureCoordinatesTextureSelectShaderProgram.FRAGMENTSHADER);
	}

	public static PositionTextureCoordinatesTextureSelectShaderProgram getInstance() {
		if(PositionTextureCoordinatesTextureSelectShaderProgram.INSTANCE == null) {
			PositionTextureCoordinatesTextureSelectShaderProgram.INSTANCE = new PositionTextureCoordinatesTextureSelectShaderProgram();
		}
		return PositionTextureCoordinatesTextureSelectShaderProgram.INSTANCE;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void link(final GLState pGLState) throws ShaderProgramLinkException {
		GLES32.glBindAttribLocation(this.mProgramID, ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION);
		GLES32.glBindAttribLocation(this.mProgramID, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES);

		super.link(pGLState);

		mUniformMVPMatrixLocation         = this.getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX);
		mUniformTexture0Location          = this.getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURE_0);
		mUniformTexture1Location          = this.getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURE_1);
		mUniformTextureSelectTex0Location = this.getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURESELECT_TEXTURE_0);
	}

	@Override
	public void resetForContextLoss() {
		super.resetForContextLoss();
		mUniformMVPMatrixLocation         = ShaderProgramConstants.LOCATION_INVALID;
		mUniformTexture0Location          = ShaderProgramConstants.LOCATION_INVALID;
		mUniformTexture1Location          = ShaderProgramConstants.LOCATION_INVALID;
		mUniformTextureSelectTex0Location = ShaderProgramConstants.LOCATION_INVALID;
	}

	@Override
	public void bind(final GLState pGLState, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

		super.bind(pGLState, pVertexBufferObjectAttributes);

		GLES32.glUniformMatrix4fv(mUniformMVPMatrixLocation, 1, false, pGLState.getModelViewProjectionGLMatrix(), 0);
		GLES32.glUniform1i(mUniformTexture0Location, 0);
		GLES32.glUniform1i(mUniformTexture1Location, 1);
	}

	@Override
	public void unbind(final GLState pGLState) {
		GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
		
		super.unbind(pGLState);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
