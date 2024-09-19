package com.reco1l.osu.graphics

import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.shape.*
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
            field = value

            if (value) {
                setSize(textureRegion?.width?.toFloat() ?: 0f, textureRegion?.height?.toFloat() ?: 0f)
                updateVertexBuffer()
            }
        }

    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal
        get() = textureRegion?.isFlippedHorizontal ?: false
        set(value) {
            textureRegion?.isFlippedHorizontal = value
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical
        get() = textureRegion?.isFlippedVertical ?: false
        set(value) {
            textureRegion?.isFlippedVertical = value
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

            if (adjustSizeWithTexture) {
                setSize(textureRegion?.width?.toFloat() ?: 0f, textureRegion?.height?.toFloat() ?: 0f)
                updateVertexBuffer()
            }
        }


    private fun applyBlendFunction() {

        if (textureRegion?.texture?.textureOptions?.mPreMultipyAlpha == true) {
            setBlendFunction(Shape.BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT)
        } else {
            setBlendFunction(Shape.BLENDFUNCTION_SOURCE_DEFAULT, Shape.BLENDFUNCTION_DESTINATION_DEFAULT)
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