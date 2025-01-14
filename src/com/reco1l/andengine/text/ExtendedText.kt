package com.reco1l.andengine.text

import com.reco1l.andengine.Axes
import com.reco1l.andengine.ExtendedEntity
import com.reco1l.toolkt.kotlin.fastForEachIndexed
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.*
import org.anddev.andengine.opengl.texture.buffer.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import org.anddev.andengine.opengl.vertex.TextVertexBuffer.VERTICES_PER_CHARACTER
import org.anddev.andengine.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.GL_STATIC_DRAW
import kotlin.math.*

/**
 * A text entity that can be displayed on the screen.
 */
open class ExtendedText : ExtendedEntity() {


    override var autoSizeAxes = Axes.Both


    /**
     * The text to be displayed
     */
    var text: String = ""
        set(value) {
            if (field != value) {
				field = value
                if (value.length > maximumSize) {
                    shouldRebuildVertexBuffer = true
                    shouldRebuildTextureBuffer = true
                }
                updateVertexBuffer()
			}
        }

    /**
     * The font to use for this text.
     * It must be already loaded and ready to use before setting it.
     */
    var font: Font? = null
        set(value) {
            if (field != value) {
				field = value
                shouldRebuildTextureBuffer = true
                updateVertexBuffer()
			}
        }

    /**
     * The horizontal alignment of the text.
     */
    var horizontalAlign = HorizontalAlign.LEFT
		set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    private var textureBuffer: TextTextureBuffer? = null

    private var shouldRebuildVertexBuffer = true

    private var shouldRebuildTextureBuffer = true

    private var maximumSize = 0

    private var currentSize = 0


    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.enableTextures(pGL)
        GLHelper.enableTexCoordArray(pGL)
    }

    override fun drawVertices(gl: GL10, pCamera: Camera?) {
        val vertexBuffer = vertexBuffer
        if (vertexBuffer != null) {
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, currentSize * VERTICES_PER_CHARACTER)
        }
    }

    override fun onApplyVertices(gl: GL10) {

        val font = font
        val textureBuffer = textureBuffer

        if (font != null && textureBuffer != null) {
            font.texture.bind(gl)
            GLHelper.texCoordPointer(gl, textureBuffer.floatBuffer)
        }

        super.onApplyVertices(gl)
    }

    override fun onUpdateVertexBuffer() {

        val text = text
        currentSize = text.length

        if (text.length > maximumSize) {
			shouldRebuildVertexBuffer = true
			shouldRebuildTextureBuffer = true
            maximumSize = text.length
        }

        if (shouldRebuildVertexBuffer) {
            shouldRebuildVertexBuffer = false

            setVertexBuffer(TextVertexBuffer(maximumSize, horizontalAlign, GL_STATIC_DRAW, true))
        }

        if (shouldRebuildTextureBuffer) {
            shouldRebuildTextureBuffer = false

            textureBuffer = TextTextureBuffer(2 * VERTICES_PER_CHARACTER * maximumSize, GL_STATIC_DRAW, true)
        }

        val lines = text.split('\n').toTypedArray()
        val linesWidth = IntArray(lines.size)

        var maximumLineWidth = 0

        lines.fastForEachIndexed { i, line ->
            linesWidth[i] = font!!.getStringWidth(line)
            maximumLineWidth = max(maximumLineWidth, linesWidth[i])
        }

        contentWidth = maximumLineWidth.toFloat()
        contentHeight = (lines.size * font!!.lineHeight + (lines.size - 1) * font!!.lineGap).toFloat()
        onContentSizeMeasured()

        textureBuffer!!.update(font!!, lines)
        vertexBuffer!!.update(font!!, maximumLineWidth, linesWidth, lines, horizontalAlign)
    }


    override fun getVertexBuffer(): TextVertexBuffer? {
        return super.getVertexBuffer() as TextVertexBuffer?
    }


    override fun finalize() {
        if (textureBuffer!!.isManaged) {
            textureBuffer!!.unloadFromActiveBufferObjectManager()
        }
        super.finalize()
    }

}