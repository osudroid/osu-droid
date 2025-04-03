package com.reco1l.andengine.text

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.opengl.font.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import javax.microedition.khronos.opengles.GL11.GL_STATIC_DRAW

/**
 * A text entity that can be displayed on the screen.
 */
open class ExtendedText : CompoundBufferedEntity() {

    /**
     * The text to be displayed
     */
    var text: String = ""
        set(value) {
            if (field != value) {
                field = value

                if (value.length > currentLength) {
                    currentLength = value.length
                    rebuildBuffer()
                }
                invalidateBuffer()
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
                invalidateBuffer()
            }
        }

    /**
     * The alignment of the text.
     */
    var alignment = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer()
            }
        }


    private var currentLength = 0


    init {
        width = FitContent
        height = FitContent
    }


    override fun onRebuildBuffer(gl: GL10) {
        super.onRebuildBuffer(gl)

        addBuffer(TextTextureBuffer())
        addBuffer(TextVertexBuffer())
    }

    override fun onUpdateBuffer(gl: GL10, vararg data: Any) {

        val text = text
        val font = font ?: return

        val lines = text.split('\n').toTypedArray()
        val linesWidth = IntArray(lines.size) { font.getStringWidth(lines[it]) }

        contentWidth = linesWidth.max().toFloat()
        contentHeight = (lines.size * font.lineHeight + (lines.size - 1) * font.lineGap).toFloat()

        super.onUpdateBuffer(gl, font, lines, linesWidth)
    }

    override fun onDeclarePointers(gl: GL10) {
        super.onDeclarePointers(gl)
        font?.texture?.bind(gl)
    }


    //region Buffers

    inner class TextVertexBuffer : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = VERTICES_PER_CHARACTER * currentLength,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        @Suppress("UNCHECKED_CAST")
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            val font = data[0] as Font
            val lines = data[1] as Array<String>
            val linesWidth = data[2] as IntArray

            val lineHeight = font.lineHeight + font.lineGap
            var i = 0

            lines.fastForEachIndexed { lineIndex, line ->

                var lineX = innerWidth * alignment.x - linesWidth[lineIndex] * alignment.x
                val lineY = innerHeight * alignment.y - (lines.size * lineHeight) * alignment.y + lineIndex * lineHeight

                line.forEach { character ->
                    val letter = font.getLetter(character)

                    val letterX = lineX + letter.mWidth
                    val letterY = lineY + font.lineHeight

                    setPosition(0)

                    putVertex(i++, lineX, lineY)
                    putVertex(i++, lineX, letterY)
                    putVertex(i++, letterX, letterY)
                    putVertex(i++, letterX, letterY)
                    putVertex(i++, letterX, lineY)
                    putVertex(i++, lineX, lineY)

                    setPosition(0)

                    lineX += letter.mAdvance
                }
            }
        }
    }


    inner class TextTextureBuffer : TextureCoordinatesBuffer(
        vertexCount = VERTICES_PER_CHARACTER * currentLength,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        @Suppress("UNCHECKED_CAST")
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {
            setPosition(0)

            val font = data[0] as Font
            val lines = data[1] as Array<String>

            lines.fastForEach { line ->
                line.forEach { character ->

                    val letter = font.getLetter(character)

                    val letterTextureX = letter.mTextureX
                    val letterTextureY = letter.mTextureY
                    val letterTextureX2 = letterTextureX + letter.mTextureWidth
                    val letterTextureY2 = letterTextureY + letter.mTextureHeight

                    putVertex(letterTextureX, letterTextureY)
                    putVertex(letterTextureX, letterTextureY2)
                    putVertex(letterTextureX2, letterTextureY2)
                    putVertex(letterTextureX2, letterTextureY2)
                    putVertex(letterTextureX2, letterTextureY)
                    putVertex(letterTextureX, letterTextureY)
                }
            }

            setPosition(0)
        }

    }

    //endregion


    companion object {

        private const val VERTICES_PER_CHARACTER = 6

    }

}