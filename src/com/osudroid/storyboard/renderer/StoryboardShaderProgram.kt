package com.osudroid.storyboard.renderer

import android.opengl.GLES32
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.GLState

/**
 * The shader program of [StoryboardBatch].
 *
 * Vertex layout per vertex (8 floats / 32 bytes): `[x, y, u, v, r, g, b, a]` with fixed attribute
 * locations `0 = a_position`, `1 = a_texCoord`, `2 = a_color`.
 *
 * Extending [ShaderProgram] makes this program self-register for EGL context loss recovery via
 * [ShaderProgram.resetAllForContextLoss], so no manual reset is required.
 */
class StoryboardShaderProgram private constructor() : ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER) {
    private var mvpMatrixLocation = ShaderProgramConstants.LOCATION_INVALID
    private var textureLocation = ShaderProgramConstants.LOCATION_INVALID

    override fun link(pGLState: GLState) {
        GLES32.glBindAttribLocation(mProgramID, ATTRIBUTE_POSITION_LOCATION, "a_position")
        GLES32.glBindAttribLocation(mProgramID, ATTRIBUTE_TEXCOORD_LOCATION, "a_texCoord")
        GLES32.glBindAttribLocation(mProgramID, ATTRIBUTE_COLOR_LOCATION, "a_color")

        super.link(pGLState)

        mvpMatrixLocation = getUniformLocation("u_mvpMatrix")
        textureLocation = getUniformLocation("s_texture")
    }

    override fun resetForContextLoss() {
        super.resetForContextLoss()
        mvpMatrixLocation = ShaderProgramConstants.LOCATION_INVALID
        textureLocation = ShaderProgramConstants.LOCATION_INVALID
    }

    /**
     * Binds this program (compiling it if needed) and uploads the current model-view-projection
     * matrix and the texture unit.
     *
     * @param pGLState The [GLState] to bind through.
     */
    fun bind(pGLState: GLState) {
        bindProgram(pGLState)

        GLES32.glUniformMatrix4fv(mvpMatrixLocation, 1, false, pGLState.modelViewProjectionGLMatrix, 0)
        GLES32.glUniform1i(textureLocation, 0)
    }

    companion object {
        const val ATTRIBUTE_POSITION_LOCATION = 0
        const val ATTRIBUTE_TEXCOORD_LOCATION = 1
        const val ATTRIBUTE_COLOR_LOCATION = 2

        private const val VERTEX_SHADER = """#version 320 es
uniform mat4 u_mvpMatrix;
in vec2 a_position;
in vec2 a_texCoord;
in vec4 a_color;
out vec2 v_texCoord;
out vec4 v_color;
void main() {
    gl_Position = u_mvpMatrix * vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
    v_color = a_color;
}
"""

        private const val FRAGMENT_SHADER = """#version 320 es
precision mediump float;
uniform sampler2D s_texture;
in vec2 v_texCoord;
in vec4 v_color;
out vec4 fragColor;
void main() {
    fragColor = v_color * texture(s_texture, v_texCoord);
}
"""

        private var instance: StoryboardShaderProgram? = null

        @JvmStatic
        fun getInstance(): StoryboardShaderProgram {
            if (instance == null) {
                instance = StoryboardShaderProgram()
            }

            return instance!!
        }
    }
}
