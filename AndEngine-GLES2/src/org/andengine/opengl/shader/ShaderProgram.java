package org.andengine.opengl.shader;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramCompileException;
import org.andengine.opengl.shader.exception.ShaderProgramException;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.shader.source.IShaderSource;
import org.andengine.opengl.shader.source.StringShaderSource;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES32;


/**
 * (c) Zynga 2011
 *
 * @author Nicolas Gramlich <ngramlich@zynga.com>
 * @since 19:56:34 - 05.08.2011
 */
public class ShaderProgram {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int[] HARDWAREID_CONTAINER = new int[1];
	private static final int[] PARAMETERS_CONTAINER = new int[1];
	private static final int[] LENGTH_CONTAINER = new int[1];
	private static final int[] SIZE_CONTAINER = new int[1];
	private static final int[] TYPE_CONTAINER = new int[1];
	private static final int NAME_CONTAINER_SIZE = 64;
	private static final byte[] NAME_CONTAINER = new byte[ShaderProgram.NAME_CONTAINER_SIZE];

	// BEGIN osu!droid modified
	/** All ShaderProgram instances – used to reset them all on EGL context loss. */
	private static final List<ShaderProgram> sAllInstances = new CopyOnWriteArrayList<>();
	// BEGIN osu!droid modified

	// ===========================================================
	// Fields
	// ===========================================================

	protected final IShaderSource mVertexShaderSource;
	protected final IShaderSource mFragmentShaderSource;

	protected int mProgramID = -1;

	protected boolean mCompiled;

	protected final HashMap<String, Integer> mUniformLocations = new HashMap<String, Integer>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public ShaderProgram(final String pVertexShaderSource, final String pFragmentShaderSource) {
		this(new StringShaderSource(pVertexShaderSource), new StringShaderSource(pFragmentShaderSource));
	}

	public ShaderProgram(final IShaderSource pVertexShaderSource, final IShaderSource pFragmentShaderSource) {
		this.mVertexShaderSource = pVertexShaderSource;
		this.mFragmentShaderSource = pFragmentShaderSource;
		// BEGIN osu!droid modified
		sAllInstances.add(this);
		// END osu!droid modified
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean isCompiled() {
		return this.mCompiled;
	}

	public void setCompiled(final boolean pCompiled) {
		this.mCompiled = pCompiled;
	}


	public int getUniformLocation(final String pUniformName) {
		final Integer location = this.mUniformLocations.get(pUniformName);
		if(location != null) {
			return location.intValue();
		} else {
			throw new ShaderProgramException("Unexpected uniform: '" + pUniformName + "'. Existing uniforms: " + this.mUniformLocations.toString());
		}
	}

	public int getUniformLocationOptional(final String pUniformName) {
		final Integer location = this.mUniformLocations.get(pUniformName);
		if(location != null) {
			return location.intValue();
		} else {
			return ShaderProgramConstants.LOCATION_INVALID;
		}
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void bind(final GLState pGLState, final VertexBufferObjectAttributes pVertexBufferObjectAttributes) throws ShaderProgramException {
		if(!this.mCompiled) {
			this.compile(pGLState);
		}
		pGLState.useProgram(this.mProgramID);

		pVertexBufferObjectAttributes.glVertexAttribPointers();
	}

	/**
	 * Binds only the shader program (compiling if needed), without applying any vertex buffer object attributes.
	 * Use this when vertex attributes are set up manually via glVertexAttribPointer.
	 */
	public void bindProgram(final GLState pGLState) throws ShaderProgramException {
		if(!this.mCompiled) {
			this.compile(pGLState);
		}
		pGLState.useProgram(this.mProgramID);
	}

	public void unbind(final GLState pGLState) throws ShaderProgramException {
//		pGLState.useProgram(0); // TODO Does this have an positive/negative impact on performance?
	}

	public void delete(final GLState pGLState) {
		if(this.mCompiled) {
			this.mCompiled = false;
			pGLState.deleteProgram(this.mProgramID);
			this.mProgramID = -1;
		}
	}

	/**
	 * Called when the EGL context has been lost (e.g. app was backgrounded long enough
	 * for the driver to destroy it). Marks the program as needing recompilation so that
	 * the next {@link #bindProgram} / {@link #bind} call will recompile and relink it
	 * against the new context.
	 */
	public void resetForContextLoss() {
		this.mCompiled = false;
		this.mProgramID = -1;
		this.mUniformLocations.clear();
	}

	/**
	 * Resets every known {@link ShaderProgram} instance for EGL context loss recovery.
	 * Call this from {@link org.andengine.opengl.view.EngineRenderer#onSurfaceCreated}.
	 */
	public static void resetAllForContextLoss() {
		for (final ShaderProgram program : sAllInstances) {
			program.resetForContextLoss();
		}
	}

	protected void compile(final GLState pGLState) throws ShaderProgramException {
		final String vertexShaderSource = this.mVertexShaderSource.getShaderSource(pGLState);
		final int vertexShaderID = ShaderProgram.compileShader(vertexShaderSource, GLES32.GL_VERTEX_SHADER);

		final String fragmentShaderSource = this.mFragmentShaderSource.getShaderSource(pGLState);
		final int fragmentShaderID = ShaderProgram.compileShader(fragmentShaderSource, GLES32.GL_FRAGMENT_SHADER);

		this.mProgramID = GLES32.glCreateProgram();
		GLES32.glAttachShader(this.mProgramID, vertexShaderID);
		GLES32.glAttachShader(this.mProgramID, fragmentShaderID);

		try {
			this.link(pGLState);
		} catch (final ShaderProgramLinkException e) {
			throw new ShaderProgramLinkException("VertexShaderSource:\n##########################\n" + vertexShaderSource + "\n##########################" + "\n\nFragmentShaderSource:\n##########################\n" + fragmentShaderSource + "\n##########################", e);
		}

		GLES32.glDeleteShader(vertexShaderID);
		GLES32.glDeleteShader(fragmentShaderID);
	}

	protected void link(final GLState pGLState) throws ShaderProgramLinkException {
		GLES32.glLinkProgram(this.mProgramID);

		GLES32.glGetProgramiv(this.mProgramID, GLES32.GL_LINK_STATUS, ShaderProgram.HARDWAREID_CONTAINER, 0);
		if(ShaderProgram.HARDWAREID_CONTAINER[0] == 0) {
			throw new ShaderProgramLinkException(GLES32.glGetProgramInfoLog(this.mProgramID));
		}

		this.initUniformLocations();

		this.mCompiled = true;
	}

	private static int compileShader(final String pSource, final int pType) throws ShaderProgramException {
		final int shaderID = GLES32.glCreateShader(pType);
		if(shaderID == 0) {
			throw new ShaderProgramException("Could not create Shader of type: '" + pType + '"');
		}

		GLES32.glShaderSource(shaderID, pSource);
		GLES32.glCompileShader(shaderID);

		GLES32.glGetShaderiv(shaderID, GLES32.GL_COMPILE_STATUS, ShaderProgram.HARDWAREID_CONTAINER, 0);
		if(ShaderProgram.HARDWAREID_CONTAINER[0] == 0) {
			throw new ShaderProgramCompileException(GLES32.glGetShaderInfoLog(shaderID), pSource);
		}
		return shaderID;
	}

	private void initUniformLocations() throws ShaderProgramLinkException {
		this.mUniformLocations.clear();

		ShaderProgram.PARAMETERS_CONTAINER[0] = 0;
		GLES32.glGetProgramiv(this.mProgramID, GLES32.GL_ACTIVE_UNIFORMS, ShaderProgram.PARAMETERS_CONTAINER, 0);
		final int numUniforms = ShaderProgram.PARAMETERS_CONTAINER[0];

		for(int i = 0; i < numUniforms; i++) {
			GLES32.glGetActiveUniform(this.mProgramID, i, ShaderProgram.NAME_CONTAINER_SIZE, ShaderProgram.LENGTH_CONTAINER, 0, ShaderProgram.SIZE_CONTAINER, 0, ShaderProgram.TYPE_CONTAINER, 0, ShaderProgram.NAME_CONTAINER, 0);

			int length = ShaderProgram.LENGTH_CONTAINER[0];

			/* Some drivers do not report the actual length here, but zero. Then the name is '\0' terminated. */
			if(length == 0) {
				while((length < ShaderProgram.NAME_CONTAINER_SIZE) && (ShaderProgram.NAME_CONTAINER[length] != '\0')) {
					length++;
				}
			}

			String name = new String(ShaderProgram.NAME_CONTAINER, 0, length);
			int location = GLES32.glGetUniformLocation(this.mProgramID, name);

			if(location == ShaderProgramConstants.LOCATION_INVALID) {
				/* Some drivers do not report an incorrect length. Then the name is '\0' terminated. */
				length = 0;
				while(length < ShaderProgram.NAME_CONTAINER_SIZE && ShaderProgram.NAME_CONTAINER[length] != '\0') {
					length++;
				}

				name = new String(ShaderProgram.NAME_CONTAINER, 0, length);
				location = GLES32.glGetUniformLocation(this.mProgramID, name);

				if(location == ShaderProgramConstants.LOCATION_INVALID) {
					throw new ShaderProgramLinkException("Invalid location for uniform: '" + name + "'.");
				}
			}

			this.mUniformLocations.put(name, location);
		}
	}


	public void setUniform(final String pUniformName, final float[] pGLMatrix) {
		GLES32.glUniformMatrix4fv(this.getUniformLocation(pUniformName), 1, false, pGLMatrix, 0);
	}

	public void setUniformOptional(final String pUniformName, final float[] pGLMatrix) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES32.glUniformMatrix4fv(location, 1, false, pGLMatrix, 0);
		}
	}

	public void setUniform(final String pUniformName, final float pX) {
		GLES32.glUniform1f(this.getUniformLocation(pUniformName), pX);
	}

	public void setUniformOptional(final String pUniformName, final float pX) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES32.glUniform1f(location, pX);
		}
	}

	public void setUniform(final String pUniformName, final float pX, final float pY) {
		GLES32.glUniform2f(this.getUniformLocation(pUniformName), pX, pY);
	}

	public void setUniformOptional(final String pUniformName, final float pX, final float pY) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES32.glUniform2f(location, pX, pY);
		}
	}

	public void setUniform(final String pUniformName, final float pX, final float pY, final float pZ) {
		GLES32.glUniform3f(this.getUniformLocation(pUniformName), pX, pY, pZ);
	}

	public void setUniformOptional(final String pUniformName, final float pX, final float pY, final float pZ) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES32.glUniform3f(location, pX, pY, pZ);
		}
	}

	public void setUniform(final String pUniformName, final float pX, final float pY, final float pZ, final float pW) {
		GLES32.glUniform4f(this.getUniformLocation(pUniformName), pX, pY, pZ, pW);
	}

	public void setUniformOptional(final String pUniformName, final float pX, final float pY, final float pZ, final float pW) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES32.glUniform4f(location, pX, pY, pZ, pW);
		}
	}

	/**
	 * @param pUniformName
	 * @param pTexture the index of the Texture to use. Similar to {@link GLES20#GL_TEXTURE0}, {@link GLES20#GL_TEXTURE1}, ... except that it is <b><code>0</code></b> based.
	 */
	public void setTexture(final String pUniformName, final int pTexture) {
		GLES32.glUniform1i(this.getUniformLocation(pUniformName), pTexture);
	}

	/**
	 * @param pUniformName
	 * @param pTexture the index of the Texture to use. Similar to {@link GLES20#GL_TEXTURE0}, {@link GLES20#GL_TEXTURE1}, ... except that it is <b><code>0</code></b> based.
	 */
	public void setTextureOptional(final String pUniformName, final int pTexture) {
		final int location = this.getUniformLocationOptional(pUniformName);
		if(location != ShaderProgramConstants.LOCATION_INVALID) {
			GLES32.glUniform1i(location, pTexture);
		}
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
