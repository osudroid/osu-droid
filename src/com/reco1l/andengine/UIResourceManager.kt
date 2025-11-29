@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import com.reco1l.andengine.component.UIComponent
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.util.GLHelper
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

        Log.i("UI", "Loading font: $fontIdentifier with texture size ${GLHelper.GlMaxTextureWidth / 2}x${GLHelper.GlMaxTextureWidth / 2}")

        val texture = BitmapTextureAtlas(
            GLHelper.GlMaxTextureWidth / 2,
            GLHelper.GlMaxTextureWidth / 2,
            TextureOptions.BILINEAR_PREMULTIPLYALPHA
        )
        val typeface = Typeface.createFromAsset(context.assets, "fonts/${family}")
        val font = Font(texture, typeface, size, true, Color.WHITE)

        UIEngine.current.apply {
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
            Log.i("UI", "Unloading font: $fontKey")

            fonts.remove(fontKey)
            fontSubscribers.remove(font)

            UIEngine.current.apply {
                fontManager.unloadFont(font)
                textureManager.unloadTexture(font.texture)
            }
        }
    }

}