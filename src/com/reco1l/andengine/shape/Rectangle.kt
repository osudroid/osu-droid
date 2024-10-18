package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.vertex.RectangleVertexBuffer
import javax.microedition.khronos.opengles.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
open class Rectangle : ExtendedEntity(vertexBuffer = RectangleVertexBuffer(GL11.GL_STATIC_DRAW, true)) {


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as RectangleVertexBuffer).update(width, height)
    }

    override fun drawVertices(graphics: GL10, camera: Camera) {
        graphics.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4)
    }

}