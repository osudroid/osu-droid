package com.reco1l.andengine.shape

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.sprite.ScaleType
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.framework.Color4
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.region.TextureRegion
import kotlin.math.cos
import kotlin.math.sin

/**
 * A box component that displays a gradient using a dynamically generated texture.
 * The texture automatically stretches to fill the component size.
 */
open class UIGradientBox : UISprite() {

    override var scaleType = ScaleType.Stretch
        set(_) {
            Log.w("UIGradientBox", "ScaleType is fixed to Stretch and cannot be changed.")
        }

    /**
     * The angle of the gradient in degrees.
     * - 0° = left to right (horizontal)
     * - 90° = top to bottom (vertical)
     * - 180° = right to left
     * - 270° = bottom to top
     */
    var gradientAngle = 90f
        set(value) {
            if (field != value) {
                field = value
                regenerateTexture()
            }
        }

    /**
     * The start color of the gradient.
     */
    var colorStart = Color4.White
        set(value) {
            if (field != value) {
                field = value
                regenerateTexture()
            }
        }

    /**
     * The end color of the gradient.
     */
    var colorEnd = Color4.Black
        set(value) {
            if (field != value) {
                field = value
                regenerateTexture()
            }
        }

    private var currentTextureAtlas: BitmapTextureAtlas? = null


    init {
        regenerateTexture()
    }


    private fun regenerateTexture() {
        currentTextureAtlas?.let {
            UIEngine.current.textureManager.unloadTexture(it)
        }

        val textureSize = 256

        val textureAtlas = BitmapTextureAtlas(textureSize, textureSize, TextureOptions.BILINEAR)
        val bitmapSource = GradientBitmapSource(textureSize, colorStart, colorEnd, gradientAngle)

        textureAtlas.addTextureAtlasSource(bitmapSource, 0, 0)

        val region = TextureRegion(textureAtlas, 0, 0, textureSize, textureSize)

        UIEngine.current.textureManager.loadTexture(textureAtlas)

        currentTextureAtlas = textureAtlas
        textureRegion = region
    }

    /**
     * A bitmap source that generates a gradient.
     */
    private class GradientBitmapSource(
        private val size: Int,
        private val colorStart: Color4,
        private val colorEnd: Color4,
        private val angle: Float
    ) : IBitmapTextureAtlasSource {

        override fun getTexturePositionX() = 0
        override fun getTexturePositionY() = 0
        override fun setTexturePositionX(pTexturePositionX: Int) = Unit
        override fun setTexturePositionY(pTexturePositionY: Int) = Unit

        override fun getWidth() = size
        override fun getHeight() = size

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config?): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val angleRad = Math.toRadians(angle.toDouble())
            val centerX = size / 2f
            val centerY = size / 2f
            val radius = size / 2f

            val x0 = centerX - cos(angleRad).toFloat() * radius
            val y0 = centerY - sin(angleRad).toFloat() * radius
            val x1 = centerX + cos(angleRad).toFloat() * radius
            val y1 = centerY + sin(angleRad).toFloat() * radius

            val gradient = LinearGradient(
                x0, y0, x1, y1,
                colorStart.toInt(),
                colorEnd.toInt(),
                Shader.TileMode.CLAMP
            )

            val paint = Paint()
            paint.shader = gradient

            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

            return bitmap
        }

        override fun deepCopy(): IBitmapTextureAtlasSource {
            return GradientBitmapSource(size, colorStart, colorEnd, angle)
        }
    }
}
