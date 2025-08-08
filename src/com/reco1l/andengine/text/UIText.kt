package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.*
import ru.nsu.ccfit.zuev.osu.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
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
                    invalidateBuffer(BufferInvalidationFlag.Instance)
                }

                invalidate(InvalidationFlag.Content)
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
                invalidateBuffer(BufferInvalidationFlag.Data)
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
    private var linesWidth: IntArray? = null


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
            linesWidth = IntArray(0)
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        lines = text.split('\n')
        linesWidth = IntArray(lines!!.size) { i -> lines!![i].sumOf { char -> font.getLetter(char).mAdvance } }

        contentWidth = linesWidth!!.max().toFloat()
        contentHeight = (lines!!.size * font.lineHeight + (lines!!.size - 1) * font.lineGap).toFloat()

        invalidateBuffer(BufferInvalidationFlag.Data)
    }

    override fun onSizeChanged() {
        super.onSizeChanged()
        invalidateBuffer(BufferInvalidationFlag.Data)
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

    override fun onDeclarePointers(gl: GL10) {
        super.onDeclarePointers(gl)
        font?.texture?.bind(gl)
    }

    override fun onApplyTransformations(gl: GL10, camera: Camera) {

        val scrollTranslationX = if (autoScrollAxes.isHorizontal) scrollX else 0f
        val scrollTranslationY = if (autoScrollAxes.isVertical) scrollY else 0f

        if (scrollTranslationX != 0f || scrollTranslationY != 0f) {
            gl.glTranslatef(-scrollTranslationX, -scrollTranslationY, 0f)
        }

        super.onApplyTransformations(gl, camera)
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

        fun update(entity: UIText, font: Font?, lines: List<String>?, linesWidth: IntArray?) {

            if (font == null || lines == null || linesWidth == null) {
                mFloatBuffer.clear()
                return
            }

            val lineHeight = font.lineHeight + font.lineGap
            var i = 0

            lines.fastForEachIndexed { lineIndex, line ->

                var lineX = entity.width * entity.alignment.x - linesWidth[lineIndex] * entity.alignment.x
                val lineY = entity.height * entity.alignment.y - lines.size * lineHeight * entity.alignment.y + lineIndex * lineHeight

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

        override fun draw(gl: GL10, entity: UIBufferedComponent<*>) {
            entity as UIText
            gl.glDrawArrays(drawTopology, 0, VERTICES_PER_CHARACTER * min(entity.currentLength, length))
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