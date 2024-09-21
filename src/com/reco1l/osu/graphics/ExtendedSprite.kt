package com.reco1l.osu.graphics

import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*


/**
 * Sprite that allows to change texture once created.
 */
open class ExtendedSprite : ExtendedEntity(vertexBuffer = RectangleVertexBuffer(GL11.GL_STATIC_DRAW, true)) {

    /**
     * Whether the size of the sprite should be adjusted to the size of the texture.
     */
    var adjustSizeWithTexture = true
        set(value) {
            if (field != value) {
                field = value

                if (value) {
                    super.setSize(textureRegion?.width?.toFloat() ?: 0f, textureRegion?.height?.toFloat() ?: 0f)
                }
            }
        }

    /**
     * Whether the texture should be flipped horizontally.
     */
    open var flippedHorizontal = false
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.isFlippedHorizontal = value
            }
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    open var flippedVertical = false
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.isFlippedVertical = value
            }
        }

    /**
     * The texture region of the sprite.
     */
    var textureRegion: TextureRegion? = null
        set(value) {

            if (field == value) {
                return
            }

            field = value
            applyBlendFunction()

            // Avoiding unnecessary buffer updates
            var wasBufferUpdated = false

            if (value == null) {
                if (adjustSizeWithTexture && (width != 0f || height != 0f)) {
                    super.setSize(0f, 0f)
                    wasBufferUpdated = true
                }
            } else {
                value.isFlippedVertical = flippedVertical
                value.isFlippedHorizontal = flippedHorizontal

                val textureWidth = value.width.toFloat()
                val textureHeight = value.height.toFloat()

                if (adjustSizeWithTexture && (width != textureWidth || height != textureHeight)) {
                    super.setSize(textureWidth, textureHeight)
                    wasBufferUpdated = true
                }
            }

            if (!wasBufferUpdated) {
                updateVertexBuffer()
            }
        }


    private fun applyBlendFunction() {

        if (textureRegion?.texture?.textureOptions?.mPreMultipyAlpha == true) {
            setBlendFunction(BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT)
        } else {
            setBlendFunction(BLENDFUNCTION_SOURCE_DEFAULT, BLENDFUNCTION_DESTINATION_DEFAULT)
        }
    }


    override fun setSize(w: Float, h: Float) {
        if (!adjustSizeWithTexture) {
            super.setSize(w, h)
        }
    }

    override fun setWidth(value: Float) {
        if (!adjustSizeWithTexture) {
            super.setWidth(value)
        }
    }

    override fun setHeight(value: Float) {
        if (!adjustSizeWithTexture) {
            super.setHeight(value)
        }
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as RectangleVertexBuffer).update(width, height)
    }

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.enableTextures(pGL)
        GLHelper.enableTexCoordArray(pGL)
    }

    override fun doDraw(pGL: GL10?, pCamera: Camera?) {
        textureRegion?.onApply(pGL)
        super.doDraw(pGL, pCamera)
    }

    override fun reset() {
        super.reset()
        applyBlendFunction()
    }
}