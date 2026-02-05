package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Fonts
import com.reco1l.andengine.theme.Size
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.*
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
                currentLength = value.codePointCount(0, value.length)

                if (currentLength > previousLength) {
                    requestBufferUpdate()
                }

                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The font used to render the text.
     */
    var font: Font? = null
        private set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The font family to use for this text.
     */
    var fontFamily = Fonts.NunitoMedium
        set(value) {
            if (field != value) {
                field = value
                fontSettingsChanged = true
            }
        }

    /**
     * The font size to use for this text.
     */
    var fontSize = FontSize.SM
        set(value) {
            if (field != value) {
                field = value
                fontSettingsChanged = true
            }
        }

    /**
     * Called when the font settings (font size or family) change.
     */
    var onFontSettingsChange: () -> Unit = {
        val oldFont = font

        if (oldFont != null) {
            UIEngine.current.resources.unsubscribeFromFont(oldFont, this)
        }

        val newFont = UIEngine.current.resources.getOrStoreFont(fontSize, fontFamily)
        font = newFont
        UIEngine.current.resources.subscribeToFont(newFont, this)

        invalidate(InvalidationFlag.Content)
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
        set(value) {
            if (field != value) {
                field = value
                clipToBounds = value != Axes.None
            }
        }

    /**
     * The speed of the auto scroll animation in pixels per second.
     */
    var autoScrollSpeed = 15f

    /**
     * The time to wait before re-starting the auto scroll animation in seconds
     */
    var autoScrollTimeout = 3f

    /**
     * Whether to wrap text that exceeds the width of the component.
     */
    var wrapText = false
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }


    protected open val textViewportWidth: Float
        get() = innerWidth

    protected open val textViewportHeight: Float
        get() = innerHeight

    protected open val textViewportX: Float
        get() = padding.left

    protected open val textViewportY: Float
        get() = padding.top


    private var currentLength = 0
    private var scrollX = 0f
    private var scrollY = 0f
    private var scrollXTimeoutElapsed = 0f
    private var scrollYTimeoutElapsed = 0f
    private var fontSettingsChanged = true

    private var lines: List<String>? = null
    private var linesWidth: IntArray? = null


    init {
        width = Size.Auto
        height = Size.Auto

        clipToBounds = true

        style = {
            fontSize = FontSize.SM
        }
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

        val originalLines = text.split('\n')

        if (wrapText && width > 0f && rawWidth != Size.Auto) {
            val wrappedLines = mutableListOf<String>()
            val wrappedLinesWidth = mutableListOf<Int>()

            originalLines.forEach { originalLine ->
                wrapLine(originalLine, font, width.toInt(), wrappedLines, wrappedLinesWidth)
            }

            lines = wrappedLines
            linesWidth = wrappedLinesWidth.toIntArray()
        } else {
            lines = originalLines

            linesWidth = IntArray(lines!!.size) { i ->
                val line = lines!![i]
                var width = 0
                var charIndex = 0

                while (charIndex < line.length) {
                    val codePoint = line.codePointAt(charIndex)
                    val charCount = Character.charCount(codePoint)

                    val characterString = line.substring(charIndex, charIndex + charCount)

                    width += font.getLetter(characterString).mAdvance
                    charIndex += charCount
                }
                width
            }
        }

        contentWidth = if (linesWidth!!.isNotEmpty()) linesWidth!!.max().toFloat() else 0f
        contentHeight = (lines!!.size * font.lineHeight + (lines!!.size - 1) * font.lineGap).toFloat()

        requestBufferUpdate()
    }

    override fun onSizeChanged() {
        if (wrapText) {
            invalidate(InvalidationFlag.Content)
        }
        requestBufferUpdate()
    }

    private fun wrapLine(line: String, font: Font, maxWidth: Int, outputLines: MutableList<String>, outputWidths: MutableList<Int>) {
        if (line.isEmpty()) {
            outputLines.add("")
            outputWidths.add(0)
            return
        }

        var currentLineStart = 0
        var currentWidth = 0
        var lastSpaceIndex = -1
        var lastSpaceWidth = 0
        var charIndex = 0

        while (charIndex < line.length) {
            val codePoint = line.codePointAt(charIndex)
            val charCount = Character.charCount(codePoint)
            val characterString = line.substring(charIndex, charIndex + charCount)

            val letterAdvance = font.getLetter(characterString).mAdvance
            val newWidth = currentWidth + letterAdvance

            if (characterString == " ") {
                lastSpaceIndex = charIndex
                lastSpaceWidth = currentWidth
            }

            if (newWidth > maxWidth && currentWidth > 0) {
                if (lastSpaceIndex > currentLineStart) {
                    outputLines.add(line.substring(currentLineStart, lastSpaceIndex))
                    outputWidths.add(lastSpaceWidth)
                    currentLineStart = lastSpaceIndex + 1
                    charIndex = currentLineStart
                    currentWidth = 0
                    lastSpaceIndex = -1
                    lastSpaceWidth = 0
                    continue
                } else {
                    outputLines.add(line.substring(currentLineStart, charIndex))
                    outputWidths.add(currentWidth)
                    currentLineStart = charIndex
                    currentWidth = 0
                    lastSpaceIndex = -1
                    lastSpaceWidth = 0
                    continue
                }
            }

            currentWidth = newWidth
            charIndex += charCount
        }

        if (currentLineStart < line.length) {
            outputLines.add(line.substring(currentLineStart))
            outputWidths.add(currentWidth)
        }
    }

    private fun nextPowerOfTwo(n: Int): Int {
        if (n <= 0) return 1
        val highest = n.takeHighestOneBit()
        return if (highest == n) n else highest shl 1
    }

    override fun createBuffer(): CompoundBuffer {
        val capacity = nextPowerOfTwo(currentLength)
        return UITextCompoundBuffer(capacity)
    }

    override fun generateBufferCacheKey(): String {
        val capacity = nextPowerOfTwo(currentLength)
        return "UITextVBO@$capacity,$fontSize,$fontFamily"
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


    override fun onManagedDraw(gl: GL10, camera: Camera) {
        if (fontSettingsChanged) {
            fontSettingsChanged = false
            onFontSettingsChange()
        }
        super.onManagedDraw(gl, camera)
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


    override fun finalize() {
        super.finalize()

        val font = font ?: return
        UIEngine.current.resources.unsubscribeFromFont(font, this)
    }


    //region Buffers

    class TextVertexBuffer(val length: Int) : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = VERTICES_PER_CHARACTER * length,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        fun update(component: UIText, font: Font?, lines: List<String>?, linesWidth: IntArray?) {

            if (font == null || lines == null || linesWidth == null) {
                mFloatBuffer.clear()
                return
            }

            val lineHeight = font.lineHeight + font.lineGap
            var i = 0

            lines.fastForEachIndexed { lineIndex, line ->

                var lineX =  component.textViewportX + component.textViewportWidth * component.alignment.x - linesWidth[lineIndex] * component.alignment.x
                val lineY = component.textViewportY + component.textViewportHeight * component.alignment.y - lines.size * lineHeight * component.alignment.y + lineIndex * lineHeight

                var charIndex = 0
                while (charIndex < line.length) {
                    val codePoint = line.codePointAt(charIndex)
                    val charCount = Character.charCount(codePoint)
                    val characterString = line.substring(charIndex, charIndex + charCount)

                    val letter = font.getLetter(characterString)

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
                    charIndex += charCount
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
                var charIndex = 0
                while (charIndex < line.length) {
                    val codePoint = line.codePointAt(charIndex)
                    val charCount = Character.charCount(codePoint)

                    val characterString = line.substring(charIndex, charIndex + charCount)
                    val letter = font.getLetter(characterString)

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
                    
                    charIndex += charCount
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
open class CompoundText : UIText() {

    /**
     * The spacing between the icons and the text.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The leading icon.
     */
    var leadingIcon: UIComponent? = null
        set(value) {
            if (field != value) {
                field = value
                value?.setParent(this, AttachmentMode.Child)
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The trailing icon.
     */
    var trailingIcon: UIComponent? = null
        set(value) {
            if (field != value) {
                field = value
                value?.setParent(this, AttachmentMode.Child)
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The width of the text without icons.
     */
    var textWidth = 0f
        private set


    override val textViewportX: Float
        get() = padding.left + (leadingIcon?.let { it.width + spacing } ?: 0f)

    override val textViewportWidth: Float
        get() = innerWidth - (leadingIcon?.let { it.width + spacing } ?: 0f) - (trailingIcon?.let { it.width + spacing } ?: 0f)


    override fun onContentChanged() {
        super.onContentChanged()
        textWidth = contentWidth

        val leadingIcon = leadingIcon
        val trailingIcon = trailingIcon

        var totalWidth = 0f

        if (leadingIcon != null) {
            leadingIcon.anchor = Anchor.CenterLeft
            leadingIcon.origin = Anchor.CenterLeft
            leadingIcon.setSize(fontSize, fontSize)

            totalWidth += leadingIcon.width + spacing
        }

        totalWidth += textWidth

        if (trailingIcon != null) {
            trailingIcon.anchor = Anchor.CenterLeft
            trailingIcon.origin = Anchor.CenterLeft
            trailingIcon.x = totalWidth + spacing
            trailingIcon.setSize(fontSize, fontSize)

            totalWidth += spacing + trailingIcon.width
        }

        contentWidth = totalWidth

        // Move trailing icon to the end.
        if (width > intrinsicWidth) {
            trailingIcon?.x = innerWidth - trailingIcon.width
        }

        requestBufferUpdate()
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        leadingIcon?.onDraw(gl, camera)
        trailingIcon?.onDraw(gl, camera)
    }
}


fun UITextCompoundBuffer(capacity: Int): CompoundBuffer {
    return CompoundBuffer(
        UIText.TextTextureBuffer(capacity),
        UIText.TextVertexBuffer(capacity)
    )
}