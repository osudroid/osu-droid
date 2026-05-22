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
public class PositionTextureCoordinatesShaderProgram extends ShaderProgram {
	// ===========================================================
	// Constants
	// ===========================================================

	private static PositionTextureCoordinatesShaderProgram INSTANCE;

	public static final String VERTEXSHADER =
			"#version 320 es\n" +
			"uniform mat4 " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + ";\n" +
			"in vec4 " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
			"in vec2 " + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ";\n" +
			"out vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
			"void main() {\n" +
			"	" + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + " = " + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ";\n" +
			"	gl_Position = " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + " * " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
			"}";

	public static final String FRAGMENTSHADER =
			"#version 320 es\n" +
			"precision lowp float;\n" +
			"uniform sampler2D " + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ";\n" +
			"in mediump vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
			"out vec4 fragColor;\n" +
			"void main() {\n" +
			"	fragColor = texture(" + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ", " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ");\n" +
			"}";

	// ===========================================================
	// Fields
	// ===========================================================

	private int mUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;
	private int mUniformTexture0Location  = ShaderProgramConstants.LOCATION_INVALID;

	public int getUniformMVPMatrixLocation()  { return mUniformMVPMatrixLocation; }
	public int getUniformTexture0Location()   { return mUniformTexture0Location; }

	// ===========================================================
	// Constructors
	// ===========================================================

	private PositionTextureCoordinatesShaderProgram() {
		super(PositionTextureCoordinatesShaderProgram.VERTEXSHADER, PositionTextureCoordinatesShaderProgram.FRAGMENTSHADER);
	}

	public static PositionTextureCoordinatesShaderProgram getInstance() {
		if(PositionTextureCoordinatesShaderProgram.INSTANCE == null) {
			PositionTextureCoordinatesShaderProgram.INSTANCE = new PositionTextureCoordinatesShaderProgram();
		}
		return PositionTextureCoordinatesShaderProgram.INSTANCE;
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

		mUniformMVPMatrixLocation = this.getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX);
		mUniformTexture0Location  = this.getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURE_0);
	}

	@Override
	public void resetForContextLoss() {
		super.resetForContextLoss();
		mUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;
		mUniformTexture0Location  = ShaderProgramConstants.LOCATION_INVALID;
	}

	@Override
	public void bind(final GLState pGLState, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);

		super.bind(pGLState, pVertexBufferObjectAttributes);

		GLES32.glUniformMatrix4fv(mUniformMVPMatrixLocation, 1, false, pGLState.getModelViewProjectionGLMatrix(), 0);
		GLES32.glUniform1i(mUniformTexture0Location, 0);
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
