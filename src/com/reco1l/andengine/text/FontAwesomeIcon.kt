package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.IconVariant
import org.andengine.engine.camera.*
import org.andengine.entity.text.TextBatching
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.font.*
import org.andengine.opengl.util.GLState
import kotlin.math.*

/**
 * An icon of the icon font that can be displayed on the screen.
 *
 * The glyph is drawn as a quad of the engine's [QuadBatcher]. [depthInfo] is not applied: batched
 * quads are drawn without a depth test.
 */
open class FontAwesomeIcon(icon: Int) : UIBufferedComponent<IBuffer>() {

    /**
     * The text to be displayed
     */
    var icon: Int = icon
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The variant of the icon font.
     */
    var iconVariant = IconVariant.Solid
        set(value) {
            if (field != value) {
                field = value
                fontSettingsChanged = true
            }
        }

    /**
     * The size of the icon font.
     */
    var iconSize = 24f
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

        val newFont = UIEngine.current.resources.getOrStoreFont(iconSize, iconVariant)
        font = newFont
        UIEngine.current.resources.subscribeToFont(newFont, this)

        invalidate(InvalidationFlag.Content)
    }


    private var font: Font? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    private var fontSettingsChanged = true


    init {
        width = MatchContent
        height = MatchContent
    }


    /** The glyph of [icon] in [font], resolved when the content changes. */
    private var letter: Letter? = null

    override fun onContentChanged() {
        val font = font

        if (font == null) {
            letter = null
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        val letter = font.getLetter(icon.toChar())

        this.letter = letter
        contentWidth = letter.mAdvance
        contentHeight = font.lineHeight
    }

    override fun onCreateBuffer(): IBuffer? {
        // The glyph goes through the quad batch; there is no buffer of its own.
        return null
    }

    override fun onUpdateBuffer() {
        // Nothing to do here, there is no buffer.
    }

    override fun onBindShader(pGLState: GLState) {
        // The batch binds its own shader.
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        onApplyColor(pGLState)

        val clearMask = clearMask()
        if (clearMask != 0) {
            GlesBackend.state.clear(clearMask)
        }

        val font = font ?: return
        val letter = letter ?: return

        if (contentWidth <= 0f || contentHeight <= 0f) {
            return
        }

        val scale = min(width / contentWidth, height / contentHeight)

        // The glyph keeps its own proportions, scaled like the line, and is centered in the icon.
        val x0 = width * 0.5f - letter.mWidth * scale * 0.5f
        val y0 = height * 0.5f - letter.mHeight * scale * 0.5f

        QuadBatcher.setProjection(pGLState.projectionGLMatrix)

        TextBatching.addLetter(
            font, letter, batchBlendMode() ?: BlendMode.Alpha, pGLState.modelViewGLMatrix,
            x0, y0, x0 + letter.mWidth * scale, y0 + letter.mHeight * scale,
            TextBatching.fillColor(font, drawRed, drawGreen, drawBlue, drawAlpha),
            TextBatching.strokeColor(font, drawRed, drawGreen, drawBlue, drawAlpha)
        )
    }

    override fun onManagedDraw(pGLState: GLState, pCamera: Camera) {
        if (fontSettingsChanged) {
            fontSettingsChanged = false
            onFontSettingsChange()
        }
        super.onManagedDraw(pGLState, pCamera)
    }


    override fun onDetached() {
        super.onDetached()

        val font = font ?: return
        UIEngine.current.resources.unsubscribeFromFont(font, this)

        this.font = null
        fontSettingsChanged = true
    }
}
