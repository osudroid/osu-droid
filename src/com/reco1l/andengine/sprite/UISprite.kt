package com.reco1l.andengine.sprite

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.sprite.UISprite.*
import com.reco1l.andengine.sprite.ScaleType.*
import com.reco1l.framework.math.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL11.*
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
            textureRegion?.isFlippedHorizontal = value
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value) {
            field = value
            textureRegion?.isFlippedVertical = value
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
                textureRegion?.setTexturePosition(value, textureY)
            }
        }

    /**
     * The Y position of the texture.
     */
    var textureY = 0
        set(value) {
            if (field != value) {
                field = value
                textureRegion?.setTexturePosition(textureX, value)
            }
        }

    /**
     * The scale type of the sprite.
     */
    var scaleType: ScaleType = Fit
        set(value) {
            if (field != value) {
                field = value
                invalidateBuffer(BufferInvalidationFlag.Data)
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
                invalidateBuffer(BufferInvalidationFlag.Data)
            }
        }


    init {
        width = MatchContent
        height = MatchContent

        onTextureRegionChanged()
    }


    open fun onTextureRegionChanged() {

        val textureRegion = textureRegion ?: return

        textureRegion.setTexturePosition(textureX, textureY)
        textureRegion.isFlippedVertical = flippedVertical
        textureRegion.isFlippedHorizontal = flippedHorizontal

        blendInfo = if (textureRegion.texture.textureOptions.mPreMultipyAlpha) BlendInfo.PreMultiply else BlendInfo.Mixture
        invalidateBuffer(BufferInvalidationFlag.Data)
    }


    override fun onSizeChanged() {
        super.onSizeChanged()
        invalidateBuffer(BufferInvalidationFlag.Data)
    }


    override fun onCreateBuffer(): SpriteVBO {
        return buffer ?: SpriteVBO()
    }

    override fun onUpdateBuffer() {
        buffer?.update(this)
    }


    override fun beginDraw(gl: GL10) {
        super.beginDraw(gl)

        if (textureRegion == null) {
            GLHelper.disableTextures(gl)
            GLHelper.disableTexCoordArray(gl)
            return
        }

        GLHelper.enableTextures(gl)
        GLHelper.enableTexCoordArray(gl)
    }

    override fun onDrawBuffer(gl: GL10) {
        textureRegion?.onApply(gl)
        super.onDrawBuffer(gl)
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