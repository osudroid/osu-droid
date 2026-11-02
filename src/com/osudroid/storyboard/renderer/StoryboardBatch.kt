package com.osudroid.storyboard.renderer

import android.opengl.GLES20
import com.osudroid.storyboard.playback.PlayableSprite
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesPipeline
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState

/**
 * Builds the quads of a storyboard and hands them to the engine's [QuadBatcher], which draws
 * consecutive quads of one texture and blend mode with a single draw call.
 *
 * Storyboard textures are uploaded with straight (non-premultiplied) alpha
 * ([org.andengine.opengl.texture.TextureOptions.BILINEAR] has `mPreMultiplyAlpha = false`), so
 * vertex colors are straight as well and normal blending uses `(SRC_ALPHA,
 * ONE_MINUS_SRC_ALPHA)` while additive blending uses `(SRC_ALPHA, ONE)`, matching osu!stable.
 * Getting this wrong burns glow textures (colored white with the shape in the alpha channel) into
 * solid rectangles.
 *
 * Quad corners are in component space; the batcher transforms them by the model-view matrix
 * captured in [begin] and draws them with the projection set there. Colors are packed to bytes
 * and therefore clamped to `[0, 1]`.
 */
class StoryboardBatch {
    private val modelView = FloatArray(16)

    // The transform from storyboard space into the current component space, applied on the CPU so
    // that all layers can share one batch regardless of the letterbox mapping.
    private var spaceOffsetX = 0f
    private var spaceOffsetY = 0f
    private var spaceScale = 1f

    /**
     * Begins a batch. Must be called from the draw thread before any quads are added: captures the
     * current model-view matrix and sets the projection of the [QuadBatcher].
     *
     * @param pGLState The current [GLState].
     */
    fun begin(pGLState: GLState) {
        System.arraycopy(pGLState.modelViewGLMatrix, 0, modelView, 0, 16)
        QuadBatcher.setProjection(pGLState.projectionGLMatrix)
        resetSpace()
    }

    /**
     * Sets the storyboard-space transform applied to subsequently added quads.
     *
     * @param offsetX The X offset in component space.
     * @param offsetY The Y offset in component space.
     * @param scale The scale from storyboard units to component units.
     */
    fun setSpace(offsetX: Float, offsetY: Float, scale: Float) {
        this.spaceOffsetX = offsetX
        this.spaceOffsetY = offsetY
        this.spaceScale = scale
    }

    /**
     * Resets the storyboard-space transform to identity.
     */
    fun resetSpace() = setSpace(0f, 0f, 1f)

    /**
     * Adds the quad of an evaluated [PlayableSprite] to the batch.
     *
     * @param sprite The sprite to draw.
     * @param region The texture region of the sprite's current frame.
     * @param alphaMultiplier An additional alpha multiplier (the component's draw alpha).
     * @param colorMultiplier An additional color multiplier, used to dim the sprite.
     */
    @JvmOverloads
    fun draw(sprite: PlayableSprite, region: TextureRegion, alphaMultiplier: Float, colorMultiplier: Float = 1f) {
        val width = region.width.toFloat()
        val height = region.height.toFloat()
        val origin = sprite.element.origin

        // Flips and negative vector scales mirror the sprite in place, keeping the geometry
        // anchored at the original origin. A flip combined with a negative scale on the same axis
        // cancels out.
        val scaleX = abs(sprite.scaleX)
        val scaleY = abs(sprite.scaleY)
        val mirrorX = sprite.flipHorizontal != (sprite.scaleX < 0f)
        val mirrorY = sprite.flipVertical != (sprite.scaleY < 0f)

        val l = -width * origin.factorX * scaleX
        val t = -height * origin.factorY * scaleY
        val r = l + width * scaleX
        val b = t + height * scaleY

        val u0 = if (mirrorX) region.u2 else region.u
        val v0 = if (mirrorY) region.v2 else region.v
        val u1 = if (mirrorX) region.u else region.u2
        val v1 = if (mirrorY) region.v else region.v2

        val x = spaceOffsetX + sprite.x * spaceScale
        val y = spaceOffsetY + sprite.y * spaceScale
        val scale = spaceScale

        // Straight (non-premultiplied) vertex color; the alpha is applied by the blend function.
        val rgba = QuadBatcher.packColor(
            sprite.red * colorMultiplier,
            sprite.green * colorMultiplier,
            sprite.blue * colorMultiplier,
            sprite.alpha * alphaMultiplier
        )
        val texture = BatchTexture.of(region.texture)
        val blend = if (sprite.additiveBlend) ADDITIVE_BLEND else ALPHA_BLEND

        // Corners top-left, top-right, bottom-right, bottom-left.
        if (sprite.rotation == 0f) {
            val left = l * scale + x
            val top = t * scale + y
            val right = r * scale + x
            val bottom = b * scale + y
            QuadBatcher.addQuad(
                texture, blend, modelView,
                left, top, right, top, right, bottom, left, bottom,
                u0, v0, u1, v1, rgba
            )
        } else {
            val s = sin(sprite.rotation)
            val c = cos(sprite.rotation)
            QuadBatcher.addQuad(
                texture, blend, modelView,
                (l * c - t * s) * scale + x, (l * s + t * c) * scale + y,
                (r * c - t * s) * scale + x, (r * s + t * c) * scale + y,
                (r * c - b * s) * scale + x, (r * s + b * c) * scale + y,
                (l * c - b * s) * scale + x, (l * s + b * c) * scale + y,
                u0, v0, u1, v1, rgba
            )
        }
    }

    /**
     * Adds an axis-aligned quad to the batch, used for the background and dim overlays. The
     * coordinates are in component space (unaffected by [setSpace]).
     *
     * @param region The texture region to draw.
     * @param left The left of the quad.
     * @param top The top of the quad.
     * @param right The right of the quad.
     * @param bottom The bottom of the quad.
     * @param red The red component.
     * @param green The green component.
     * @param blue The blue component.
     * @param alpha The alpha component.
     */
    fun drawQuad(
        region: TextureRegion,
        left: Float, top: Float, right: Float, bottom: Float,
        red: Float, green: Float, blue: Float, alpha: Float
    ) {
        QuadBatcher.addQuad(
            BatchTexture.of(region.texture), ALPHA_BLEND, modelView,
            left, top, right, top, right, bottom, left, bottom,
            region.u, region.v, region.u2, region.v2,
            QuadBatcher.packColor(red, green, blue, alpha)
        )
    }

    /**
     * Ends the batch. The quads stay pending in the [QuadBatcher], so that following quads of the
     * same texture and blend mode can share their draw call; the batcher draws them before anyone
     * else changes GL state.
     */
    fun end() = Unit

    /**
     * Draws the quads pending in the [QuadBatcher] now.
     */
    fun flush() = QuadBatcher.flush()

    companion object {
        private val ALPHA_BLEND: BlendMode =
            GlesPipeline.blendModeOf(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)!!
        private val ADDITIVE_BLEND: BlendMode =
            GlesPipeline.blendModeOf(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)!!
    }
}
