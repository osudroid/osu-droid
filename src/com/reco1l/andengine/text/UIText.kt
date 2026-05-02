package com.reco1l.andengine.text

import android.opengl.GLES20
import android.util.Log
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.toolkt.kotlin.*
import org.andengine.opengl.font.*
import org.andengine.opengl.font.exception.FontException
import org.andengine.opengl.shader.PositionTextureCoordinatesUniformColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.*
import javax.microedition.khronos.opengles.GL11.GL_STATIC_DRAW
import kotlin.math.*

/**
 * A text entity that can be displayed on the screen.
 */
open class UIText : UIBufferedComponent<CompoundBuffer>() {

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
                    requestNewBuffer()
                }

                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The font to use for this text.
     * It must be already loaded and ready to use before setting it.
     */
    var font: Font? = ResourceManager.getInstance().getFont("smallFont")
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The alignment of the text.
     */
    var alignment = Anchor.TopLeft
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }

    /**
     * Which axes to scroll the text automatically when it overflows.
     */
    var autoScrollAxes = Axes.X

    /**
     * The speed of the auto scroll animation in pixels per second.
     */
    var autoScrollSpeed = 15f

    /**
     * The time to wait before re-starting the auto scroll animation in seconds
     */
    var autoScrollTimeout = 3f


    private var currentLength = 0

    private var scrollX = 0f
    private var scrollY = 0f
    private var scrollXTimeoutElapsed = 0f
    private var scrollYTimeoutElapsed = 0f


    private var lines: List<String>? = null
    private var linesWidth: FloatArray? = null


    init {
        width = MatchContent
        height = MatchContent
        invalidate(InvalidationFlag.Content)
    }


    override fun onContentChanged() {
        val text = text
        val font = font

        if (font == null) {
            lines = emptyList()
            linesWidth = FloatArray(0)
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        lines = text.split('\n')
        linesWidth = FloatArray(lines!!.size) { i -> lines!![i].fold(0f) { acc, char -> acc + font.safeGetLetter(char).mAdvance } }

        contentWidth = linesWidth!!.max()
        contentHeight = lines!!.size.toFloat() * font.lineHeight

        requestBufferUpdate()
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }

    override fun onCreateBuffer(): CompoundBuffer {
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

    override fun onUpdateBuffer() {
        buffer?.getFirstOf<TextTextureBuffer>()?.update(font, lines)
        buffer?.getFirstOf<TextVertexBuffer>()?.update(this, font, lines, linesWidth)
    }

    override fun onDeclarePointers(gl: GLState) {
        super.onDeclarePointers(gl)
        font?.texture?.bind(gl)
    }

    override fun onBindShader(pGLState: GLState) {
        val shader = PositionTextureCoordinatesUniformColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        // Upload MVP matrix
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformModelViewPositionMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(
                PositionTextureCoordinatesUniformColorShaderProgram.sUniformModelViewPositionMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // Upload texture unit sampler
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformTexture0Location >= 0) {
            GLES20.glUniform1i(PositionTextureCoordinatesUniformColorShaderProgram.sUniformTexture0Location, 0)
        }

        // Upload color uniform
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformColorLocation >= 0) {
            GLES20.glUniform4f(
                PositionTextureCoordinatesUniformColorShaderProgram.sUniformColorLocation,
                drawRed, drawGreen, drawBlue, drawAlpha
            )
        }

        // Disable color vertex attribute (this shader uses uniform color)
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        // UV (attr 3) is already set up by TextTextureBuffer inside the CompoundBuffer.beginDraw()
    }

    override fun onApplyTransformations(pGLState: GLState) {

        val scrollTranslationX = if (autoScrollAxes.isHorizontal) scrollX else 0f
        val scrollTranslationY = if (autoScrollAxes.isVertical) scrollY else 0f

        if (scrollTranslationX != 0f || scrollTranslationY != 0f) {
            pGLState.translateModelViewGLMatrixf(-scrollTranslationX, -scrollTranslationY, 0f)
        }

        super.onApplyTransformations(pGLState)
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (autoScrollAxes != Axes.None) {

            fun processAutoScroll(currentScroll: Float, maxScroll: Float, currentTimeout: Float) : Pair<Float, Float> {

                if (currentScroll == 0f && currentTimeout < autoScrollTimeout) {
                    return currentScroll to currentTimeout + deltaTimeSec
                }

                if (currentScroll >= maxScroll) {
                    if (currentTimeout > autoScrollTimeout) {
                        return 0f to 0f
                    }
                    return currentScroll to currentTimeout + deltaTimeSec
                }

                return min(currentScroll + autoScrollSpeed * deltaTimeSec, maxScroll) to 0f
            }

            val maxScrollX = contentWidth - width

            if (autoScrollAxes.isHorizontal && maxScrollX > 0) {
                val (x, timeout) = processAutoScroll(scrollX, maxScrollX, scrollXTimeoutElapsed)

                scrollX = x
                scrollXTimeoutElapsed = timeout
            } else {
                scrollX = 0f
            }

            val maxScrollY = contentHeight - height

            if (autoScrollAxes.isVertical && maxScrollY > 0) {
                val (y, timeout) = processAutoScroll(scrollY, maxScrollY, scrollYTimeoutElapsed)

                scrollY = y
                scrollYTimeoutElapsed = timeout
            } else {
                scrollY = 0f
            }

        } else {
            scrollX = 0f
            scrollY = 0f
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    //region Buffers

    class TextVertexBuffer(val length: Int) : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = VERTICES_PER_CHARACTER * length,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        fun update(entity: UIText, font: Font?, lines: List<String>?, linesWidth: FloatArray?) {

            if (font == null || lines == null || linesWidth == null) {
                mFloatBuffer.clear()
                return
            }

            val lineHeight = font.lineHeight
            var i = 0

            lines.fastForEachIndexed { lineIndex, line ->

                var lineX = entity.width * entity.alignment.x - linesWidth[lineIndex] * entity.alignment.x
                val lineY = entity.height * entity.alignment.y - lines.size * lineHeight * entity.alignment.y + lineIndex * lineHeight

                line.forEach { character ->
                    val letter = font.safeGetLetter(character)

                    // Use the glyph's actual offset and height so glyphs are not
                    // vertically stretched to fill the full line height.
                    val glyphX = lineX + letter.mOffsetX
                    val glyphY = lineY + letter.mOffsetY
                    val letterX = glyphX + letter.mWidth
                    val letterY = glyphY + letter.mHeight

                    setPosition(0)

                    putVertex(i++, glyphX, glyphY)
                    putVertex(i++, glyphX, letterY)
                    putVertex(i++, letterX, letterY)
                    putVertex(i++, letterX, letterY)
                    putVertex(i++, letterX, glyphY)
                    putVertex(i++, glyphX, glyphY)

                    setPosition(0)

                    lineX += letter.mAdvance
                }
            }
        }

        override fun draw(gl: GLState, entity: UIBufferedComponent<*>) {
            entity as UIText
            GLES20.glDrawArrays(drawTopology, 0, VERTICES_PER_CHARACTER * min(entity.currentLength, length))
        }
    }


    class TextTextureBuffer(length: Int) : TextureCoordinatesBuffer(
        vertexCount = VERTICES_PER_CHARACTER * length,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        fun update(font: Font?, lines: List<String>?) {

            if (font == null || lines == null) {
                mFloatBuffer.clear()
                return
            }

            setPosition(0)

            lines.fastForEach { line ->
                line.forEach { character ->

                    val letter = font.safeGetLetter(character)

                    val u = letter.mU
                    val v = letter.mV
                    val u2 = letter.mU2
                    val v2 = letter.mV2

                    putVertex(u, v)
                    putVertex(u, v2)
                    putVertex(u2, v2)
                    putVertex(u2, v2)
                    putVertex(u2, v)
                    putVertex(u, v)
                }
            }

            setPosition(0)
        }

    }

    //endregion


    companion object {
        private const val VERTICES_PER_CHARACTER = 6

        /**
         * Safely retrieve a [Letter] for [character] from [font], returning the letter for '?'
         * as a fallback when the glyph atlas is full ([FontException]).
         */
        internal fun Font.safeGetLetter(character: Char): Letter {
            return try {
                getLetter(character)
            } catch (_: FontException) {
                Log.w("UIText", "Font atlas full for character '${character}' (U+${character.code.toString(16).uppercase()}), using fallback.")
                try { getLetter('?') } catch (_: FontException) { getLetter(' ') }
            }
        }
    }

}


/**
 * A compound text entity that can be displayed with leading and trailing icons.
 */
open class CompoundText : UIContainer() {

    /**
     * The text entity.
     */
    val textEntity = UIText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }


    var spacing = 0f

    //region Shortcuts

    var text by textEntity::text

    var font by textEntity::font

    var alignment by textEntity::alignment

    //endregion

    //region Icons

    /**
     * The leading icon.
     */
    var leadingIcon: UIComponent? = null
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value

                if (value != null) {
                    onIconChange(value)
                    attachChild(value, 0)
                }
            }
        }

    /**
     * The trailing icon.
     */
    var trailingIcon: UIComponent? = null
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value

                if (value != null) {
                    onIconChange(value)
                    attachChild(value)
                }
            }
        }


    /**
     * Which leading icon's size axes should match the text size.
     */
    var autoSizeLeadingIcon = Axes.Both

    /**
     * Which trailing icon's size axes should match the text size.
     */
    var autoSizeTrailingIcon = Axes.Both


    /**
     * Called when one of the icons changes.
     */
    open var onIconChange: (UIComponent) -> Unit = { icon ->
        val anchor = if (icon === leadingIcon) Anchor.CenterLeft else Anchor.CenterRight
        icon.anchor = anchor
        icon.origin = anchor
    }

    //endregion

    override fun onContentChanged() {
        contentWidth = textEntity.contentWidth + (leadingIcon?.let { it.width + spacing } ?: 0f) + (trailingIcon?.let { it.width + spacing } ?: 0f)
        contentHeight = textEntity.height
    }


    init {
        +textEntity
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        val leadingIcon = leadingIcon
        val trailingIcon = trailingIcon

        val iconSize = textEntity.height

        if (leadingIcon != null) {
            if (autoSizeLeadingIcon.isHorizontal) {
                leadingIcon.width = iconSize
            }
            if (autoSizeLeadingIcon.isVertical) {
                leadingIcon.height = iconSize
            }
        }

        if (trailingIcon != null) {
            if (autoSizeTrailingIcon.isHorizontal) {
                trailingIcon.width = iconSize
            }
            if (autoSizeTrailingIcon.isVertical) {
                trailingIcon.height = iconSize
            }
        }


        val leadingIconSize = leadingIcon?.let { it.width + spacing } ?: 0f
        val trailingIconSize = trailingIcon?.let { it.width + spacing } ?: 0f

        textEntity.x = leadingIconSize
        textEntity.width = width - leadingIconSize - trailingIconSize

        super.onManagedUpdate(deltaTimeSec)
    }
}


fun UITextCompoundBuffer(capacity: Int): CompoundBuffer {
    return CompoundBuffer(
        UIText.TextTextureBuffer(capacity),
        UIText.TextVertexBuffer(capacity)
    )
}