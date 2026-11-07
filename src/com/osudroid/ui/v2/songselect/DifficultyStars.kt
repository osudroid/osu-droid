package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.ui.ISkinnable
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.ResourceManager
import kotlin.math.min

/**
 * A row of skinnable `star` sprites visualizing a star rating, like the legacy song menu did:
 * one full star per whole point and a scaled-down star for the fractional part.
 *
 * All stars are drawn from a single buffer with one draw call.
 */
class DifficultyStars : UIBufferedComponent<DifficultyStars.StarsVertexBuffer>(), ISkinnable {

    /**
     * The star rating to display. Values above [MAX_STARS] are capped.
     */
    var rating = 0f
        set(value) {
            if (field != value) {
                field = value
                onStarsChanged()
            }
        }

    /**
     * The size of a single star in scene units.
     */
    var starSize = 16f
        set(value) {
            if (field != value) {
                field = value
                onStarsChanged()
            }
        }

    /**
     * The space between two stars.
     */
    var spacing = 2f
        set(value) {
            if (field != value) {
                field = value
                onStarsChanged()
            }
        }


    private var texture: TextureRegion? = null

    private val uvBuffer = StarsUVBuffer()

    // Positions and texture coordinates interleaved as x, y, u, v for the mesh renderer.
    private val texturedScratch = FloatArray(VERTICES_PER_STAR * MAX_STARS * 4)

    /**
     * Number of stars drawn, including the fractional one.
     */
    private var visibleStars = 0


    init {
        width = MatchContent
        height = MatchContent
        onSkinChanged()
    }


    override fun onSkinChanged() {
        texture = ResourceManager.getInstance().getTexture("star")

        val texture = texture
        if (texture != null) {
            blendMode = if (texture.texture.textureOptions.mPreMultiplyAlpha) BlendMode.Premultiplied else DEFAULT_BLEND
        }
        requestBufferUpdate()
    }

    private fun onStarsChanged() {
        val capped = min(rating, MAX_STARS.toFloat())
        val fullStars = capped.toInt()
        visibleStars = if (capped - fullStars > 0f) fullStars + 1 else fullStars

        contentWidth = if (visibleStars > 0) visibleStars * starSize + (visibleStars - 1) * spacing else 0f
        contentHeight = starSize
        requestBufferUpdate()
    }


    override fun onCreateBuffer(): StarsVertexBuffer {
        return buffer ?: StarsVertexBuffer()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
        uvBuffer.update(texture)
    }

    override fun onDrawBuffer(gl: DrawContext) {
        val texture = texture ?: return
        val positions = buffer?.data ?: return
        if (visibleStars == 0) {
            return
        }

        val vertexCount = VERTICES_PER_STAR * visibleStars
        val uvs = uvBuffer.data
        val vertices = texturedScratch
        for (i in 0 until vertexCount) {
            vertices[i * 4] = positions[i * 2]
            vertices[i * 4 + 1] = positions[i * 2 + 1]
            vertices[i * 4 + 2] = uvs[i * 2]
            vertices[i * 4 + 3] = uvs[i * 2 + 1]
        }

        MeshRenderer.drawTextured(
            PrimitiveType.Triangles, vertices, 0, vertexCount,
            gl.modelViewGLMatrix, gl.projectionGLMatrix,
            BatchTexture.of(texture.texture),
            QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha),
            resolvedBlendMode, depthState.test, depthState.write
        )
    }


    class StarsVertexBuffer : VertexBuffer(
        drawTopology = PrimitiveType.Triangles,
        vertexCount = VERTICES_PER_STAR * MAX_STARS,
        vertexSize = VERTEX_2D
    ) {
        fun update(entity: DifficultyStars) {
            val capped = min(entity.rating, MAX_STARS.toFloat())
            val size = entity.starSize
            var index = 0

            for (i in 0 until entity.visibleStars) {
                // The last star is scaled by the fractional part, centered vertically like the full stars.
                val scale = if (i < capped.toInt()) 1f else capped - capped.toInt()
                val starWidth = size * scale
                val x = i * (size + entity.spacing)
                val y = (size - starWidth) / 2f

                putVertex(index++, x, y)
                putVertex(index++, x, y + starWidth)
                putVertex(index++, x + starWidth, y + starWidth)
                putVertex(index++, x + starWidth, y + starWidth)
                putVertex(index++, x + starWidth, y)
                putVertex(index++, x, y)
            }
        }
    }

    class StarsUVBuffer : TextureCoordinatesBuffer(
        vertexCount = VERTICES_PER_STAR * MAX_STARS,
        vertexSize = VERTEX_2D
    ) {
        fun update(texture: TextureRegion?) {
            val u = texture?.u ?: 0f
            val v = texture?.v ?: 0f
            val u2 = texture?.u2 ?: 0f
            val v2 = texture?.v2 ?: 0f
            var index = 0

            // Every star uses the whole texture region, in the same vertex order as the positions.
            for (i in 0 until MAX_STARS) {
                putVertex(index++, u, v)
                putVertex(index++, u, v2)
                putVertex(index++, u2, v2)
                putVertex(index++, u2, v2)
                putVertex(index++, u2, v)
                putVertex(index++, u, v)
            }
        }
    }


    companion object {
        const val MAX_STARS = 10

        private const val VERTICES_PER_STAR = 6
    }

}
