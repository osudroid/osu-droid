package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.shape.Box.BoxVertexBuffer.Companion.BOX_VERTICES
import com.reco1l.andengine.text.TextureFont.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
import kotlin.math.*

open class TextureFont(val characters: MutableMap<Char, TextureRegion>) : BufferedEntity<TextureTextVertexBuffer>(TextureTextVertexBuffer()) {

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
        width = FitContent
        height = FitContent
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

    override fun onUpdateBuffer(gl: GL10, vararg data: Any) {
        if (data.isEmpty()) {
            super.onUpdateBuffer(gl, 0f, 0f)
        } else {
            super.onUpdateBuffer(gl, *data)
        }
    }


    class TextureTextVertexBuffer : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = BOX_VERTICES,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        override fun update(gl: GL10, entity: BufferedEntity<*>, vararg data: Any) {

            val width = data[0] as Float
            val height = data[1] as Float

            putVertex(0, 0f, 0f)
            putVertex(1, 0f, height)
            putVertex(2, width, 0f)
            putVertex(3, width, height)
        }
    }
}