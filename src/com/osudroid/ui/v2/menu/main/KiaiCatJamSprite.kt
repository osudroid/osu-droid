package com.osudroid.ui.v2.menu.main

import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.sprite.ScaleType
import com.reco1l.andengine.sprite.UISprite
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.MeshProgram
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState

/**
 * A sprite that renders a catjam animation frame clipped to a circle.
 *
 * Draws through a [MeshProgram] whose fragment shader fades out fragments outside a circular
 * radius in normalized quad space (0 to 1, the `vLocal` coordinate). No mask texture is needed:
 * the circle is computed mathematically in the fragment shader.
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

    // Draws with its own circle-clipping program, never through the quad batch.
    override val isBatchable: Boolean get() = false

    init {
        scaleType = ScaleType.Stretch
        if (frames.isNotEmpty()) textureRegion = frames[0]
        alpha = 0f
    }

    override fun onTextureRegionChanged() {
        textureRegion ?: return
        // The program always outputs premultiplied alpha (rgb * a, a),
        // so PreMultiply blending must be used unconditionally regardless of
        // whether the underlying texture was uploaded as premultiplied.
        blendInfo = BlendInfo.PreMultiply
        uvBuffer.update(this)
        requestBufferUpdate()
    }

    override fun onDrawBuffer(gl: GLState) {
        val texture = textureRegion?.texture ?: return
        val positions = buffer?.data ?: return

        val clipProgram = program
        clipProgram.set(RADIUS, clipRadius)
        MeshRenderer.drawProgramQuad(
            clipProgram, positions, uvBuffer.data,
            gl.modelViewGLMatrix, gl.projectionGLMatrix,
            BatchTexture.of(texture),
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            meshBlendMode()
        )
    }

    companion object {
        const val GRID_COLS = 13
        const val FRAME_SIZE = 112
        const val TOTAL_FRAMES = 143

        private const val RADIUS = "uRadius"

        // uRadius: circle radius in quad space (0.5 fills the whole quad). A small smooth edge
        // avoids aliasing at the circle rim. The atlas is uploaded with premultiplied alpha, so the
        // sampled rgb already includes its alpha; only the clip and the sprite alpha are applied to
        // rgb, while alpha tracks the full coverage and opacity product.
        private const val FRAGMENT_SHADER = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
uniform float uRadius;
in vec2 vTexCoord;
in vec2 vLocal;
in vec4 vColor;
out vec4 fragColor;
void main() {
    float dist = length(vLocal - vec2(0.5, 0.5));
    float clip = 1.0 - smoothstep(uRadius - 0.02, uRadius, dist);
    if (clip <= 0.0) discard;
    vec4 cat = texture(uTexture, vTexCoord);
    float colorScale = clip * vColor.a;
    fragColor = vec4(cat.rgb * vColor.rgb * colorScale, cat.a * colorScale);
}
"""

        private var programInstance: MeshProgram? = null

        // Shared by all cat sprites; created at the first draw, the program restores itself after a context loss.
        private val program: MeshProgram
            get() = programInstance ?: MeshProgram("kiai-cat", FRAGMENT_SHADER, listOf(RADIUS)).also { programInstance = it }

        /** Disposes the GL program; the next draw creates it again. For shutdown and tests. */
        fun releaseProgram() {
            programInstance?.releaseResources()
            programInstance = null
        }
    }
}
