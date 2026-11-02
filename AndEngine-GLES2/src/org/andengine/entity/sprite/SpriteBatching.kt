package org.andengine.entity.sprite

import org.andengine.entity.shape.Shape
import org.andengine.entity.sprite.vbo.HighPerformanceSpriteVertexBufferObject
import org.andengine.graphics.BlendFactor
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesPipeline
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.util.GLState

/**
 * Decides whether an engine [Sprite] is drawn through the [QuadBatcher] and hands it over: the
 * corners, texture coordinates and color are taken from the sprite's own vertex data, the blend
 * mode from its blend function. GL thread only.
 */
object SpriteBatching {

    private val factors = BlendFactor.values()

    /** Blend modes by source and destination factor, created on first use. */
    private val blendModes = Array(factors.size) { arrayOfNulls<BlendMode>(factors.size) }

    /** Per sprite class: no override of the draw methods below [Sprite] / [TiledSprite]. */
    private val plainClasses = HashMap<Class<*>, Boolean>()

    private val x = FloatArray(4)
    private val y = FloatArray(4)
    private val u = FloatArray(4)
    private val v = FloatArray(4)

    /**
     * The blend mode of [shape], with the same factors for color and alpha as the sprite's own
     * blend function. A sprite without blending replaces the destination, which is
     * [BlendMode.Opaque]. Null for factors that have no [BlendFactor].
     */
    private fun blendOf(shape: Shape): BlendMode? {
        if (!shape.isBlendingEnabled) return BlendMode.Opaque
        val src = GlesPipeline.blendFactorOf(shape.blendFunctionSource) ?: return null
        val dst = GlesPipeline.blendFactorOf(shape.blendFunctionDestination) ?: return null
        val row = blendModes[src.ordinal]
        var mode = row[dst.ordinal]
        if (mode == null) {
            mode = BlendMode(src, dst, src, dst)
            row[dst.ordinal] = mode
        }
        return mode
    }

    // Matches the overridden methods by name, so it relies on these names surviving shrinking:
    // keep draw/preDraw/postDraw of Sprite subclasses if obfuscation is ever enabled.
    private fun hasPlainDrawPath(type: Class<*>): Boolean {
        val cached = plainClasses[type]
        if (cached != null) return cached
        var plain = true
        var c: Class<*>? = type
        while (c != null && c != Sprite::class.java && c != TiledSprite::class.java) {
            for (m in c.declaredMethods) {
                val n = m.name
                if (n == "draw" || n == "preDraw" || n == "postDraw") plain = false
            }
            c = c.superclass
        }
        plainClasses[type] = plain
        return plain
    }

    /**
     * True if [sprite] can be drawn through the batch: the default shader, the engine's own vertex
     * buffer, a blend function the batch knows, and no subclass that draws itself.
     */
    @JvmStatic
    fun canBatch(sprite: Sprite): Boolean =
        sprite.shaderProgram === PositionColorTextureCoordinatesShaderProgram.getInstance() &&
            sprite.vertexBufferObject is HighPerformanceSpriteVertexBufferObject &&
            blendOf(sprite) != null &&
            hasPlainDrawPath(sprite.javaClass)

    /**
     * Adds the quad whose corners are the vertices [c0]..[c3] (in order around the quad) of the
     * sprite's vertex data, starting at float [offset]. The vertices are transformed by the
     * current model-view matrix of [glState].
     */
    @JvmStatic
    fun add(sprite: Sprite, glState: GLState, offset: Int, c0: Int, c1: Int, c2: Int, c3: Int) {
        val blend = blendOf(sprite) ?: return
        val data = (sprite.vertexBufferObject as HighPerformanceSpriteVertexBufferObject).bufferData
        val m = glState.modelViewGLMatrix
        val a = m[0]
        val b = m[1]
        val c = m[4]
        val d = m[5]
        val tx = m[12]
        val ty = m[13]
        for (k in 0 until 4) {
            val corner = when (k) {
                0 -> c0
                1 -> c1
                2 -> c2
                else -> c3
            }
            val i = offset + corner * Sprite.VERTEX_SIZE
            val lx = data[i + Sprite.VERTEX_INDEX_X]
            val ly = data[i + Sprite.VERTEX_INDEX_Y]
            x[k] = a * lx + c * ly + tx
            y[k] = b * lx + d * ly + ty
            u[k] = data[i + Sprite.TEXTURECOORDINATES_INDEX_U]
            v[k] = data[i + Sprite.TEXTURECOORDINATES_INDEX_V]
        }
        val rgba = java.lang.Float.floatToRawIntBits(data[offset + c0 * Sprite.VERTEX_SIZE + Sprite.COLOR_INDEX])
        QuadBatcher.setProjection(glState.projectionGLMatrix)
        QuadBatcher.addQuad(BatchTexture.of(sprite.textureRegion.texture), blend, x, y, u, v, rgba)
    }
}
