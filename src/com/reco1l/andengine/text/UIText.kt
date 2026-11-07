package com.reco1l.andengine.text

import android.util.Log
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import org.andengine.engine.camera.Camera
import org.andengine.entity.text.TextBatching
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.text.*
import org.andengine.graphics.text.exception.FontException
import ru.nsu.ccfit.zuev.osu.*
import kotlin.math.*

/**
 * A text entity that can be displayed on the screen.
 *
 * The glyphs are drawn as quads of the engine's [QuadBatcher], so consecutive texts of the same
 * glyph atlas and blend share one draw. [depthState] is not applied: batched quads are drawn without
 * a depth test.
 */
open class UIText : UIBufferedComponent<IBuffer>() {

    /**
     * The text to be displayed
     */
    var text: String = ""
        set(value) {
            if (field != value) {
                field = value
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


    private var scrollX = 0f
    private var scrollY = 0f
    private var scrollXTimeoutElapsed = 0f
    private var scrollYTimeoutElapsed = 0f


    private var linesWidth = FloatArray(0)

    /** The letters of every line, resolved when the content changes so drawing does not look them up. */
    private var lineLetters: Array<Array<Letter>> = emptyArray()


    init {
        width = MatchContent
        height = MatchContent
        invalidate(InvalidationFlag.Content)
    }


    override fun onContentChanged() {
        val text = text
        val font = font

        if (font == null) {
            linesWidth = FloatArray(0)
            lineLetters = emptyArray()
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        val lines = text.split('\n')
        val lineLetters = Array(lines.size) { i -> Array(lines[i].length) { c -> font.safeGetLetter(lines[i][c]) } }

        this.lineLetters = lineLetters
        linesWidth = FloatArray(lines.size) { i -> lineLetters[i].fold(0f) { acc, letter -> acc + letter.mAdvance } }

        contentWidth = linesWidth.max()
        contentHeight = lines.size.toFloat() * font.lineHeight
    }

    override fun doDraw(drawContext: DrawContext, pCamera: Camera) {
        onApplyColor(drawContext)

        clearIfRequested(drawContext)

        val font = font ?: return
        val linesWidth = linesWidth
        val lineLetters = lineLetters

        if (lineLetters.isEmpty()) {
            return
        }

        val blend = resolvedBlendMode
        val fillColor = TextBatching.fillColor(font, drawRed, drawGreen, drawBlue, drawAlpha)
        val strokeColor = TextBatching.strokeColor(font, drawRed, drawGreen, drawBlue, drawAlpha)

        QuadBatcher.setProjection(drawContext.projectionGLMatrix)
        val modelView = drawContext.modelViewGLMatrix

        val lineHeight = font.lineHeight
        val alignment = alignment

        for (lineIndex in lineLetters.indices) {

            val letters = lineLetters[lineIndex]

            var lineX = width * alignment.x - linesWidth[lineIndex] * alignment.x
            val lineY = height * alignment.y - lineLetters.size * lineHeight * alignment.y + lineIndex * lineHeight

            for (i in letters.indices) {
                val letter = letters[i]

                // Use the glyph's actual offset and height so glyphs are not
                // vertically stretched to fill the full line height.
                val glyphX = lineX + letter.mOffsetX
                val glyphY = lineY + letter.mOffsetY

                TextBatching.addLetter(
                    font, letter, blend, modelView,
                    glyphX, glyphY, glyphX + letter.mWidth, glyphY + letter.mHeight,
                    fillColor, strokeColor
                )

                lineX += letter.mAdvance
            }
        }
    }

    override fun onApplyTransformations(drawContext: DrawContext) {

        val scrollTranslationX = if (autoScrollAxes.isHorizontal) scrollX else 0f
        val scrollTranslationY = if (autoScrollAxes.isVertical) scrollY else 0f

        if (scrollTranslationX != 0f || scrollTranslationY != 0f) {
            drawContext.translateModelViewGLMatrixf(-scrollTranslationX, -scrollTranslationY, 0f)
        }

        super.onApplyTransformations(drawContext)
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


    companion object {

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
