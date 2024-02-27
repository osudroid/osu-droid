package com.reco1l.legacy.graphics

import android.opengl.GLES20
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram.FRAGMENTSHADER
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram.VERTEXSHADER
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_COLOR
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES
import org.andengine.opengl.shader.constants.ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION
import org.andengine.opengl.shader.constants.ShaderProgramConstants.LOCATION_INVALID
import org.andengine.opengl.shader.constants.ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX
import org.andengine.opengl.shader.constants.ShaderProgramConstants.UNIFORM_TEXTURE_0
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes

/**
 * Same as [PositionColorTextureCoordinatesShaderProgram] but with OES External extension.
 * This as well handles coloring and texture coordinates.
 */
object ExternalTextureShaderProgram : ShaderProgram(
    VERTEXSHADER,

    // The fragment shader is the same than in PositionColorTextureCoordinatesShaderProgram but we
    // have to replace the sampler2D with samplerExternalOES and enable the extension as well.
    "#extension GL_OES_EGL_image_external : require\n" + FRAGMENTSHADER.replace("sampler2D", "samplerExternalOES"),
)
{

    private var sUniformModelViewPositionMatrixLocation = LOCATION_INVALID
    private var sUniformTexture0Location = LOCATION_INVALID


    override fun link(gl: GLState)
    {
        GLES20.glBindAttribLocation(mProgramID, ATTRIBUTE_POSITION_LOCATION, ATTRIBUTE_POSITION)
        GLES20.glBindAttribLocation(mProgramID, ATTRIBUTE_COLOR_LOCATION, ATTRIBUTE_COLOR)
        GLES20.glBindAttribLocation(mProgramID, ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ATTRIBUTE_TEXTURECOORDINATES)

        super.link(gl)

        sUniformModelViewPositionMatrixLocation = getUniformLocation(UNIFORM_MODELVIEWPROJECTIONMATRIX)
        sUniformTexture0Location = getUniformLocation(UNIFORM_TEXTURE_0)
    }

    override fun bind(gl: GLState, attributes: VertexBufferObjectAttributes)
    {
        super.bind(gl, attributes)

        GLES20.glUniformMatrix4fv(sUniformModelViewPositionMatrixLocation, 1, false, gl.modelViewProjectionGLMatrix, 0)
        GLES20.glUniform1i(sUniformTexture0Location, 0)
    }

}


/**
 * Same as [PositionColorShaderProgram] but allows to override fragments alpha via an uniform.
 */
object AlphaOverrideShaderProgram : ShaderProgram(
    PositionColorShaderProgram.VERTEXSHADER,
    """
        precision lowp float;
        varying vec4 ${ShaderProgramConstants.VARYING_COLOR};
        uniform float ${ShaderProgramConstants.UNIFORM_ALPHA};
        
        void main() {
            vec4 color = ${ShaderProgramConstants.VARYING_COLOR};
            color.a = ${ShaderProgramConstants.UNIFORM_ALPHA};
            gl_FragColor = color;
        }  
    """.trimIndent()
)
{

    private var sUniformModelViewPositionMatrixLocation = LOCATION_INVALID
    private var sUniformAlphaLocation = LOCATION_INVALID


    override fun link(gl: GLState)
    {
        GLES20.glBindAttribLocation(mProgramID, ATTRIBUTE_POSITION_LOCATION, ATTRIBUTE_POSITION)
        GLES20.glBindAttribLocation(mProgramID, ATTRIBUTE_COLOR_LOCATION, ATTRIBUTE_COLOR)

        super.link(gl)

        sUniformModelViewPositionMatrixLocation = getUniformLocation(UNIFORM_MODELVIEWPROJECTIONMATRIX)
        sUniformAlphaLocation = getUniformLocation(ShaderProgramConstants.UNIFORM_ALPHA)
    }

    override fun bind(gl: GLState, pVertexBufferObjectAttributes: VertexBufferObjectAttributes)
    {
        GLES20.glDisableVertexAttribArray(ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

        super.bind(gl, pVertexBufferObjectAttributes)

        GLES20.glUniformMatrix4fv(sUniformModelViewPositionMatrixLocation, 1, false, gl.getModelViewProjectionGLMatrix(), 0)
    }

    override fun unbind(gl: GLState)
    {
        GLES20.glEnableVertexAttribArray(ATTRIBUTE_TEXTURECOORDINATES_LOCATION)
        super.unbind(gl)
    }

    fun setAlphaUniform(alpha: Float) = GLES20.glUniform1f(sUniformAlphaLocation, alpha)
}
