package com.acivev.andengine.opengl;

import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;

import android.opengl.GLES20;

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

    private static ExternalOESShaderProgram INSTANCE;

    public static ExternalOESShaderProgram getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExternalOESShaderProgram();
        }
        return INSTANCE;
    }

    // ===========================================================
    // Shader sources
    // ===========================================================

    public static final String VERTEXSHADER =
            "uniform mat4 " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX + ";\n" +
            "uniform mat4 u_stMatrix;\n" +
            "attribute vec4 " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
            "attribute vec2 " + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ";\n" +
            "varying vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
            "void main() {\n" +
            "    " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + " = " +
            "        (u_stMatrix * vec4(" + ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES + ", 0.0, 1.0)).xy;\n" +
            "    gl_Position = " + ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX +
            "        * " + ShaderProgramConstants.ATTRIBUTE_POSITION + ";\n" +
            "}";

    public static final String FRAGMENTSHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES " + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ";\n" +
            "uniform vec4 " + ShaderProgramConstants.UNIFORM_COLOR + ";\n" +
            "varying mediump vec2 " + ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ";\n" +
            "void main() {\n" +
            "    gl_FragColor = " + ShaderProgramConstants.UNIFORM_COLOR +
            "        * texture2D(" + ShaderProgramConstants.UNIFORM_TEXTURE_0 + ", " +
            ShaderProgramConstants.VARYING_TEXTURECOORDINATES + ");\n" +
            "}";

    // ===========================================================
    // Uniform locations (populated after link)
    // ===========================================================

    public static int sUniformMVPMatrixLocation  = ShaderProgramConstants.LOCATION_INVALID;
    public static int sUniformSTMatrixLocation   = ShaderProgramConstants.LOCATION_INVALID;
    public static int sUniformTexture0Location   = ShaderProgramConstants.LOCATION_INVALID;
    public static int sUniformColorLocation      = ShaderProgramConstants.LOCATION_INVALID;

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
        GLES20.glBindAttribLocation(this.mProgramID,
                ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION,
                ShaderProgramConstants.ATTRIBUTE_POSITION);
        GLES20.glBindAttribLocation(this.mProgramID,
                ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION,
                ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES);

        super.link(pGLState);

        sUniformMVPMatrixLocation = this.getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX);
        sUniformSTMatrixLocation  = this.getUniformLocation("u_stMatrix");
        sUniformTexture0Location  = this.getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURE_0);
        sUniformColorLocation     = this.getUniformLocation(ShaderProgramConstants.UNIFORM_COLOR);
    }

    // ===========================================================
    // bind / unbind  (required overrides – unused but kept for completeness)
    // ===========================================================

    @Override
    public void bind(final GLState pGLState,
                     final VertexBufferObjectAttributes pVertexBufferObjectAttributes) {
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
        super.bind(pGLState, pVertexBufferObjectAttributes);
    }

    @Override
    public void unbind(final GLState pGLState) {
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION);
        super.unbind(pGLState);
    }

    // ===========================================================
    // resetForContextLoss override
    // ===========================================================

    @Override
    public void resetForContextLoss() {
        super.resetForContextLoss();
        sUniformMVPMatrixLocation = ShaderProgramConstants.LOCATION_INVALID;
        sUniformSTMatrixLocation  = ShaderProgramConstants.LOCATION_INVALID;
        sUniformTexture0Location  = ShaderProgramConstants.LOCATION_INVALID;
        sUniformColorLocation     = ShaderProgramConstants.LOCATION_INVALID;
    }
}

