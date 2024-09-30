package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*


/**
 * Sprite that allows to change texture once created.
 */
open class ExtendedSprite(textureRegion: TextureRegion? = null) : ExtendedEntity(vertexBuffer = RectangleVertexBuffer(GL11.GL_STATIC_DRAW, true)) {


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

    override var translationX = 0f
        set(value) {
            if (field != value) {
                field = value
                applyTextureTranslation()
            }
        }

    override var translationY = 0f
        set(value) {
            if (field != value) {
                field = value
                applyTextureTranslation()
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
            applyBlendFunction()
            applyTextureTranslation()

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
                applyTextureTranslation()
            }
        }

    /**
     * The Y position of the texture.
     */
    open var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                applyTextureTranslation()
            }
        }

    /**
     * The portion of the texture to display on the X axis in a range of -1 to 1.
     *
     * Setting a portion that is not 1 will crop the texture and override [translationX]
     * value. Additionally negative values will crop the texture from the left.
     */
    open var portionX = 1f
        set(value) {
            val coerced = value.coerceIn(-1f, 1f)
            if (field != coerced) {
                field = coerced
                applyTextureTranslation()
            }
        }

    /**
     * The portion of the texture to display on the Y axis in a range of -1 to 1.
     *
     * Setting a portion that is not 1 will crop the texture and override [translationY]
     * value. Additionally negative values will crop the texture from the top.
     */
    open var portionY = 1f
        set(value) {
            val coerced = value.coerceIn(-1f, 1f)
            if (field != coerced) {
                field = coerced
                applyTextureTranslation()
            }
        }


    init {
        @Suppress("LeakingThis")
        this.textureRegion = textureRegion
    }


    private fun applyBlendFunction() {

        if (textureRegion?.texture?.textureOptions?.mPreMultipyAlpha == true) {
            setBlendFunction(BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT)
        } else {
            setBlendFunction(BLENDFUNCTION_SOURCE_DEFAULT, BLENDFUNCTION_DESTINATION_DEFAULT)
        }
    }

    private fun applyTextureTranslation() {

        if (portionX == 1f && portionY == 1f) {
            textureRegion?.setTexturePosition(textureX, textureY)
            return
        }

        val texture = textureRegion ?: return

        val offsetX = texture.width * (1f - abs(portionX)) * sign(portionX)
        val offsetY = texture.height * (1f - abs(portionY)) * sign(portionY)

        texture.setTexturePosition(
            (textureX - offsetX).toInt(),
            (textureY - offsetY).toInt(),
        )

        this.translationX = -offsetX
        this.translationY = -offsetY
    }


    override fun onUpdateVertexBuffer() {
        (vertexBuffer as RectangleVertexBuffer).update(width, height)
    }

    override fun onInitDraw(pGL: GL10) {
        super.onInitDraw(pGL)
        GLHelper.enableTextures(pGL)
        GLHelper.enableTexCoordArray(pGL)
    }

    override fun doDraw(pGL: GL10, pCamera: Camera) {
        textureRegion?.onApply(pGL)
        super.doDraw(pGL, pCamera)
    }

    override fun reset() {
        super.reset()
        applyBlendFunction()
    }
}