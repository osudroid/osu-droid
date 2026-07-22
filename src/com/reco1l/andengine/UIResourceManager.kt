@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import com.reco1l.andengine.component.UIComponent
import org.anddev.andengine.opengl.font.Font
import org.anddev.andengine.opengl.texture.ITexture
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

        Log.i("UIResourceManager", "Loading font: $fontIdentifier with texture size ${GLHelper.GlMaxTextureWidth / 2}x${GLHelper.GlMaxTextureWidth / 2}")

        val typeface = Typeface.createFromAsset(context.assets, "fonts/${family}")

        val pageFactory = Supplier<ITexture> {
            val sideLength = GLHelper.GlMaxTextureWidth / 2
            val page = BitmapTextureAtlas(sideLength, sideLength, TextureOptions.BILINEAR_PREMULTIPLYALPHA)

            UIEngine.current.textureManager.loadTexture(page)
            page
        }

        val font = Font(pageFactory, typeface, size, true, Color.WHITE)

        UIEngine.current.apply {
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