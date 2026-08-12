package com.acivev.andengine.opengl;

import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES32;

/**
 * Shader program for rendering Android {@link android.graphics.SurfaceTexture} video frames,
 * which are uploaded to a {@code GL_TEXTURE_EXTERNAL_OES} texture target.
 *
 * The vertex shader applies the SurfaceTexture ST-transform matrix to correctly map UV
 * coordinates (accounts for the Y-flip and any other orientation adjustments provided by
 * the hardware decoder / SurfaceTexture).
 */
public class ExternalOESShaderProgram extends ShaderProgram {

    // ===========================================================
    // Singleton
    // ===========================================================

    private static volatile ExternalOESShaderProgram INSTANCE;

    public static ExternalOESShaderProgram getInstance() {
        if (INSTANCE == null) {
            synchronized (ExternalOESShaderProgram.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ExternalOESShaderProgram();
                }
            }
        }
        return INSTANCE;
    }

    // ===========================================================
    // Shader sources
    // ===========================================================

    public static final String VERTEXSHADER =
            "#version 320 es\n" +
            "uniform mat4 " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + ";\n" +
            "uniform mat4 u_stMatrix;\n" +
            "in vec4 " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
            "in vec2 " + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ";\n" +
            "out vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
            "void main() {\n" +
            "    " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + " = " +
            "        (u_stMatrix * vec4(" + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ", 0.0, 1.0)).xy;\n" +
            "    gl_Position = " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX +
            "        * " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
            "}";

    public static final String FRAGMENTSHADER =
            "#version 320 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES " + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ";\n" +
            "uniform vec4 " + ShaderProgramConstants.UNIFORM_COLOR + ";\n" +
            "in mediump vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "    fragColor = " + ShaderProgramConstants.UNIFORM_COLOR +
            "        * texture(" + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ", " +
            ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ");\n" +
            "}";

    // ===========================================================
    // Uniform locations (populated after link, private to prevent external mutation)
    // ===========================================================

    private int mUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;
    private int mUniformSTMatrixLocation  = ShaderProgramConstants.LOCATION_INVALID;
    private int mUniformTexture0Location  = ShaderProgramConstants.LOCATION_INVALID;
    private int mUniformColorLocation     = ShaderProgramConstants.LOCATION_INVALID;

    public int getUniformMVPMatrixLocation()  { return mUniformMVPMatrixLocation; }
    public int getUniformSTMatrixLocation()   { return mUniformSTMatrixLocation; }
    public int getUniformTexture0Location()   { return mUniformTexture0Location; }
    public int getUniformColorLocation()      { return mUniformColorLocation; }


    // ===========================================================
    // Constructor
    // ===========================================================

    private ExternalOESShaderProgram() {
        super(VERTEXSHADER, FRAGMENTSHADER);
    }

    // ===========================================================
    // Link
    // ===========================================================

    @Override
    protected void link(final GLState pGLState) throws ShaderProgramLinkException {
        GLES32.glBindAttribLocation(this.mProgramID,
                ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
                ShaderProgramConstants.ATTRIBUTE_POSITION);
        GLES32.glBindAttribLocation(this.mProgramID,
                ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION,
                ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES);

        super.link(pGLState);

        mUniformMVPMatrixLocation = this.getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX);
        mUniformSTMatrixLocation  = this.getUniformLocation("u_stMatrix");
        mUniformTexture0Location  = this.getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURE_0);
        mUniformColorLocation     = this.getUniformLocation(ShaderProgramConstants.UNIFORM_COLOR);
    }

    // ===========================================================
    // bind / unbind  (required overrides – unused but kept for completeness)
    // ===========================================================

    @Override
    public void bind(final GLState pGLState,
                     final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
        super.bind(pGLState, pVertexBufferObjectAttributes);
    }

    @Override
    public void unbind(final GLState pGLState) {
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
        super.unbind(pGLState);
    }

    // ===========================================================
    // resetForContextLoss override
    // ===========================================================

    @Override
    public void resetForContextLoss() {
        super.resetForContextLoss();
        mUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;
        mUniformSTMatrixLocation  = ShaderProgramConstants.LOCATION_INVALID;
        mUniformTexture0Location  = ShaderProgramConstants.LOCATION_INVALID;
        mUniformColorLocation     = ShaderProgramConstants.LOCATION_INVALID;
    }
}

