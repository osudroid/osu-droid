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
                isTextDirty = true
            }
        }

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                isTextDirty = true
            }
        }


    private val textureRegions = mutableListOf<TextureRegion>()


    private var isTextDirty = true


    override fun onManagedDraw(pGL: GL10, pCamera: Camera) {

        if (isTextDirty) {
            onUpdateText()
        }

        super.onManagedDraw(pGL, pCamera)
    }


    private fun onUpdateText() {

        isTextDirty = false

        contentWidth = 0f
        contentHeight = 0f

        textureRegions.clear()
        for (i in text.indices) {

            val textureRegion = characters[text[i]] ?: continue

            textureRegions.add(textureRegion)

            contentWidth += textureRegion.width + spacing
            contentHeight = max(contentHeight, textureRegion.height.toFloat())
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
            val textureWidth = texture.width.toFloat()
            val textureHeight = texture.height.toFloat()

            gl.glTranslatef(offsetX, 0f, 0f)

            vertexBuffer.update(textureWidth, textureHeight)
            texture.onApply(gl)

            onApplyVertices(gl)
            drawVertices(gl, camera)

            gl.glTranslatef(-offsetX, 0f, 0f)

            offsetX += textureWidth + spacing
        }
    }


    override fun getVertexBuffer(): BoxVertexBuffer {
        return super.getVertexBuffer() as BoxVertexBuffer
    }


    override fun updateVertexBuffer() = Unit

    override fun onUpdateVertexBuffer() = Unit

}