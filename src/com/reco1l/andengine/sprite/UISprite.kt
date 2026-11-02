package com.reco1l.andengine.sprite

import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.sprite.UISprite.*
import com.reco1l.andengine.sprite.ScaleType.*
import com.reco1l.framework.math.*
import org.andengine.graphics.DrawContext
import org.andengine.opengl.texture.region.*
import org.andengine.engine.camera.Camera
import kotlin.math.*

/**
 * Sprite that allows to change texture once created.
 */
@Suppress("LeakingThis")
open class UISprite(textureRegion: TextureRegion? = null) : UIBufferedComponent<SpriteVBO>() {

    override var contentWidth: Float
        get() = textureRegion?.width?.toFloat() ?: 0f
        set(_) = Unit

    override var contentHeight: Float
        get() = textureRegion?.height?.toFloat() ?: 0f
        set(_) = Unit


    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false
        set(value) {
            field = value
            requestBufferUpdate()
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value) {
            field = value
            requestBufferUpdate()
        }

    /**
     * The texture region of the sprite.
     */
    var textureRegion = textureRegion
        set(value) {
            if (field != value) {
                field = value
                onTextureRegionChanged()
                invalidate(InvalidationFlag.Content)
            }
        }

    // Where the region started inside its texture when it was assigned. Regions can live on a shared
    // atlas page, so [textureX]/[textureY] are offsets from there rather than absolute positions.
    private var regionOriginX = 0f
    private var regionOriginY = 0f

    /**
     * The X offset of the texture, relative to where the region starts.
     */
    var textureX = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(regionOriginX + value, regionOriginY + textureY)
            }
        }

    /**
     * The Y offset of the texture, relative to where the region starts.
     */
    var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(regionOriginX + textureX, regionOriginY + value)
            }
        }

    /**
     * The scale type of the sprite.
     */
    open var scaleType: ScaleType = Fit
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }

    /**
     * The alignment of the texture.
     *
     * If the scale type is [ScaleType.Stretch] it will not take effect.
     */
    var gravity: Vec2 = Anchor.Center
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }


    /**
     * The texture coordinates of this sprite, one pair per vertex of the position buffer.
     * Managed separately from the position buffer so it can be per-instance when position is shared.
     * Protected so subclasses with a program of their own (e.g. [UIVideoSprite]) can read them.
     */
    protected val uvBuffer = SpriteUVBuffer()

    /**
     * Whether this sprite may be drawn through the [QuadBatcher]. Subclasses that draw with a
     * program of their own (overriding [onDrawBuffer]) return false and always take the buffer
     * path. Even when true, a sprite without a texture, with depth testing or clearing, or with a
     * blend function the batch cannot express is drawn through the buffer path.
     */
    open val isBatchable: Boolean get() = true

    // Scratch for [computeQuad], shared by both draw paths and the buffers.
    private val quad = SpriteQuad()


    init {
        width = MatchContent
        height = MatchContent

        onTextureRegionChanged()
    }



    open fun onTextureRegionChanged() {
        val textureRegion = textureRegion ?: return
        regionOriginX = textureRegion.textureX
        regionOriginY = textureRegion.textureY
        if (textureX != 0 || textureY != 0) {
            textureRegion.setTexturePosition(regionOriginX + textureX, regionOriginY + textureY)
        }
        blendInfo = if (textureRegion.texture.textureOptions.mPreMultiplyAlpha) BlendInfo.PreMultiply else BlendInfo.Mixture
        uvBuffer.update(this)
        requestBufferUpdate()
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }


    override fun onCreateBuffer(): SpriteVBO {
        return buffer ?: SpriteVBO()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
        uvBuffer.update(this)
    }


    override fun doDraw(pGLState: DrawContext, pCamera: Camera) {
        // A texture that is not on the GPU yet (e.g. an atlas page still waiting for upload) would
        // bind the invalid id and sample whatever is still bound; skip the frame before touching state.
        val region = textureRegion
        val texture = region?.texture
        if (texture != null && !texture.isLoadedToHardware) {
            return
        }
        if (region != null && drawBatched(pGLState, region)) {
            return
        }
        super.doDraw(pGLState, pCamera)
    }

    /**
     * Adds this sprite to the [QuadBatcher] if it can be drawn there; false leaves it to the
     * buffer path. Same geometry, texture coordinates and color as the buffer path; the blend
     * function is the one the buffer path would set, for color and alpha alike.
     */
    private fun drawBatched(gl: DrawContext, region: TextureRegion): Boolean {
        if (!isBatchable || !canBatchShape()) {
            return false
        }
        val blend = batchBlendMode() ?: return false

        onApplyColor(gl)
        val q = quad
        computeQuad(q)

        QuadBatcher.setProjection(gl.projectionGLMatrix)
        QuadBatcher.add(
            BatchTexture.of(region.texture),
            blend,
            gl.modelViewGLMatrix,
            q.x0, q.y0, q.x1, q.y1,
            q.u0, q.v0, q.u1, q.v1,
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha)
        )
        return true
    }

    /**
     * Computes the quad the sprite covers in local coordinates and the texture coordinates at its
     * edges, from the size, [scaleType], [gravity] and flip flags. Both draw paths use this.
     */
    private fun computeQuad(out: SpriteQuad) {
        val textureWidth = contentWidth
        val textureHeight = contentHeight

        val quadWidth: Float
        val quadHeight: Float

        when (scaleType) {

            Crop -> {
                val scale = max(width / textureWidth, height / textureHeight)
                quadWidth = textureWidth * scale
                quadHeight = textureHeight * scale
            }

            Fit -> {
                val scale = min(width / textureWidth, height / textureHeight)
                quadWidth = textureWidth * scale
                quadHeight = textureHeight * scale
            }

            Stretch -> {
                quadWidth = width
                quadHeight = height
            }
        }

        val x = (width - quadWidth) * gravity.x
        val y = (height - quadHeight) * gravity.y

        out.x0 = x
        out.y0 = y
        out.x1 = x + quadWidth
        out.y1 = y + quadHeight

        val region = textureRegion
        val u = region?.u ?: 0f
        val v = region?.v ?: 0f
        val u2 = region?.u2 ?: 0f
        val v2 = region?.v2 ?: 0f

        out.u0 = if (flippedHorizontal) u2 else u
        out.u1 = if (flippedHorizontal) u else u2
        out.v0 = if (flippedVertical) v2 else v
        out.v1 = if (flippedVertical) v else v2
    }

    override fun onDrawBuffer(pGLState: DrawContext) {
        // Without a texture there is nothing to sample.
        val texture = textureRegion?.texture ?: return
        val positions = buffer?.data ?: return
        val uvs = uvBuffer.data

        // Interleave the buffers' positions and texture coordinates (same strip order) as x, y, u, v.
        val vertices = texturedScratch
        for (i in 0 until SPRITE_VERTICES) {
            vertices[i * 4] = positions[i * 2]
            vertices[i * 4 + 1] = positions[i * 2 + 1]
            vertices[i * 4 + 2] = uvs[i * 2]
            vertices[i * 4 + 3] = uvs[i * 2 + 1]
        }

        MeshRenderer.drawTextured(
            PrimitiveType.TriangleStrip, vertices, 0, SPRITE_VERTICES,
            pGLState.modelViewGLMatrix, pGLState.projectionGLMatrix,
            BatchTexture.of(texture),
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            meshBlendMode(), meshDepthTest(), depthInfo.mask
        )
    }


    class SpriteVBO : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = SPRITE_VERTICES,
        vertexSize = VERTEX_2D
    ) {
        fun update(entity: UISprite) {
            val q = entity.quad
            entity.computeQuad(q)
            addQuad(0, q.x0, q.y0, q.x1, q.y1)
        }
    }

    /**
     * UV coordinate buffer for the sprite (maps texture region to quad vertices).
     * Vertex order matches [SpriteVBO]: top-left, bottom-left, top-right, bottom-right (GL_TRIANGLE_STRIP).
     */
    class SpriteUVBuffer : TextureCoordinatesBuffer(
        vertexCount = SPRITE_VERTICES,
        vertexSize = VERTEX_2D
    ) {
        fun update(entity: UISprite) {
            val q = entity.quad
            entity.computeQuad(q)

            // Vertex order: top-left, bottom-left, top-right, bottom-right
            putVertex(0, q.u0, q.v0)
            putVertex(1, q.u0, q.v1)
            putVertex(2, q.u1, q.v0)
            putVertex(3, q.u1, q.v1)
        }
    }

    /**
     * The rectangle a sprite covers, ([x0], [y0]) to ([x1], [y1]) in local coordinates, and the
     * texture coordinates at its left/top ([u0], [v0]) and right/bottom ([u1], [v1]) edges.
     */
    class SpriteQuad {
        @JvmField var x0 = 0f
        @JvmField var y0 = 0f
        @JvmField var x1 = 0f
        @JvmField var y1 = 0f
        @JvmField var u0 = 0f
        @JvmField var v0 = 0f
        @JvmField var u1 = 0f
        @JvmField var v1 = 0f
    }

    private companion object {
        const val SPRITE_VERTICES = 4

        // x, y, u, v per vertex for the mesh renderer; GL thread only, shared by all sprites.
        val texturedScratch = FloatArray(SPRITE_VERTICES * 4)
    }

}

enum class ScaleType {

    /**
     * Scale the texture to fill the entire sprite cropping the excess.
     */
    Crop,

    /**
     * Scale the texture to fit the sprite without cropping.
     */
    Fit,

    /**
     * Scale the texture to fit the sprite without cropping.
     * The texture will be stretched to fill the entire sprite.
     */
    Stretch
}