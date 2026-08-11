package com.osudroid.resources

import android.content.Context
import android.graphics.Typeface
import org.andengine.engine.Engine
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontFactory
import org.andengine.opengl.font.StrokeFont
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import ru.nsu.ccfit.zuev.osu.Config
import java.util.concurrent.ConcurrentHashMap

/**
 * A named cache of loaded fonts, safe to read and write from any thread. See [TextureStore]'s
 * documentation for why this matters.
 */
class FontStore {

    // Set via init(), mirroring ResourceManager.Init() - see TextureStore's equivalent for why.
    private lateinit var engine: Engine
    private lateinit var context: Context

    private val fonts = ConcurrentHashMap<String, Font>()

    fun init(engine: Engine, context: Context) {
        this.engine = engine
        this.context = context
    }

    fun get(name: String): Font? = fonts[name]

    fun containsKey(name: String): Boolean = fonts.containsKey(name)

    /** Returns the cached font, lazily loading a default-styled one under [name] if absent. */
    fun getOrLoadDefault(name: String): Font? {
        if (!fonts.containsKey(name)) {
            loadFont(name, null, 35, android.graphics.Color.WHITE)
        }
        return fonts[name]
    }

    fun clear() = fonts.clear()

    fun loadFont(resname: String, file: String?, size: Int, color: Int): Font {
        val scaledSize = size / Config.getTextureQuality()
        val texture = BitmapTextureAtlas(engine.textureManager, FONT_TEXTURE_SIZE, FONT_TEXTURE_SIZE, TextureOptions.BILINEAR_PREMULTIPLYALPHA)

        val font = if (file == null) {
            Font(engine.fontManager, texture, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), scaledSize.toFloat(), true, color)
        } else {
            FontFactory.createFromAsset(engine.fontManager, texture, context.assets, "fonts/$file", scaledSize.toFloat(), true, color)
        }

        engine.textureManager.loadTexture(texture)
        engine.fontManager.loadFont(font)
        fonts[resname] = font
        return font
    }

    fun loadStrokeFont(resname: String, file: String?, size: Int, color1: Int, color2: Int): StrokeFont {
        val scaledSize = size / Config.getTextureQuality()
        val texture = BitmapTextureAtlas(engine.textureManager, STROKE_FONT_TEXTURE_WIDTH, STROKE_FONT_TEXTURE_HEIGHT, TextureOptions.BILINEAR_PREMULTIPLYALPHA)

        val font = if (file == null) {
            StrokeFont(
                engine.fontManager, texture, Typeface.create(Typeface.DEFAULT, Typeface.NORMAL), scaledSize.toFloat(), true, color1,
                if (Config.getTextureQuality() == 1) 2f else 0.75f, color2,
            )
        } else {
            FontFactory.createStrokeFromAsset(
                engine.fontManager, texture, context.assets, "fonts/$file", scaledSize.toFloat(), true, color1,
                2f / Config.getTextureQuality(), color2,
            )
        }

        engine.textureManager.loadTexture(texture)
        engine.fontManager.loadFont(font)
        fonts[resname] = font
        return font
    }

    companion object {
        private const val FONT_TEXTURE_SIZE = 1024
        private const val STROKE_FONT_TEXTURE_WIDTH = 1024
        private const val STROKE_FONT_TEXTURE_HEIGHT = 512
    }
}
