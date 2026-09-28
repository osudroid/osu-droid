package com.osudroid.storyboard.renderer

import android.opengl.GLES32
import com.osudroid.storyboard.playback.PlayableSprite
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState

/**
 * A quad batch for storyboard rendering.
 *
 * Quads are accumulated into a client-side vertex array with the layout
 * `[x, y, u, v, r, g, b, a]` per vertex and flushed with a single indexed draw call whenever the
 * texture or blend mode changes, or the batch is full.
 *
 * Storyboard textures are uploaded with straight (non-premultiplied) alpha
 * ([org.andengine.opengl.texture.TextureOptions.BILINEAR] has `mPreMultiplyAlpha = false`), so
 * vertex colors are straight as well and normal blending uses `(SRC_ALPHA,
 * ONE_MINUS_SRC_ALPHA)` while additive blending uses `(SRC_ALPHA, ONE)` - matching osu!stable
 * and the previous renderer, which selected these factors based on the texture's premultiply
 * flag. Getting this wrong burns glow textures (colored white with the shape in the alpha
 * channel) into solid rectangles.
 *
 * The GL state handling is ported from the previous storyboard renderer (`TextureQuadBatch`),
 * which embeds hard-won fixes for keeping AndEngine's [GLState] caches authoritative: shader,
 * texture and array-buffer binds are routed through [GLState], and the vertex attribute arrays
 * are restored to the state AndEngine expects after the batch finishes.
 */
class StoryboardBatch {
    private val vertices = FloatArray(MAX_QUADS * FLOATS_PER_QUAD)
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(MAX_QUADS * FLOATS_PER_QUAD * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private val indexBuffer: ShortBuffer = ByteBuffer
        .allocateDirect(MAX_QUADS * 6 * Short.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .also {
            var vertex = 0

            for (i in 0 until MAX_QUADS) {
                val v0 = vertex.toShort()
                val v1 = (vertex + 1).toShort()
                val v2 = (vertex + 2).toShort()
                val v3 = (vertex + 3).toShort()

                it.put(v0).put(v1).put(v2)
                it.put(v1).put(v3).put(v2)
                vertex += 4
            }

            it.position(0)
        }

    private var offset = 0
    private var texture: ITexture? = null
    private var additiveBlend = false
    private var glState: GLState? = null

    // The transform from storyboard space into the current component space, applied on the CPU so
    // that all layers can share one batch regardless of the letterbox mapping.
    private var spaceOffsetX = 0f
    private var spaceOffsetY = 0f
    private var spaceScale = 1f

    /**
     * Begins a batch. Must be called from the draw thread before any quads are added.
     *
     * @param pGLState The current [GLState].
     */
    fun begin(pGLState: GLState) {
        glState = pGLState
        offset = 0
        texture = null
        additiveBlend = false
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
        val alpha = sprite.alpha * alphaMultiplier

        prepare(region.texture, sprite.additiveBlend)

        val width = region.width.toFloat()
        val height = region.height.toFloat()
        val origin = sprite.element.origin

        // Flips and negative vector scales mirror the sprite in place, keeping the geometry
        // anchored at the original origin (matching osu!lazer's origin adjustment). A flip
        // combined with a negative scale on the same axis cancels out.
        val scaleX = abs(sprite.scaleX)
        val scaleY = abs(sprite.scaleY)
        val mirrorX = sprite.flipHorizontal != (sprite.scaleX < 0f)
        val mirrorY = sprite.flipVertical != (sprite.scaleY < 0f)

        var l = -width * origin.factorX * scaleX
        var t = -height * origin.factorY * scaleY
        var r = l + width * scaleX
        var b = t + height * scaleY

        var u1 = region.u
        var u2 = region.u2
        var v1 = region.v
        var v2 = region.v2

        if (mirrorX) {
            val tmp = u1; u1 = u2; u2 = tmp
        }

        if (mirrorY) {
            val tmp = v1; v1 = v2; v2 = tmp
        }

        // Straight (non-premultiplied) vertex color; the alpha is applied by the blend function.
        val cr = sprite.red * colorMultiplier
        val cg = sprite.green * colorMultiplier
        val cb = sprite.blue * colorMultiplier

        val x = spaceOffsetX + sprite.x * spaceScale
        val y = spaceOffsetY + sprite.y * spaceScale
        val scale = spaceScale

        var i = offset

        if (sprite.rotation == 0f) {
            l = l * scale + x
            t = t * scale + y
            r = r * scale + x
            b = b * scale + y

            i = putVertex(i, l, t, u1, v1, cr, cg, cb, alpha)
            i = putVertex(i, r, t, u2, v1, cr, cg, cb, alpha)
            i = putVertex(i, l, b, u1, v2, cr, cg, cb, alpha)
            i = putVertex(i, r, b, u2, v2, cr, cg, cb, alpha)
        } else {
            val s = sin(sprite.rotation)
            val c = cos(sprite.rotation)

            i = putVertex(i, (l * c - t * s) * scale + x, (l * s + t * c) * scale + y, u1, v1, cr, cg, cb, alpha)
            i = putVertex(i, (r * c - t * s) * scale + x, (r * s + t * c) * scale + y, u2, v1, cr, cg, cb, alpha)
            i = putVertex(i, (l * c - b * s) * scale + x, (l * s + b * c) * scale + y, u1, v2, cr, cg, cb, alpha)
            i = putVertex(i, (r * c - b * s) * scale + x, (r * s + b * c) * scale + y, u2, v2, cr, cg, cb, alpha)
        }

        offset = i

        if (offset == vertices.size) {
            flush()
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
        prepare(region.texture, false)

        var i = offset
        i = putVertex(i, left, top, region.u, region.v, red, green, blue, alpha)
        i = putVertex(i, right, top, region.u2, region.v, red, green, blue, alpha)
        i = putVertex(i, left, bottom, region.u, region.v2, red, green, blue, alpha)
        i = putVertex(i, right, bottom, region.u2, region.v2, red, green, blue, alpha)
        offset = i

        if (offset == vertices.size) {
            flush()
        }
    }

    /**
     * Flushes any pending quads and restores the GL state AndEngine expects.
     */
    fun end() {
        flush()

        // Release the shader binding through GLState so the cache reflects program = 0.
        glState?.useProgram(0)
        glState = null
    }

    private fun prepare(texture: ITexture, additive: Boolean) {
        if (texture !== this.texture || additive != this.additiveBlend) {
            flush()
            this.texture = texture
            this.additiveBlend = additive
        }
    }

    private fun putVertex(
        index: Int,
        x: Float, y: Float, u: Float, v: Float,
        r: Float, g: Float, b: Float, a: Float
    ): Int {
        val ary = vertices
        var i = index
        ary[i++] = x
        ary[i++] = y
        ary[i++] = u
        ary[i++] = v
        ary[i++] = r
        ary[i++] = g
        ary[i++] = b
        ary[i++] = a
        return i
    }

    /**
     * Issues the pending quads with a single indexed draw call. Automatically called on texture
     * or blend changes, on capacity, and by [end].
     */
    fun flush() {
        val glState = glState ?: return
        val texture = texture

        if (offset == 0 || texture == null) {
            return
        }

        val shader = StoryboardShaderProgram.getInstance()
        shader.bind(glState)

        glState.enableBlend()
        glState.blendFunction(
            GLES32.GL_SRC_ALPHA,
            if (additiveBlend) GLES32.GL_ONE else GLES32.GL_ONE_MINUS_SRC_ALPHA
        )

        // Route texture binds through GLState so its per-unit texture cache stays authoritative
        // (see Issue 28 of the GLES2 migration review).
        glState.activeTexture(GLES32.GL_TEXTURE0)
        glState.bindTexture(texture.hardwareTextureID)

        // Unbind any VBO so glVertexAttribPointer reads from the client-side buffer.
        glState.bindArrayBuffer(0)
        GLES32.glBindBuffer(GLES32.GL_ELEMENT_ARRAY_BUFFER, 0)

        vertexBuffer.position(0)
        vertexBuffer.put(vertices, 0, offset)

        vertexBuffer.position(0)
        GLES32.glEnableVertexAttribArray(StoryboardShaderProgram.ATTRIBUTE_POSITION_LOCATION)
        GLES32.glVertexAttribPointer(
            StoryboardShaderProgram.ATTRIBUTE_POSITION_LOCATION, 2,
            GLES32.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer
        )

        vertexBuffer.position(2)
        GLES32.glEnableVertexAttribArray(StoryboardShaderProgram.ATTRIBUTE_TEXCOORD_LOCATION)
        GLES32.glVertexAttribPointer(
            StoryboardShaderProgram.ATTRIBUTE_TEXCOORD_LOCATION, 2,
            GLES32.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer
        )

        vertexBuffer.position(4)
        GLES32.glEnableVertexAttribArray(StoryboardShaderProgram.ATTRIBUTE_COLOR_LOCATION)
        GLES32.glVertexAttribPointer(
            StoryboardShaderProgram.ATTRIBUTE_COLOR_LOCATION, 4,
            GLES32.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer
        )

        indexBuffer.position(0)
        GLES32.glDrawElements(
            GLES32.GL_TRIANGLES,
            offset / FLOATS_PER_QUAD * 6,
            GLES32.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        // Restore the vertex attribute arrays AndEngine expects to be enabled: slot 0 = position,
        // slot 1 = color. The storyboard-specific color slot (2) must be disabled.
        GLES32.glDisableVertexAttribArray(StoryboardShaderProgram.ATTRIBUTE_TEXCOORD_LOCATION)
        GLES32.glDisableVertexAttribArray(StoryboardShaderProgram.ATTRIBUTE_COLOR_LOCATION)
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_POSITION_LOCATION)
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        offset = 0
    }

    companion object {
        private const val MAX_QUADS = 1024
        private const val FLOATS_PER_VERTEX = 8
        private const val FLOATS_PER_QUAD = 4 * FLOATS_PER_VERTEX
        private const val VERTEX_STRIDE_BYTES = FLOATS_PER_VERTEX * Float.SIZE_BYTES
    }
}
