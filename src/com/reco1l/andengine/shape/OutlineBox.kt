package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*

open class OutlineBox : ExtendedEntity(vertexBuffer = OutlineBoxVertexBuffer()) {

    /**
     * The width of the line.
     */
    var lineWidth = 1f


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)

        pGL.glLineWidth(lineWidth)
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as OutlineBoxVertexBuffer).update(drawWidth, drawHeight)
    }

    override fun drawVertices(gl: GL10, camera: Camera) {
        gl.glDrawArrays(GL_LINE_LOOP, 0, 4)
    }


    class OutlineBoxVertexBuffer : VertexBuffer(4 * 2, GL11.GL_STATIC_DRAW, false) {

        fun update(width: Float, height: Float) {
            floatBuffer.apply {
                put(0, 0f)
                put(1, 0f)

                put(2, width)
                put(3, 0f)

                put(4, width)
                put(5, height)

                put(6, 0f)
                put(7, height)
            }

            setHardwareBufferNeedsUpdate()
        }

    }

}