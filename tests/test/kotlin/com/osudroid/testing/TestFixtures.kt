package com.osudroid.testing

import android.graphics.Bitmap
import android.graphics.Color
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphMetrics
import org.andengine.graphics.text.GlyphRasterizer
import org.andengine.graphics.texture.Pixels
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import ru.nsu.ccfit.zuev.osu.ResourceManager

/** A [width] x [height] page that reports itself as uploaded under the texture name [id]. */
class Page(id: Int, width: Int = 16, height: Int = width) :
    BitmapTextureAtlas(TextureManager(), width, height, TextureOptions.BILINEAR) {
    init {
        mHardwareTextureID = id
    }
}

/**
 * Glyphs without a font file: visible glyphs are 20 pixels wide and 30 high (24 for 'i'), with an
 * advance of 22 (14 for 'i'); the space has no pixels and an advance of 12.
 */
class FakeRasterizer : GlyphRasterizer {
    override val ascent: Float get() = -40f
    override val descent: Float get() = 10f

    override fun measure(c: Char, out: GlyphMetrics) {
        if (c == ' ') {
            out.width = 0
            out.height = 0
            out.offsetX = 0f
            out.offsetY = 0f
            out.advance = 12f
            return
        }
        out.width = 20
        out.height = if (c == 'i') 24 else 30
        out.offsetX = 2f
        out.offsetY = -30f
        out.advance = if (c == 'i') 14f else 22f
    }

    override fun rasterize(c: Char, layer: Int, out: ByteBuffer) {
        val height = if (c == 'i') 24 else 30
        for (i in 0 until 20 * height) out.put(0xFF.toByte())
    }
}

/** Makes [font] the default font of the resource manager, which UIText looks up on creation. */
fun installTestFont(font: Font) {
    val resources = ResourceManager.getInstance()
    val store = ResourceManager::class.java.getDeclaredField("fontStore").apply { isAccessible = true }.get(resources)
    @Suppress("UNCHECKED_CAST")
    val fonts = store.javaClass.getDeclaredField("fonts").apply { isAccessible = true }.get(store) as MutableMap<String, Font>
    fonts["smallFont"] = font
}

/** The default test font and the shape masks of [installTestFontAndMasks]. */
class TestFontAndMasks(val font: Font, val masks: ShapeMasks)

/**
 * Installs a [FakeRasterizer] font as the default font (see [installTestFont]) and shape masks for
 * [ShapeBatching], both on a glyph atlas of their own, at a raster scale of 1. Needs an installed GL
 * fake; [disposeTestFontAndMasks] undoes it.
 */
fun installTestFontAndMasks(): TestFontAndMasks {
    FontManager.setRasterScale(1f)
    val atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
    val masks = ShapeMasks(atlas)
    ShapeBatching.masks = masks
    val font = Font(FontManager(), FakeRasterizer(), atlas)
    installTestFont(font)
    return TestFontAndMasks(font, masks)
}

/** Removes the shape masks and frees the atlas textures of [masks] and of the glyphs of [font]. */
fun disposeTestFontAndMasks(font: Font, masks: ShapeMasks) {
    ShapeBatching.masks = null
    val arrays = HashSet<GlyphArray>()
    arrays += masks.solid().array
    for (c in "ABi ") {
        font.getLetter(c).mGlyph?.let { arrays += it.array }
    }
    masks.dispose()
    for (a in arrays) if (a.hasTexture) a.texture.dispose()
    FontManager.setRasterScale(1f)
}

/** A player that ignores every call; nothing is played. */
fun silentPlayer(): ExoPlayer =
    Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { _, method, _ ->
        defaultValue(method.returnType)
    } as ExoPlayer

/** Writes a [width] x [height] PNG filled with the straight color ([r], [g], [b], [a]) to [file]. */
fun writePng(file: File, width: Int, height: Int, r: Int, g: Int, b: Int, a: Int): File {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.isPremultiplied = false
    bitmap.eraseColor(Color.argb(a, r, g, b))
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

/** The R, G, B and A byte of the pixel at ([x], [y]). */
fun pixelAt(pixels: Pixels, x: Int, y: Int): List<Int> {
    val offset = pixels.data.position() + (y * pixels.width + x) * 4
    return List(4) { pixels.data.get(offset + it).toInt() and 0xFF }
}

/** The Kotlin and Java source files under [roots], leaving out the directory [excluded]. */
fun sourceFiles(roots: List<File>, excluded: File? = null): Sequence<File> =
    roots.asSequence().flatMap { root ->
        root.walkTopDown()
            .onEnter { excluded == null || it.canonicalFile != excluded.canonicalFile }
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
    }

/** Every line of [file] that is not a comment line, with its location as `path:line`. */
fun codeLines(file: File): List<Pair<String, String>> {
    val path = file.invariantSeparatorsPath
    return file.readLines().mapIndexedNotNull { index, line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) null
        else "$path:${index + 1}" to line
    }
}
