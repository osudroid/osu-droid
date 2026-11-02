package org.andengine.entity.primitive

import org.andengine.entity.primitive.vbo.HighPerformanceRectangleVertexBufferObject
import org.andengine.entity.sprite.SpriteBatching
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.opengl.shader.PositionColorShaderProgram
import org.andengine.opengl.util.GLState

/**
 * Decides whether an engine [Rectangle] is drawn through the [QuadBatcher] and hands it over: the
 * box, color and blend function are taken from the rectangle itself. GL thread only.
 */
object RectangleBatching {

    /** Per rectangle class: no override of the draw methods below [Rectangle]. */
    private val plainClasses = HashMap<Class<*>, Boolean>()

    // Matches the overridden methods by name, so it relies on these names surviving shrinking:
    // keep draw/preDraw/postDraw of Rectangle subclasses if obfuscation is ever enabled.
    private fun hasPlainDrawPath(type: Class<*>): Boolean {
        val cached = plainClasses[type]
        if (cached != null) return cached
        var plain = true
        var c: Class<*>? = type
        while (c != null && c != Rectangle::class.java) {
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
     * True if [rect] can be drawn through the batch: the default shader, the engine's own vertex
     * buffer, a blend function the batch knows, and no subclass that draws itself.
     */
    @JvmStatic
    fun canBatch(rect: Rectangle): Boolean =
        rect.shaderProgram === PositionColorShaderProgram.getInstance() &&
            rect.vertexBufferObject is HighPerformanceRectangleVertexBufferObject &&
            SpriteBatching.blendOf(rect) != null &&
            hasPlainDrawPath(rect.javaClass)

    /**
     * Adds the rectangle's box in the current model-view matrix of [glState], colored with the
     * entity's own color and alpha (straight alpha, as in the vertex buffer). Alpha 0 adds nothing.
     */
    @JvmStatic
    fun add(rect: Rectangle, glState: GLState) {
        val blend = SpriteBatching.blendOf(rect) ?: return
        val rgba = rect.color.abgrPackedInt
        QuadBatcher.setProjection(glState.projectionGLMatrix)
        ShapeBatching.rect(glState.modelViewGLMatrix, 0f, 0f, rect.width, rect.height, rgba, blend)
    }
}
