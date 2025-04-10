package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.info.*
import com.reco1l.andengine.shape.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*

/**
 * Sprite that allows to change texture once created.
 */
open class ExtendedSprite(textureRegion: TextureRegion? = null) : Box() {

    override var contentWidth: Float
        get() = textureRegion?.width?.toFloat() ?: 0f
        set(_) = Unit

    override var contentHeight: Float
        get() = textureRegion?.height?.toFloat() ?: 0f
        set(_) = Unit


    /**
     * Whether the texture should be flipped horizontally.
     */
    open var flippedHorizontal = false
        set(value) {
            field = value
            textureRegion?.isFlippedHorizontal = value
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    open var flippedVertical = false
        set(value) {
            field = value
            textureRegion?.isFlippedVertical = value
        }

    /**
     * The texture region of the sprite.
     */
    open var textureRegion = textureRegion
        set(value) {
            if (field != value) {
                field = value
                onTextureRegionChanged()
                invalidate(InvalidationFlag.ContentSize)
            }
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
        width = FitContent
        height = FitContent

        @Suppress("LeakingThis")
        onTextureRegionChanged()
    }


    open fun onTextureRegionChanged() {

        val textureRegion = textureRegion ?: return

        textureRegion.setTexturePosition(textureX, textureY)
        textureRegion.isFlippedVertical = flippedVertical
        textureRegion.isFlippedHorizontal = flippedHorizontal

        blendInfo = if (textureRegion.texture.textureOptions.mPreMultipyAlpha) BlendInfo.PreMultiply else BlendInfo.Mixture
    }


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)
        GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
    }

    override fun onDrawBuffer(gl: GL10) {
        textureRegion?.onApply(gl)
        super.onDrawBuffer(gl)
    }

}