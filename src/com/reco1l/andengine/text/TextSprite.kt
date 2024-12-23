package com.reco1l.andengine.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.graphics.Typeface
import android.opengl.GLUtils
import android.util.Log
import androidx.core.graphics.alpha
import com.reco1l.andengine.Axes
import com.reco1l.andengine.getDrawHeight
import com.reco1l.andengine.getDrawWidth
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.framework.ColorARGB
import org.anddev.andengine.opengl.texture.Texture
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.GlobalManager
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

/**
 * A sprite that displays text.
 *
 * Differently from the original [Text] class, this is a sprite that pre-renders the entire text
 * to a texture, it is not as efficient as the original [Text] class, but it is more flexible and
 * allows for more customization.
 *
 * It is not recommended to use this on places where the text changes frequently, as it will
 * generate a new texture every time the text changes.
 */
class TextSprite : ExtendedSprite() {


    override var autoSizeAxes = Axes.Both

    override var textureRegion: TextureRegion? = TextTextureRegion(TextTexture())
        set(_) {
            Log.e("ExtendedText", "textureRegion is read-only for ExtendedText")
        }


    /**
     * The text to be displayed.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The color of the background of the text.
     */
    var backgroundColor = ColorARGB.Transparent
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The color of the text stroke.
     */
    var strokeColor = ColorARGB.Transparent
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The width of the text stroke.
     */
    var strokeWidth = 0f
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The size of the text in pixels.
     */
    var size = 12f
        get() {
            if (relativeSize && parent != null) {
                return field * max(parent.getDrawWidth(), parent.getDrawHeight())
            }
            return field
        }
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * Whether the size of the text is relative to the size of the parent.
     */
    var relativeSize = false
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The typeface of the text.
     */
    var typeFace = Typeface.DEFAULT
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }

    /**
     * The style of the text.
     */
    var typeStyle = Typeface.NORMAL
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    private var shader: Shader? = null


    private val texture = textureRegion!!.texture as TextTexture


    init {
        GlobalManager.getInstance().engine.textureManager.loadTexture(texture)
    }


    override fun onUpdateVertexBuffer() {

        (textureRegion as TextTextureRegion).update(
            text,
            color,
            backgroundColor,
            strokeColor,
            strokeWidth,
            size,
            Typeface.create(typeFace, typeStyle),
            shader
        )

        onContentSizeMeasured()
        super.onUpdateVertexBuffer()
    }


    // Shaders

    /**
     * Sets a linear gradient color to the text.
     *
     * The gradient is defined by two points (x0, y0) and (x1, y1) in the range [0, 1] and two colors (startColor, endColor).
     */
    fun setLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float, startColor: ColorARGB, endColor: ColorARGB, tileMode: TileMode = TileMode.MIRROR) {
        shader = RadialGradient(
            x0 * drawWidth,
            y0 * drawHeight,
            max(drawWidth, drawHeight),
            startColor.toInt(),
            endColor.toInt(),
            tileMode
        )
        shader = LinearGradient(
            x0 * drawWidth,
            y0 * drawHeight,
            x1 * drawWidth,
            y1 * drawHeight,
            startColor.toInt(),
            endColor.toInt(),
            tileMode
        )
        updateVertexBuffer()
    }


    override fun finalize() {
        GlobalManager.getInstance().engine.textureManager.unloadTexture(texture)
        super.finalize()
    }

}

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
        paint.shader = null
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