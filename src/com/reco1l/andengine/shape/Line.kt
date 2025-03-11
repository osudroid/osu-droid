package com.reco1l.andengine.shape

import com.reco1l.andengine.*
import com.reco1l.framework.math.Vec2
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.util.GLHelper
import org.anddev.andengine.opengl.vertex.VertexBuffer
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*

/**
 * A rectangle shape based on [ExtendedEntity].
 */
open class Line : ExtendedEntity(vertexBuffer = LineVertexBuffer()) {

    /**
     * The width of the line.
     */
    var lineWidth = 1f

    var fromPoint = Vec2.Zero
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    var toPoint = Vec2.Zero
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.disableCulling(pGL)
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
        pGL.glLineWidth(lineWidth)
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as LineVertexBuffer).update(fromPoint, toPoint)
    }

    override fun drawVertices(gl: GL10, camera: Camera) {
        gl.glDrawArrays(GL_LINES, 0, 2)
    }


    class LineVertexBuffer : VertexBuffer(2 * 2, GL11.GL_STATIC_DRAW, true) {

        fun update(fromPoint: Vec2, toPoint: Vec2) {
            floatBuffer.apply {
                position(0)
                put(fromPoint.x)
                put(fromPoint.y)
                put(toPoint.x)
                put(toPoint.y)
                position(0)
            }
            setHardwareBufferNeedsUpdate()
        }

    }

}
