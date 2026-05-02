@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLES20
import android.util.Log
import com.reco1l.andengine.component.UIComponent
import org.andengine.opengl.font.Font
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import java.lang.ref.WeakReference

class UIResourceManager(private val context: Context) {

    private val fonts = mutableMapOf<String, Font>()
    private val fontSubscribers = mutableMapOf<Font, MutableList<WeakReference<UIComponent>>>()


    fun getOrStoreFont(size: Float, family: String): Font {

        val fontIdentifier = "${family}-${size}"

        val fetchedFont = fonts[fontIdentifier]
        if (fetchedFont != null) {
            return fetchedFont
        }

        val engine = UIEngine.current
        val buf = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, buf, 0)
        val maxTextureSize = buf[0].coerceAtLeast(256)

        Log.i("UIResourceManager", "Loading font: $fontIdentifier with texture size ${maxTextureSize}x${maxTextureSize}")

        val texture = BitmapTextureAtlas(
            engine.textureManager,
            maxTextureSize,
            maxTextureSize,
            TextureOptions.BILINEAR_PREMULTIPLYALPHA
        )
        val typeface = Typeface.createFromAsset(context.assets, "fonts/${family}")
        val font = Font(engine.fontManager, texture, typeface, size, true, Color.WHITE)

        engine.apply {
            textureManager.loadTexture(texture)
            fontManager.loadFont(font)

            fonts[fontIdentifier] = font
        }

        return font
    }

    fun subscribeToFont(font: Font, component: UIComponent) {
        val subscribers = fontSubscribers.getOrPut(font) { mutableListOf() }
        if (subscribers.none { it.get() === component }) {
            subscribers.add(WeakReference(component))
        }
    }

    fun unsubscribeFromFont(font: Font, component: UIComponent) {

        val subscribers = fontSubscribers[font] ?: return
        subscribers.removeAll { it.get() === component || it.get() == null }

        if (subscribers.isEmpty()) {
            val fontKey = fonts.entries.find { it.value == font }?.key
            Log.i("UIResourceManager", "Unloading font: $fontKey")

            fonts.remove(fontKey)
            fontSubscribers.remove(font)

            UIEngine.current.apply {
                fontManager.unloadFont(font)
                textureManager.unloadTexture(font.texture)
            }
        }
    }

}