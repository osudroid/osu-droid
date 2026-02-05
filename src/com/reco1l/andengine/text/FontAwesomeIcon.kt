package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.buffered.VertexBuffer
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.IconVariant
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*
import javax.microedition.khronos.opengles.GL11.GL_STATIC_DRAW
import kotlin.math.*
import com.reco1l.andengine.theme.Size

/**
 * A text entity that can be displayed on the screen.
 */
open class FontAwesomeIcon(icon: Int) : UIBufferedComponent<CompoundBuffer>() {

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
    var iconSize = FontSize.SM
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
        width = Size.Auto
        height = Size.Auto
    }


    override fun onContentChanged() {
        val text = icon.toChar()
        val font = font

        if (font == null) {
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        contentWidth = font.getLetter(text).mAdvance.toFloat()
        contentHeight = font.lineHeight.toFloat()

        requestBufferUpdate()
    }

    override fun createBuffer(): CompoundBuffer {
        return CompoundBuffer(IconTextureBuffer(), IconVertexBuffer())
    }

    override fun canReuseBuffer(buffer: CompoundBuffer): Boolean {
        return true
    }

    override fun generateBufferCacheKey(): String {
        return "IconVBO@$iconSize,$iconVariant,$icon"
    }

    override fun onUpdateBuffer() {
        val font = font
        val letter = font?.getLetter(icon.toChar())

        buffer?.getFirstOf<IconTextureBuffer>()?.update(letter)
        buffer?.getFirstOf<IconVertexBuffer>()?.update(this, font, letter)
    }

    override fun onDeclarePointers(gl: GL10) {
        super.onDeclarePointers(gl)
        font?.texture?.bind(gl)
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {
        if (fontSettingsChanged) {
            fontSettingsChanged = false
            onFontSettingsChange()
        }
        super.onManagedDraw(gl, camera)
    }


    override fun finalize() {
        super.finalize()

        val font = font ?: return
        UIEngine.current.resources.unsubscribeFromFont(font, this)
    }



    //region Buffers

    class IconVertexBuffer() : VertexBuffer(
        drawTopology = GL_TRIANGLES,
        vertexCount = VERTICES_PER_CHARACTER,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        fun update(component: FontAwesomeIcon, font: Font?, letter: Letter?) {

            if (font == null || letter == null) {
                mFloatBuffer.clear()
                return
            }

            val lineHeight = font.lineHeight + font.lineGap
            var i = 0

            val scale = min(component.width / component.contentWidth, component.height / component.contentHeight)

            val lineX = component.width * 0.5f - component.contentWidth * scale * 0.5f
            val lineY = component.height * 0.5f - lineHeight * scale * 0.5f

            val letterX = lineX + letter.mWidth * scale
            val letterY = lineY + font.lineHeight * scale

            setPosition(0)

            putVertex(i++, lineX, lineY)
            putVertex(i++, lineX, letterY)
            putVertex(i++, letterX, letterY)
            putVertex(i++, letterX, letterY)
            putVertex(i++, letterX, lineY)
            putVertex(i++, lineX, lineY)


            setPosition(0)
        }

        override fun draw(gl: GL10, entity: UIBufferedComponent<*>) {
            gl.glDrawArrays(drawTopology, 0, VERTICES_PER_CHARACTER)
        }
    }


    class IconTextureBuffer() : TextureCoordinatesBuffer(
        vertexCount = VERTICES_PER_CHARACTER,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {

        fun update(letter: Letter?) {

            if (letter == null) {
                mFloatBuffer.clear()
                return
            }

            setPosition(0)

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

            setPosition(0)
        }

    }

    //endregion


    companion object {
        private const val VERTICES_PER_CHARACTER = 6
    }

}
