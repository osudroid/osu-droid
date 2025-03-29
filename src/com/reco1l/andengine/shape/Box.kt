package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.Box.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
open class Box : BufferedEntity<BoxVertexBuffer>(BoxVertexBuffer()) {

    class BoxVertexBuffer : VertexBuffer(GL_TRIANGLE_STRIP, BOX_VERTICES, VERTEX_2D, GL_STATIC_DRAW) {

        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {
            putVertex(0, 0f, 0f)
            putVertex(1, 0f, entity.drawHeight)
            putVertex(2, entity.drawWidth, 0f)
            putVertex(3, entity.drawWidth, entity.drawHeight)
        }

        companion object {
            const val BOX_VERTICES = 4
        }
    }

}


