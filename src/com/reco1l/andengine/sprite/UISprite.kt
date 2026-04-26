package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.sprite.UISprite.*
import com.reco1l.andengine.sprite.ScaleType.*
import com.reco1l.framework.math.*
import org.andengine.opengl.shader.PositionTextureCoordinatesUniformColorShaderProgram
import org.andengine.opengl.shader.constants.ShaderProgramConstants
import org.andengine.opengl.texture.region.*
import org.andengine.opengl.util.*
import android.opengl.GLES20
import kotlin.math.*

/**
 * Sprite that allows to change texture once created.
 */
@Suppress("LeakingThis")
open class UISprite(textureRegion: TextureRegion? = null) : UIBufferedComponent<SpriteVBO>() {

    override var contentWidth: Float
        get() = textureRegion?.width?.toFloat() ?: 0f
        set(_) = Unit

    override var contentHeight: Float
        get() = textureRegion?.height?.toFloat() ?: 0f
        set(_) = Unit


    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false
        set(value) {
            field = value
            requestBufferUpdate()
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value) {
            field = value
            requestBufferUpdate()
        }

    /**
     * The texture region of the sprite.
     */
    var textureRegion = textureRegion
        set(value) {
            if (field != value) {
                field = value
                onTextureRegionChanged()
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The X position of the texture.
     */
    var textureX = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(value.toFloat(), textureY.toFloat())
            }
        }

    /**
     * The Y position of the texture.
     */
    var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(textureX.toFloat(), value.toFloat())
            }
        }

    /**
     * The scale type of the sprite.
     */
    open var scaleType: ScaleType = Fit
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }

    /**
     * The alignment of the texture.
     *
     * If the scale type is [ScaleType.Stretch] it will not take effect.
     */
    var gravity: Vec2 = Anchor.Center
        set(value) {
            if (field != value) {
                field = value
                requestBufferUpdate()
            }
        }


    /**
     * The UV coordinate buffer for this sprite (attribute 3).
     * Managed separately from the position buffer so it can be per-instance when position is shared.
     * Protected so subclasses (e.g. UIVideoSprite) can call uvBuffer.beginDraw() with a custom shader.
     */
    protected val uvBuffer = SpriteUVBuffer()


    init {
        width = MatchContent
        height = MatchContent

        onTextureRegionChanged()
    }



    open fun onTextureRegionChanged() {
        val textureRegion = textureRegion ?: return
        textureRegion.setTexturePosition(textureX.toFloat(), textureY.toFloat())
        blendInfo = if (textureRegion.texture.textureOptions.mPreMultiplyAlpha) BlendInfo.PreMultiply else BlendInfo.Mixture
        uvBuffer.update(this)
        requestBufferUpdate()
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        requestBufferUpdate()
    }


    override fun onCreateBuffer(): SpriteVBO {
        return buffer ?: SpriteVBO()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
        uvBuffer.update(this)
    }


    override fun beginDraw(pGLState: GLState) {
        super.beginDraw(pGLState)
    }

    override fun onBindShader(pGLState: GLState) {
        val shader = PositionTextureCoordinatesUniformColorShaderProgram.getInstance()
        shader.bindProgram(pGLState)

        // Upload MVP matrix
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformModelViewPositionMatrixLocation >= 0) {
            GLES20.glUniformMatrix4fv(
                PositionTextureCoordinatesUniformColorShaderProgram.sUniformModelViewPositionMatrixLocation,
                1, false, pGLState.modelViewProjectionGLMatrix, 0
            )
        }

        // Upload texture unit sampler
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformTexture0Location >= 0) {
            GLES20.glUniform1i(PositionTextureCoordinatesUniformColorShaderProgram.sUniformTexture0Location, 0)
        }

        // Upload color uniform
        if (PositionTextureCoordinatesUniformColorShaderProgram.sUniformColorLocation >= 0) {
            GLES20.glUniform4f(
                PositionTextureCoordinatesUniformColorShaderProgram.sUniformColorLocation,
                drawRed, drawGreen, drawBlue, drawAlpha
            )
        }

        // Disable color vertex attribute (this shader uses uniform color)
        GLES20.glDisableVertexAttribArray(ShaderProgramConstants.ATTRIBUTE_COLOR_LOCATION)

        // Set up UV coordinates at attribute 3
        uvBuffer.beginDraw(pGLState)
    }

    override fun onDrawBuffer(pGLState: GLState) {
        textureRegion?.texture?.bind(pGLState)
        super.onDrawBuffer(pGLState)
    }


    class SpriteVBO : VertexBuffer(
        drawTopology = GL_TRIANGLE_STRIP,
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        fun update(entity: UISprite) {
            val textureWidth = entity.contentWidth
            val textureHeight = entity.contentHeight

            var quadWidth = textureWidth
            var quadHeight = textureHeight

            when (entity.scaleType) {

                Crop -> {
                    val scale = max(entity.width / textureWidth, entity.height / textureHeight)
                    quadWidth = textureWidth * scale
                    quadHeight = textureHeight * scale
                }

                Fit -> {
                    val scale = min(entity.width / textureWidth, entity.height / textureHeight)
                    quadWidth = textureWidth * scale
                    quadHeight = textureHeight * scale
                }

                Stretch -> Unit
            }

            val x = (entity.width - quadWidth) * entity.gravity.x
            val y = (entity.height - quadHeight) * entity.gravity.y

            addQuad(0, x, y, x + quadWidth, y + quadHeight)
        }
    }

    /**
     * UV coordinate buffer for the sprite (maps texture region to quad vertices).
     * Vertex order matches [SpriteVBO]: top-left, bottom-left, top-right, bottom-right (GL_TRIANGLE_STRIP).
     */
    class SpriteUVBuffer : TextureCoordinatesBuffer(
        vertexCount = 4,
        vertexSize = VERTEX_2D,
        bufferUsage = GL_STATIC_DRAW
    ) {
        fun update(entity: UISprite) {
            val region = entity.textureRegion
            val u = region?.u ?: 0f
            val v = region?.v ?: 0f
            val u2 = region?.u2 ?: 0f
            val v2 = region?.v2 ?: 0f

            // Vertex order: top-left, bottom-left, top-right, bottom-right
            if (entity.flippedHorizontal && entity.flippedVertical) {
                putVertex(0, u2, v2)
                putVertex(1, u2, v)
                putVertex(2, u, v2)
                putVertex(3, u, v)
            } else if (entity.flippedHorizontal) {
                putVertex(0, u2, v)
                putVertex(1, u2, v2)
                putVertex(2, u, v)
                putVertex(3, u, v2)
            } else if (entity.flippedVertical) {
                putVertex(0, u, v2)
                putVertex(1, u, v)
                putVertex(2, u2, v2)
                putVertex(3, u2, v)
            } else {
                putVertex(0, u, v)
                putVertex(1, u, v2)
                putVertex(2, u2, v)
                putVertex(3, u2, v2)
            }
            invalidateOnHardware()
        }
    }

}

enum class ScaleType {

    /**
     * Scale the texture to fill the entire sprite cropping the excess.
     */
    Crop,

    /**
     * Scale the texture to fit the sprite without cropping.
     */
    Fit,

    /**
     * Scale the texture to fit the sprite without cropping.
     * The texture will be stretched to fill the entire sprite.
     */
    Stretch
}