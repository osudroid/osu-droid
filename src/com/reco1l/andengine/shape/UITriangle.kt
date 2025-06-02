package com.reco1l.andengine.shape

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.UITriangle.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*

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
        invalidateBuffer(BufferInvalidationFlag.Data)
    }

    override fun onCreateBuffer(gl: GL10): TriangleVBO {
        return TriangleVBO()
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.lineWidth(gl, lineWidth)
    }


    inner class TriangleVBO : VertexBuffer(GL_TRIANGLES, 3, VERTEX_2D, GL_STATIC_DRAW) {

        override fun update(gl: GL10, entity: UIBufferedComponent<*>, vararg data: Any) {
            addTriangle(
                index = 0,
                centerX = entity.width / 2f,
                centerY = entity.height / 2f,
                width = entity.innerWidth,
                height = entity.innerHeight
            )
        }

        override fun draw(gl: GL10, entity: UIBufferedComponent<*>) {
            gl.glDrawArrays(if (paintStyle == PaintStyle.Fill) GL_TRIANGLES else GL_LINE_LOOP, 0, vertexCount)
        }

    }
}


