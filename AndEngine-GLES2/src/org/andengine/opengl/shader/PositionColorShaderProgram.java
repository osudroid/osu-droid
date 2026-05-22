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
public class PositionColorShaderProgram extends ShaderProgram {
	// ===========================================================
	// Constants
	// ===========================================================

	private static PositionColorShaderProgram INSTANCE;

	public static final String VERTEXSHADER =
			"#version 320 es\n" +
			"uniform mat4 " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + ";\n" +
			"in vec4 " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
			"in vec4 " + ShaderProgramConstants.ATTRIBUTE_COLOR + ";\n" +
			"out vec4 " + ShaderProgramConstants.VARYING_COLOR + ";\n" +
			"void main() {\n" +
			"	gl_Position = " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + " * " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
			"	" + ShaderProgramConstants.VARYING_COLOR + " = " + ShaderProgramConstants.ATTRIBUTE_COLOR + ";\n" +
			"}";

	public static final String FRAGMENTSHADER =
			"#version 320 es\n" +
			"precision lowp float;\n" +
			"in vec4 " + ShaderProgramConstants.VARYING_COLOR + ";\n" +
			"out vec4 fragColor;\n" +
			"void main() {\n" +
			"	fragColor = " + ShaderProgramConstants.VARYING_COLOR + ";\n" +
			"}";

	// ===========================================================
	// Fields  (private instance — reset on context loss)
	// ===========================================================

	private int mUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;

	public int getUniformMVPMatrixLocation() { return mUniformMVPMatrixLocation; }

	// ===========================================================
	// Constructors
	// ===========================================================

	private PositionColorShaderProgram() {
		super(PositionColorShaderProgram.VERTEXSHADER, PositionColorShaderProgram.FRAGMENTSHADER);
	}

	public static PositionColorShaderProgram getInstance() {
		if(PositionColorShaderProgram.INSTANCE == null) {
			PositionColorShaderProgram.INSTANCE = new PositionColorShaderProgram();
		}
		return PositionColorShaderProgram.INSTANCE;
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
		GLES32.glBindAttribLocation(this.mProgramID, ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION, ShaderProgramConstants.ATTRIBUTE_COLOR);

		super.link(pGLState);

		mUniformMVPMatrixLocation = this.getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX);
	}

	@Override
	public void bind(final GLState pGLState, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
		GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION);

		super.bind(pGLState, pVertexBufferObjectAttributes);

		GLES32.glUniformMatrix4fv(mUniformMVPMatrixLocation, 1, false, pGLState.getModelViewProjectionGLMatrix(), 0);
	}

	@Override
	public void unbind(final GLState pGLState) {
		GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION);
		
		super.unbind(pGLState);
	}

	@Override
	public void resetForContextLoss() {
		super.resetForContextLoss();
		mUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
