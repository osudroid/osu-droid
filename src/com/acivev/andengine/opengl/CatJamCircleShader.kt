package com.acivev.andengine.opengl

import android.opengl.GLES20
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.GLState

/**
 * Single-texture GLSL shader for [com.acivev.ui.menu.main.KiaiCatJamSprite].
 *
 * The vertex shader passes two sets of UVs:
 * - `a_textureCoordinates` → atlas sub-region UVs for sampling the catjam frame
 * - `a_quadPos` (attribute [QUAD_POS_LOCATION]) → normalized 0-to-1 position across the full
 *   quad, always (0,0)→(1,1) regardless of the texture atlas region.
 *
 * The fragment shader uses the quad-position UV to compute the distance from the quad
 * center (0.5, 0.5) and smoothly discards pixels that fall outside the circular logo area,
 * clipping the animation to a circle without requiring any mask texture.
 */
class CatJamCircleShader private constructor() : ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER) {

    var uniformMVP = ShaderProgramConstants.LOCATION_INVALID; private set
    var uniformCatjam = ShaderProgramConstants.LOCATION_INVALID; private set
    var uniformColor = ShaderProgramConstants.LOCATION_INVALID; private set
    var uniformRadius = ShaderProgramConstants.LOCATION_INVALID; private set

    override fun link(pGLState: GLState) {
        GLES20.glBindAttribLocation(mProgramID, ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION, ShaderProgramConstants.ATTRIBUTE_POSITION)
        GLES20.glBindAttribLocation(mProgramID, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION, ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES)
        GLES20.glBindAttribLocation(mProgramID, QUAD_POS_LOCATION, QUAD_POS_ATTRIBUTE)

        super.link(pGLState)

        uniformMVP = getUniformLocation(ShaderProgramConstants.UNIFORM_MODELVIEWPROJECTIONMATRIX)
        uniformCatjam = getUniformLocation(ShaderProgramConstants.UNIFORM_TEXTURE_0)
        uniformColor = getUniformLocation(ShaderProgramConstants.UNIFORM_COLOR)
        uniformRadius = getUniformLocation("u_radius")
    }

    override fun resetForContextLoss() {
        super.resetForContextLoss()
        uniformMVP = ShaderProgramConstants.LOCATION_INVALID
        uniformCatjam = ShaderProgramConstants.LOCATION_INVALID
        uniformColor = ShaderProgramConstants.LOCATION_INVALID
        uniformRadius = ShaderProgramConstants.LOCATION_INVALID
    }

    companion object {
        /** GLSL vertex attribute location for the normalized quad-position (0→1). */
        const val QUAD_POS_LOCATION = 4

        private const val QUAD_POS_ATTRIBUTE = "a_quadPos"

        /** Singleton — safe to share across all catjam sprites. */
        val INSTANCE: CatJamCircleShader by lazy { CatJamCircleShader() }

        private val VERTEX_SHADER = """
            uniform mat4 u_modelViewProjectionMatrix;
            attribute vec4 a_position;
            attribute vec2 a_textureCoordinates;
            attribute vec2 a_quadPos;
            varying vec2 v_texCoord;
            varying vec2 v_quadPos;
            void main() {
                v_texCoord = a_textureCoordinates;
                v_quadPos  = a_quadPos;
                gl_Position = u_modelViewProjectionMatrix * a_position;
            }
        """.trimIndent()

        // u_radius: circle radius in quad-UV space (default 0.5 = fills whole quad).
        // A small smooth edge avoids aliasing at the circle rim.
        private val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture_0;
            uniform vec4 u_color;
            uniform float u_radius;
            varying vec2 v_texCoord;
            varying vec2 v_quadPos;
            void main() {
                float dist = length(v_quadPos - vec2(0.5, 0.5));
                float edge = u_radius;
                float clip = 1.0 - smoothstep(edge - 0.02, edge, dist);
                if (clip <= 0.0) discard;
                vec4 cat = texture2D(u_texture_0, v_texCoord);
                // Premultiplied alpha output: multiply ALL channels (including RGB) by the
                // final alpha so that when sprite alpha is 0, gl_FragColor = (0,0,0,0)
                // and the ONE blend factor adds nothing to the destination.
                float finalAlpha = cat.a * clip * u_color.a;
                gl_FragColor = vec4(cat.rgb * u_color.rgb * finalAlpha, finalAlpha);
            }
        """.trimIndent()
    }
}
