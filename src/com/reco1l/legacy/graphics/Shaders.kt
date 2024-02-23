package com.reco1l.legacy.graphics

import android.opengl.GLES20
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram.*
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants.*
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
