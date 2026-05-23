package com.reco1l.andengine.text

import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.text.UITextureText.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

/**
 * A text component that uses textures for each character.
 */
open class UITextureText(val characters: MutableMap<Char, TextureRegion>) : UIBufferedComponent<TextureTextVertexBuffer>() {

    /**
     * The spacing between glyphs.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The scale of the textures on the x-axis.
     */
    var textureScaleX = 1f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The scale of the textures on the y-axis.
     */
    var textureScaleY = 1f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * When set, each character is placed in a fixed-width cell (unscaled pixels) looked up by
     * character. Characters absent from the map use their natural texture width. Useful for
     * preventing layout shifts when glyphs in the same role have varying widths.
     */
    var fixedCharWidths: Map<Char, Float>? = null
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * When set, content size (width/height) is measured from this string instead of [text].
     * The rendered text is still [text]. Use this to give the component a stable bounding box
     * sized for the widest value it can display, so surrounding elements don't shift.
     */
    var measureText: String? = null
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }


    private val textureRegions = mutableListOf<Pair<Char, TextureRegion>>()


    init {
        width = MatchContent
        height = MatchContent
    }


    fun setTextureScale(scale: Float) {
        textureScaleX = scale
        textureScaleY = scale
    }


    private fun onUpdateText() {
        textureRegions.clear()

        for (char in text) {
            val textureRegion = characters[char] ?: continue
            textureRegions.add(char to textureRegion)
        }

        var contentWidth = 0f
        var contentHeight = 0f

        for (char in measureText ?: text) {
            val textureRegion = characters[char] ?: continue
            val cellWidth = (fixedCharWidths?.get(char) ?: textureRegion.width.toFloat()) * textureScaleX

            contentWidth += cellWidth + spacing
            contentHeight = max(contentHeight, textureRegion.height * textureScaleY)
        }

        contentWidth -= spacing

        super.contentWidth = contentWidth
        super.contentHeight = contentHeight
    }

    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        beginDraw(gl)

        var offsetX = 0f

        for (i in textureRegions.indices) {

            val (char, texture) = textureRegions[i]
            val textureWidth = texture.width * textureScaleX
            val textureHeight = texture.height * textureScaleY
            val cellWidth = (fixedCharWidths?.get(char) ?: texture.width.toFloat()) * textureScaleX

            gl.glPushMatrix()
            gl.glTranslatef(offsetX + (cellWidth - textureWidth) / 2f, 0f, 0f)

            buffer?.update(textureWidth, textureHeight)
            texture.onApply(gl)

            onDeclarePointers(gl)
            onDrawBuffer(gl)

            gl.glPopMatrix()

            offsetX += cellWidth + spacing
        }
    }

    override fun onCreateBuffer(): TextureTextVertexBuffer {
        return TextureTextVertexBuffer()
    }

    override fun onUpdateBuffer() {
        // Nothing to do here, buffer is updated in `doDraw`.
    }

    class TextureTextVertexBuffer : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_DYNAMIC_DRAW
    ) {
        fun update(textureWidth: Float, textureHeight: Float) {
            addQuad(0, 0f, 0f, textureWidth, textureHeight)
        }
    }
}
