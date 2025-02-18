package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import kotlin.math.*

open class TextureFont(private val characters: MutableMap<Char, TextureRegion>) : Box() {


    override var autoSizeAxes = Axes.Both


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


    fun setTextureScale(scale: Float) {
        textureScaleX = scale
        textureScaleY = scale
    }


    private fun onUpdateText() {

        contentWidth = 0f
        contentHeight = 0f

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
        onContentSizeMeasured()
    }

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)

        GLHelper.enableTextures(pGL)
        GLHelper.enableTexCoordArray(pGL)
    }

    override fun doDraw(gl: GL10, camera: Camera) {
        onInitDraw(gl)

        var offsetX = 0f

        for (i in textureRegions.indices) {

            val texture = textureRegions[i]
            val textureWidth = texture.width * textureScaleX
            val textureHeight = texture.height * textureScaleY

            gl.glPushMatrix()
            gl.glTranslatef(offsetX, 0f, 0f)

            vertexBuffer.update(textureWidth, textureHeight)
            texture.onApply(gl)

            onApplyVertices(gl)
            drawVertices(gl, camera)
            gl.glPopMatrix()

            offsetX += textureWidth + spacing
        }
    }


    override fun getVertexBuffer(): BoxVertexBuffer {
        return super.getVertexBuffer() as BoxVertexBuffer
    }


    override fun updateVertexBuffer() = Unit

    override fun onUpdateVertexBuffer() = Unit

}