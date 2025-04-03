package com.reco1l.andengine

import org.anddev.andengine.collision.ShapeCollisionChecker
import org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_X
import org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_Y

/**
 * Checker for extended entity collisions.
 */
object EntityCollisionChecker {


    private val vertices = FloatArray(8)


    /**
     * Checks if the given entity contains the given point.
     */
    fun contains(entity: ExtendedEntity, x: Float, y: Float, fromScene: Boolean): Boolean {

        val left = 0f
        val top = 0f
        val right = entity.width
        val bottom = entity.height

        vertices[0 + VERTEX_INDEX_X] = left
        vertices[0 + VERTEX_INDEX_Y] = top

        vertices[2 + VERTEX_INDEX_X] = right
        vertices[2 + VERTEX_INDEX_Y] = top

        vertices[4 + VERTEX_INDEX_X] = right
        vertices[4 + VERTEX_INDEX_Y] = bottom

        vertices[6 + VERTEX_INDEX_X] = left
        vertices[6 + VERTEX_INDEX_Y] = bottom

        if (fromScene) {
            entity.localToSceneTransformation.transform(vertices)
        } else {
            entity.localToParentTransformation.transform(vertices)
        }

        return ShapeCollisionChecker.checkContains(vertices, vertices.size, x, y)
    }
}