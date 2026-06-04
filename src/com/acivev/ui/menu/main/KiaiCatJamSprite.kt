package com.acivev.ui.menu.main

import android.opengl.GLES32
import com.acivev.andengine.opengl.CatJamCircleShader
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.sprite.ScaleType
import com.reco1l.andengine.sprite.UISprite
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState

/**
 * A sprite that renders a catjam animation frame clipped to a circle.
 *
 * Uses [com.acivev.andengine.opengl.CatJamCircleShader] which discards fragments outside a circular radius in
 * normalized quad-UV space (0→1). No mask texture is needed — the circle is computed
 * mathematically in the fragment shader.
 *
 * The sprite should be sized to match the logo exactly (same width/height) so that
 * the UV-space circle radius of 0.5 coincides with the logo's circle boundary.
 */
class KiaiCatJamSprite(val frames: Array<TextureRegion>) : UISprite() {

    /**
     * Circle clip radius in normalized quad-UV space [0..1].
     * 0.5 means the circle fills the full quad (touching all four sides at the midpoints).
     * Reduce slightly (e.g. 0.48) to add extra inset margin.
     */
    var clipRadius: Float = 0.5f

    /** Static full-quad position buffer bound at [com.acivev.andengine.opengl.CatJamCircleShader.QUAD_POS_LOCATION]. */
    private val quadPosBuffer = QuadPosBuffer()

    init {
        scaleType = ScaleType.Stretch
        if (frames.isNotEmpty()) textureRegion = frames[0]
        alpha = 0f
    }

    override fun onTextureRegionChanged() {
        textureRegion ?: return
        // CatJamCircleShader always outputs premultiplied alpha (rgb * a, a),
        // so PreMultiply blending must be used unconditionally regardless of
        // whether the underlying texture was uploaded as premultiplied.
        blendInfo = BlendInfo.PreMultiply
        uvBuffer.update(this)
        requestBufferUpdate()
    }


    override fun onBindShader(pGLState: GLState) {
        val shader = CatJamCircleShader.INSTANCE
        shader.bindProgram(pGLState)

        if (shader.uniformMVP >= 0)
            GLES32.glUniformMatrix4fv(shader.uniformMVP, 1, false, pGLState.modelViewProjectionGLMatrix, 0)
        if (shader.uniformCatjam >= 0)
            GLES32.glUniform1i(shader.uniformCatjam, 0)
        if (shader.uniformColor >= 0)
            GLES32.glUniform4f(shader.uniformColor, drawRed, drawGreen, drawBlue, drawAlpha)
        if (shader.uniformRadius >= 0)
            GLES32.glUniform1f(shader.uniformRadius, clipRadius)

        // This shader does not use per-vertex color
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        // Catjam atlas UVs at attribute 3
        uvBuffer.beginDraw(pGLState)

        // Normalised quad-position (0→1) at attribute 4
        quadPosBuffer.bindAsQuadPosAttribute()
    }

    override fun onDrawBuffer(pGLState: GLState) {
        textureRegion?.texture?.bind(pGLState)
        super.onDrawBuffer(pGLState)
        GLES32.glDisableVertexAttribArray(CatJamCircleShader.QUAD_POS_LOCATION)
    }


    /**
     * Static 2D position buffer that maps (0,0)→(1,1) over the quad, bound at
     * [CatJamCircleShader.Companion.QUAD_POS_LOCATION].
     * Vertex order matches [SpriteVBO]: top-left, bottom-left, top-right, bottom-right.
     */
    private class QuadPosBuffer : VertexBuffer(
        drawTopology = GLES32.GL_TRIANGLE_STRIP,
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GLES32.GL_STATIC_DRAW
    ) {
        init {
            putVertex(0, 0f, 0f) // top-left
            putVertex(1, 0f, 1f) // bottom-left
            putVertex(2, 1f, 0f) // top-right
            putVertex(3, 1f, 1f) // bottom-right
            invalidateOnHardware()
        }

        fun bindAsQuadPosAttribute() {
            bindAndUpload()
            GLES32.glVertexAttribPointer(
                CatJamCircleShader.QUAD_POS_LOCATION,
                VERTEX_2D, GLES32.GL_FLOAT, false, 0, 0)
            GLES32.glEnableVertexAttribArray(CatJamCircleShader.QUAD_POS_LOCATION)
        }
    }

    companion object {
        const val GRID_COLS = 13
        const val FRAME_SIZE = 112
        const val TOTAL_FRAMES = 143
    }
}