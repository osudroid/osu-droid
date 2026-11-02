package org.andengine.entity.sprite

import android.util.Log
import org.andengine.entity.shape.Shape
import org.andengine.graphics.BlendFactor
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesPipeline

/**
 * Hands an engine [Sprite] to the [QuadBatcher]: the corners and texture coordinates are taken
 * from the sprite's own vertex data, the color from the sprite when it is drawn, the blend mode
 * from its blend function. GL thread only.
 */
object SpriteBatching {

    private const val TAG = "SpriteBatching"

    private val factors = BlendFactor.values()

    /** Blend modes by source and destination factor, created on first use. */
    private val blendModes = Array(factors.size) { arrayOfNulls<BlendMode>(factors.size) }

    private var unknownBlendLogged = false

    private val x = FloatArray(4)
    private val y = FloatArray(4)
    private val u = FloatArray(4)
    private val v = FloatArray(4)

    /**
     * The blend mode of [shape], with the same factors for color and alpha as the shape's own
     * blend function. A shape without blending replaces the destination, which is
     * [BlendMode.Opaque]. A blend function the batch does not know falls back to
     * [BlendMode.Alpha], logged once.
     */
    @JvmStatic
    fun batchBlendOf(shape: Shape): BlendMode {
        if (!shape.isBlendingEnabled) return BlendMode.Opaque
        val src = GlesPipeline.blendFactorOf(shape.blendFunctionSource)
        val dst = GlesPipeline.blendFactorOf(shape.blendFunctionDestination)
        if (src == null || dst == null) {
            if (!unknownBlendLogged) {
                unknownBlendLogged = true
                Log.w(TAG, "Blend function (${shape.blendFunctionSource}, ${shape.blendFunctionDestination}) is not supported by the batch, using alpha blending")
            }
            return BlendMode.Alpha
        }
        val row = blendModes[src.ordinal]
        var mode = row[dst.ordinal]
        if (mode == null) {
            mode = BlendMode(src, dst, src, dst)
            row[dst.ordinal] = mode
        }
        return mode
    }

    /**
     * Adds the sprite's quad, transformed by the current model-view matrix of [drawContext], colored
     * with the sprite's own color and alpha.
     */
    @JvmStatic
    fun add(sprite: Sprite, drawContext: DrawContext) {
        val blend = batchBlendOf(sprite)
        val corners = sprite.corners
        val uvs = sprite.textureCoordinates
        val m = drawContext.modelViewGLMatrix
        val a = m[0]
        val b = m[1]
        val c = m[4]
        val d = m[5]
        val tx = m[12]
        val ty = m[13]
        for (k in 0 until Sprite.VERTICES_PER_SPRITE) {
            val lx = corners[2 * k]
            val ly = corners[2 * k + 1]
            x[k] = a * lx + c * ly + tx
            y[k] = b * lx + d * ly + ty
            u[k] = uvs[2 * k]
            v[k] = uvs[2 * k + 1]
        }
        val rgba = sprite.color.abgrPackedInt
        QuadBatcher.setProjection(drawContext.projectionGLMatrix)
        QuadBatcher.addQuad(BatchTexture.of(sprite.textureRegion.texture), blend, x, y, u, v, rgba)
    }
}
