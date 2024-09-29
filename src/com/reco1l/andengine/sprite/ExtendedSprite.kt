package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*


/**
 * Sprite that allows to change texture once created.
 */
open class ExtendedSprite(textureRegion: TextureRegion? = null) : ExtendedEntity(vertexBuffer = RectangleVertexBuffer(GL11.GL_STATIC_DRAW, true)) {


    override var autoSizeAxes = Axes.Both
        set(value) {
            if (field != value) {
                field = value

                if (autoSizeAxes != Axes.None) {
                    val textureWidth = textureRegion?.width?.toFloat() ?: 0f
                    val textureHeight = textureRegion?.height?.toFloat() ?: 0f

                    var updateBuffer = false

                    if (textureWidth != internalWidth && (autoSizeAxes == Axes.X || autoSizeAxes == Axes.Both)) {
                        internalWidth = textureWidth
                        updateBuffer = true
                    }

                    if (textureHeight != internalHeight && (autoSizeAxes == Axes.Y || autoSizeAxes == Axes.Both)) {
                        internalHeight = textureHeight
                        updateBuffer = true
                    }

                    if (updateBuffer) {
                        updateVertexBuffer()
                    }

                    if (parent is Container) {
                        (parent as Container).onChildSizeChanged(this)
                    }
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

            if (value != null) {
                value.isFlippedVertical = flippedVertical
                value.isFlippedHorizontal = flippedHorizontal
            }

            if (autoSizeAxes != Axes.None) {
                val textureWidth = textureRegion?.width?.toFloat() ?: 0f
                val textureHeight = textureRegion?.height?.toFloat() ?: 0f

                var updateBuffer = false

                if (textureWidth != internalWidth && (autoSizeAxes == Axes.X || autoSizeAxes == Axes.Both)) {
                    internalWidth = textureWidth
                    updateBuffer = true
                }

                if (textureHeight != internalHeight && (autoSizeAxes == Axes.Y || autoSizeAxes == Axes.Both)) {
                    internalHeight = textureHeight
                    updateBuffer = true
                }

                if (updateBuffer) {
                    updateVertexBuffer()
                }

                if (parent is Container) {
                    (parent as Container).onChildSizeChanged(this)
                }
            }
        }


    init {
        this.textureRegion = textureRegion
    }


    private fun applyBlendFunction() {

        if (textureRegion?.texture?.textureOptions?.mPreMultipyAlpha == true) {
            setBlendFunction(BLENDFUNCTION_SOURCE_PREMULTIPLYALPHA_DEFAULT, BLENDFUNCTION_DESTINATION_PREMULTIPLYALPHA_DEFAULT)
        } else {
            setBlendFunction(BLENDFUNCTION_SOURCE_DEFAULT, BLENDFUNCTION_DESTINATION_DEFAULT)
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

    override fun doDraw(pGL: GL10, pCamera: Camera) {
        textureRegion?.onApply(pGL)
        super.doDraw(pGL, pCamera)
    }

    override fun reset() {
        super.reset()
        applyBlendFunction()
    }
}