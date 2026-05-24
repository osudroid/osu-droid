package com.reco1l.andengine.text

import android.opengl.GLES32
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.text.UITextureText.*
import org.andengine.engine.camera.*
import org.andengine.opengl.shader.PositionTextureCoordinatesUniformColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.*
import org.andengine.opengl.util.GLState
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

    /**
     * VBO holding full-range UV coordinates: (0,0), (0,1), (1,0), (1,1).
     * Each glyph texture is its own atlas page, so full-range UVs are always correct.
     */
    private val fullUVBuffer = FullUVBuffer()


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
            val cellWidth = (fixedCharWidths?.get(char) ?: textureRegion.width) * textureScaleX

            contentWidth += cellWidth + spacing
            contentHeight = max(contentHeight, textureRegion.height * textureScaleY)
        }

        contentWidth -= spacing

        super.contentWidth = contentWidth
        super.contentHeight = contentHeight
    }

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        beginDraw(pGLState)

        var offsetX = 0f

        for (i in textureRegions.indices) {

            val (char, texture) = textureRegions[i]
            val textureWidth = texture.width * textureScaleX
            val textureHeight = texture.height * textureScaleY
            val cellWidth = (fixedCharWidths?.get(char) ?: texture.width.toFloat()) * textureScaleX

            pGLState.pushModelViewGLMatrix()
            pGLState.translateModelViewGLMatrixf(offsetX + (cellWidth - textureWidth) / 2f, 0f, 0f)

            // Update position quad for this character and re-upload to GPU.
            buffer?.update(textureWidth, textureHeight)
            buffer?.invalidateOnHardware()
            buffer?.beginDraw(pGLState)

            texture.texture.bind(pGLState)

            onDeclarePointers(pGLState)
            onDrawBuffer(pGLState)

            pGLState.popModelViewGLMatrix()

            offsetX += cellWidth + spacing
        }

        // Restore vertex attribute array state so old AndEngine Sprite rendering is not broken.
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES32.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

        // Reset GL buffer binding so legacy VBO binding is not tricked by a stale
        // cache entry left over from Buffer.bindAndUpload(), which calls glBindBuffer
        // directly without going through GLState.  Without this, the first legacy sprite
        // drawn after any UITextureText will silently skip its own bind call and render
        // from the wrong buffer (causing corrupted geometry / wrong draw order).
        pGLState.bindArrayBuffer(0)
    }

    override fun onCreateBuffer(): TextureTextVertexBuffer {
        return TextureTextVertexBuffer()
    }

    override fun onUpdateBuffer() {
        // Nothing to do here, buffer is updated in `doDraw`.
    }

    override fun onBindShader(pGLState: GLState) {
        val shader = PositionTextureCoordinatesUniformColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        // Upload texture unit
        if (shader.uniformTexture0Location >= 0) {
            GLES32.glUniform1i(shader.uniformTexture0Location, 0)
        }

        // Upload color
        if (shader.uniformColorLocation >= 0) {
            GLES32.glUniform4f(
                shader.uniformColorLocation,
                drawRed, drawGreen, drawBlue, drawAlpha
            )
        }

        // Disable per-vertex color array
        GLES32.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        // Set up full-range UV VBO at attribute 3
        fullUVBuffer.beginDraw(pGLState)
    }

    override fun onDeclarePointers(gl: GLState) {
        super.onDeclarePointers(gl)
        // Re-upload MVP matrix per character since each character is translated differently.
        val shader = PositionTextureCoordinatesUniformColorShaderProgram.getInstance()
        if (shader.uniformMVPMatrixLocation >= 0) {
            GLES32.glUniformMatrix4fv(
                shader.uniformMVPMatrixLocation,
                1, false, gl.modelViewProjectionGLMatrix, 0
            )
        }
    }

    inner class TextureTextVertexBuffer : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_DYNAMIC_DRAW
    ) {
        fun update(textureWidth: Float, textureHeight: Float) {
            addQuad(0, 0f, 0f, textureWidth, textureHeight)
        }
    }

    /**
     * UV buffer covering the full texture (0,0)→(1,1).
     * Vertex order: top-left, bottom-left, top-right, bottom-right (GL_TRIANGLE_STRIP).
     */
    inner class FullUVBuffer : TextureCoordinatesBuffer(
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        init {
            putVertex(0, 0f, 0f)  // top-left
            putVertex(1, 0f, 1f)  // bottom-left
            putVertex(2, 1f, 0f)  // top-right
            putVertex(3, 1f, 1f)  // bottom-right
            invalidateOnHardware()
        }
    }
}
