package com.reco1l.andengine.text

import android.graphics.Shader
import android.graphics.Typeface
import android.util.Log
import com.reco1l.andengine.Axes
import com.reco1l.andengine.getDrawHeight
import com.reco1l.andengine.getDrawWidth
import com.reco1l.andengine.sprite.ExtendedSprite
import com.reco1l.framework.ColorARGB
import org.anddev.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.GlobalManager
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

    /**
     * The shader to be applied to the text.
     */
    var shader: Shader? = null
        set(value) {
            if (field != value) {
                field = value
                updateVertexBuffer()
            }
        }


    /**
     * The width of the text line.
     */
    val lineWidth
        get() = size * text.length


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


    override fun finalize() {
        GlobalManager.getInstance().engine.textureManager.unloadTexture(texture)
        super.finalize()
    }

}
