package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.opengl.font.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import javax.microedition.khronos.opengles.GL11.GL_STATIC_DRAW
import kotlin.math.*

/**
 * A text entity that can be displayed on the screen.
 */
open class ExtendedText : BufferedEntity<CompoundBuffer>() {

    /**
     * The text to be displayed
     */
    var text: String = ""
        set(value) {
            if (field != value) {
                field = value

                val previousLength = currentLength
                currentLength = value.length

                if (currentLength > previousLength) {
                    invalidateBuffer(BufferInvalidationFlag.Instance)
                } else {
                    invalidateBuffer(BufferInvalidationFlag.Data)
                }

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
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }

    /**
     * The alignment of the text.
     */
    var alignment = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }


    private var currentLength = 0


    init {
        width = FitContent
        height = FitContent
    }


    override fun onCreateBuffer(gl: GL10): CompoundBuffer {
        val currentLength = currentLength
        val currentBuffer = buffer?.getFirstOf<TextVertexBuffer>()

        if (currentBuffer == null || currentLength > currentBuffer.length) {
            return CompoundBuffer(
                TextTextureBuffer(currentLength),
                TextVertexBuffer(currentLength)
            )
        }
        return buffer!!
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

    inner class TextVertexBuffer(val length: Int) : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = VERTICES_PER_CHARACTER * length,
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

                var lineX = width * alignment.x - linesWidth[lineIndex] * alignment.x
                val lineY = height * alignment.y - lines.size * lineHeight * alignment.y + lineIndex * lineHeight

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

        override fun draw(gl: GL10, entity: BufferedEntity<*>) {
            gl.glDrawArrays(drawTopology, 0, VERTICES_PER_CHARACTER * min(currentLength, length))
        }
    }


    inner class TextTextureBuffer(length: Int) : TextureCoordinatesBuffer(
        vertexCount = VERTICES_PER_CHARACTER * length,
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