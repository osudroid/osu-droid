package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*

/**
 * Sprite that allows to change texture once created.
 */
open class ExtendedSprite(textureRegion: TextureRegion? = null) : Rectangle() {


    override var autoSizeAxes = Axes.Both
        set(value) {
            if (field != value) {
                field = value

                onApplyInternalSize(
                    textureRegion?.width?.toFloat() ?: 0f,
                    textureRegion?.height?.toFloat() ?: 0f
                )
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
    open var textureRegion: TextureRegion? = null
        set(value) {

            if (field == value) {
                return
            }

            field = value

            value?.setTexturePosition(textureX, textureY)
            value?.isFlippedVertical = flippedVertical
            value?.isFlippedHorizontal = flippedHorizontal

            onApplyInternalSize(
                value?.width?.toFloat() ?: 0f,
                value?.height?.toFloat() ?: 0f
            )
        }

    /**
     * The X position of the texture.
     */
    open var textureX = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(value, textureY)
            }
        }

    /**
     * The Y position of the texture.
     */
    open var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(textureX, value)
            }
        }


    init {
        @Suppress("LeakingThis")
        this.textureRegion = textureRegion
    }


    override fun applyBlending(pGL: GL10) {
        if (textureRegion?.texture?.textureOptions?.mPreMultipyAlpha == true) {
            GLHelper.blendFunction(pGL, BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT)
        } else {
            super.applyBlending(pGL)
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

    override fun onApplyVertices(pGL: GL10) {
        super.onApplyVertices(pGL)
        textureRegion?.onApply(pGL)
    }

}