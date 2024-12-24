package com.reco1l.andengine

import org.anddev.andengine.collision.ShapeCollisionChecker
import org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_X
import org.anddev.andengine.util.constants.Constants.VERTEX_INDEX_Y

object EntityCollision {


    private val vertices = FloatArray(8)


    fun contains(entity: ExtendedEntity, x: Float, y: Float): Boolean {

        val left = 0f
        val top = 0f
        val right = entity.drawWidth
        val bottom = entity.drawHeight

        vertices[0 + VERTEX_INDEX_X] = left
        vertices[0 + VERTEX_INDEX_Y] = top

        vertices[2 + VERTEX_INDEX_X] = right
        vertices[2 + VERTEX_INDEX_Y] = top

        vertices[4 + VERTEX_INDEX_X] = right
        vertices[4 + VERTEX_INDEX_Y] = bottom

        vertices[6 + VERTEX_INDEX_X] = left
        vertices[6 + VERTEX_INDEX_Y] = bottom

        entity.getLocalToSceneTransformation().transform(vertices)

        return ShapeCollisionChecker.checkContains(vertices, vertices.size, x, y)
    }

}