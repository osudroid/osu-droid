package com.reco1l.andengine.text

import android.opengl.GLES20
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


    private val textureRegions = mutableListOf<TextureRegion>()

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

    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        beginDraw(pGLState)

        var offsetX = 0f

        for (i in textureRegions.indices) {

            val texture = textureRegions[i]
            val textureWidth = texture.width * textureScaleX
            val textureHeight = texture.height * textureScaleY

            pGLState.pushModelViewGLMatrix()
            pGLState.translateModelViewGLMatrixf(offsetX, 0f, 0f)

            // Update position quad for this character and re-upload to GPU.
            buffer?.update(textureWidth, textureHeight)
            buffer?.invalidateOnHardware()
            buffer?.beginDraw(pGLState)

            texture.texture.bind(pGLState)

            onDeclarePointers(pGLState)
            onDrawBuffer(pGLState)

            pGLState.popModelViewGLMatrix()

            offsetX += textureWidth + spacing
        }

        // Restore vertex attribute array state so old AndEngine Sprite rendering is not broken.
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)
        GLES20.glEnableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_TEXTURECOORDINATES_LOCATION)

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
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformTexture0Location >= 0) {
            GLES20.glUniform1i(PositionTextureCoordinatesUniformColorShaderProgram.sUniformTexture0Location, 0)
        }

        // Upload color
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformColorLocation >= 0) {
            GLES20.glUniform4f(
                PositionTextureCoordinatesUniformColorShaderProgram.sUniformColorLocation,
                drawRed, drawGreen, drawBlue, drawAlpha
            )
        }

        // Disable per-vertex color array
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        // Set up full-range UV VBO at attribute 3
        fullUVBuffer.beginDraw(pGLState)
    }

    override fun onDeclarePointers(gl: GLState) {
        super.onDeclarePointers(gl)
        // Re-upload MVP matrix per character since each character is translated differently.
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformModelViewPositionMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(
                PositionTextureCoordinatesUniformColorShaderProgram.sUniformModelViewPositionMatrixLocation,
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