package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.OutlineBox.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

open class OutlineBox : BufferedEntity<OutlineBoxVertexBuffer>(OutlineBoxVertexBuffer()) {

    /**
     * The width of the line.
     */
    var lineWidth = 1f


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        gl.glLineWidth(lineWidth)
    }


    class OutlineBoxVertexBuffer : VertexBuffer(GL_LINE_LOOP, BOX_VERTICES, VERTEX_2D, GL_STATIC_DRAW) {

        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {
            putVertex(0, 0f, 0f)
            putVertex(1, entity.width, 0f)
            putVertex(2, entity.width, entity.height)
            putVertex(3, 0f, entity.height)
        }

        companion object {
            const val BOX_VERTICES = 4
        }
    }
}