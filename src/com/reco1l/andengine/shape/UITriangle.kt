package com.reco1l.andengine.shape

import android.opengl.GLES20
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UITriangle.*
import org.andengine.opengl.util.*

/**
 * A rectangle shape based on [UIComponent].
 */
open class UITriangle : UIBufferedComponent<TriangleVBO>() {

    /**
     * The style of painting for the triangle.
     */
    var paintStyle = PaintStyle.Fill

    /**
     * The line width of the triangle if the style is [PaintStyle.Outline].
     */
    var lineWidth = 1f


    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    override fun onCreateBuffer(): TriangleVBO {
        return TriangleVBO()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)
        pGLState.lineWidth(lineWidth)
    }


    class TriangleVBO : VertexBuffer(GL_TRIANGLES, 3, VERTEX_2D, GL_STATIC_DRAW) {

        fun update(entity: UITriangle) {
            addTriangle(
                index = 0,
                centerX = entity.width / 2f,
                centerY = entity.height / 2f,
                width = entity.innerWidth,
                height = entity.innerHeight
            )
        }

        override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
            entity as UITriangle
            GLES20.glDrawArrays(if (entity.paintStyle == PaintStyle.Fill) GLES20.GL_TRIANGLES else GLES20.GL_LINE_LOOP, 0, vertexCount)
        }

    }
}


