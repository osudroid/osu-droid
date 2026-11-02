package org.andengine.entity.primitive

import org.andengine.entity.sprite.SpriteBatching
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.opengl.util.GLState

/**
 * Hands an engine [Rectangle] to the [QuadBatcher]: the box, color and blend function are taken
 * from the rectangle itself when it is drawn. GL thread only.
 */
object RectangleBatching {

    /**
     * Adds the rectangle's box in the current model-view matrix of [glState], colored with the
     * entity's own color and alpha (straight alpha). Alpha 0 adds nothing.
     */
    @JvmStatic
    fun add(rect: Rectangle, glState: GLState) {
        val blend = SpriteBatching.batchBlendOf(rect)
        val rgba = rect.color.abgrPackedInt
        QuadBatcher.setProjection(glState.projectionGLMatrix)
        ShapeBatching.rect(glState.modelViewGLMatrix, 0f, 0f, rect.width, rect.height, rgba, blend)
    }
}
