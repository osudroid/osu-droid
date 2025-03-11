package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*

/**
 * A triangle shape.
 */
open class Triangle : ExtendedEntity(vertexBuffer = TriangleVertexBuffer()) {


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as TriangleVertexBuffer).update(drawWidth, drawHeight)
    }

    override fun drawVertices(gl: GL10, camera: Camera) {
        gl.glDrawArrays(GL_TRIANGLE_FAN, 0, 3)
    }


    class TriangleVertexBuffer : VertexBuffer(3 * 2, GL11.GL_STATIC_DRAW, false) {

        fun update(width: Float, height: Float) {

            floatBuffer.put(0, width / 2f)
            floatBuffer.put(1, 0f)

            floatBuffer.put(2, 0f)
            floatBuffer.put(3, height)

            floatBuffer.put(4, width)
            floatBuffer.put(5, height)

            setHardwareBufferNeedsUpdate()
        }

    }

}
