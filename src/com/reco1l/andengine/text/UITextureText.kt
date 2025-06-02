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


    private val textureRegions = mutableListOf<TextureRegion>()


    init {
        width = MatchContent
        height = MatchContent
    }


    fun setTextureScale(scale: Float) {
        textureScaleX = scale
        textureScaleY = scale
    }


    private fun onUpdateText() {

        var contentWidth = 0f
        var contentHeight = 0f

        textureRegions.clear()
        for (i in text.indices) {

            val textureRegion = characters[text[i]] ?: continue
            val textureWidth = textureRegion.width * textureScaleX
            val textureHeight = textureRegion.height * textureScaleY

            textureRegions.add(textureRegion)

            contentWidth += textureWidth + spacing
            contentHeight = max(contentHeight, textureHeight)
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

            val texture = textureRegions[i]
            val textureWidth = texture.width * textureScaleX
            val textureHeight = texture.height * textureScaleY

            gl.glPushMatrix()
            gl.glTranslatef(offsetX, 0f, 0f)

            onUpdateBuffer(gl, textureWidth, textureHeight)
            texture.onApply(gl)

            onDeclarePointers(gl)
            onDrawBuffer(gl)

            gl.glPopMatrix()

            offsetX += textureWidth + spacing
        }
    }

    override fun onCreateBuffer(gl: GL10): TextureTextVertexBuffer {
        return TextureTextVertexBuffer()
    }

    override fun onUpdateBuffer(gl: GL10, vararg data: Any) {
        if (data.isEmpty()) {
            super.onUpdateBuffer(gl, 0f, 0f)
        } else {
            super.onUpdateBuffer(gl, *data)
        }
    }


    inner class TextureTextVertexBuffer : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        override fun update(gl: GL10, entity: UIBufferedComponent<*>, vararg data: Any) {
            addQuad(0, 0f, 0f, data[0] as Float, data[1] as Float)
        }
    }
}