package com.reco1l.andengine.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.opengl.GLUtils
import androidx.core.graphics.alpha
import com.reco1l.framework.ColorARGB
import org.anddev.andengine.opengl.texture.Texture
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.region.TextureRegion
import javax.microedition.khronos.opengles.GL10


class TextTexture : Texture(PixelFormat.RGBA_8888, TextureOptions.BILINEAR_PREMULTIPLYALPHA, null) {


    private val paint = Paint()

    private val backgroundPaint = Paint()

    private val strokePaint = Paint()

    private val bounds = Rect()

    private val canvas = Canvas()


    private var text: String = ""


    fun update(
        newText: String,
        color: ColorARGB,
        backgroundColor: ColorARGB,
        strokeColor: ColorARGB,
        strokeWidth: Float,
        size: Float,
        typeFace: Typeface,
        shader: Shader?
    ) {
        paint.isAntiAlias = true
        paint.color = color.toInt()
        paint.shader = shader
        paint.textSize = size
        paint.typeface = typeFace

        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = backgroundColor.toInt()

        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = strokeColor.toInt()
        strokePaint.isAntiAlias = true
        strokePaint.textSize = size
        strokePaint.typeface = typeFace
        strokePaint.strokeWidth = strokeWidth

        paint.getTextBounds(newText, 0, newText.length, bounds)

        text = newText
        isUpdateOnHardwareNeeded = true
    }


    override fun writeTextureToHardware(pGL: GL10) {

        if (width == 0 || height == 0) {
            return
        }

        val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)

        val offsetX = -bounds.left.toFloat()
        val offsetY = -bounds.top.toFloat()

        if (backgroundPaint.color.alpha > 0f) {
            canvas.drawRect(0f, 0f, bounds.width().toFloat(), bounds.height().toFloat(), backgroundPaint)
        }

        canvas.drawText(text, offsetX, offsetY, paint)

        if (strokePaint.strokeWidth > 0 && strokePaint.color.alpha > 0) {
            canvas.drawText(text, offsetX, offsetY, strokePaint)
        }

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
    }


    override fun getWidth(): Int {
        return bounds.width()
    }

    override fun getHeight(): Int {
        return bounds.height()
    }

}

class TextTextureRegion(private val textTexture: TextTexture): TextureRegion(textTexture, 0, 0, textTexture.width, textTexture.height) {


    fun update(
        text: String,
        color: ColorARGB,
        backgroundColor: ColorARGB,
        strokeColor: ColorARGB,
        strokeWidth: Float,
        size: Float,
        typeFace: Typeface,
        shader: Shader?
    ) {
        textTexture.update(text, color, backgroundColor, strokeColor, strokeWidth, size, typeFace, shader)
        mWidth = textTexture.width
        mHeight = textTexture.height
        updateTextureRegionBuffer()
    }

}