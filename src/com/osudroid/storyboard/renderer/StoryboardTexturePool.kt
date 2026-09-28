package com.osudroid.storyboard.renderer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.edlplan.andengine.TextureHelper
import com.osudroid.storyboard.model.Storyboard
import com.reco1l.andengine.texture.BlankTextureRegion
import java.io.File
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource

/**
 * Loads and manages the textures of a storyboard, rooted at the beatmap folder.
 *
 * Textures that are referenced frequently (at least [PACK_USAGE_THRESHOLD] times) and are small
 * enough are shelf-packed into shared [BitmapTextureAtlas] pages so that the sprite batch can
 * draw them without texture switches. All other textures are loaded individually. Missing or
 * corrupt files resolve to a 1x1 red placeholder, matching the previous implementation.
 */
class StoryboardTexturePool @JvmOverloads constructor(
    private val directory: File,

    /**
     * Whether textures of the current skin are preferred over the textures in [directory].
     */
    private val useSkinSprites: Boolean = false
) {
    private val textures = HashMap<String, TextureRegion>()
    private val createdTextures = HashSet<ITexture>()

    private val bitmapOptions = BitmapFactory.Options().also { it.inPremultiplied = true }

    // Shelf packing state.
    private var currentPage = 0
    private var currentX = 0
    private var currentY = 0
    private var lineMaxY = 0

    /**
     * Loads all textures used by a storyboard.
     *
     * @param storyboard The storyboard to load the textures of.
     */
    fun load(storyboard: Storyboard) {
        val counts = storyboard.textureUsageCounts().toMutableMap()

        val background = storyboard.backgroundFilename
        if (background != null && !storyboard.usesBackgroundImage()) {
            counts[background] = (counts[background] ?: 0) + 1
        }

        if (useSkinSprites) {
            // Textures provided by the skin are owned by the skin, so they are neither packed nor
            // loaded from the beatmap folder.
            counts.keys.removeAll { name ->
                val region = getSkinTexture(name) ?: return@removeAll false
                textures[name] = region
                true
            }
        }

        packAll(counts.keys.filter { counts[it]!! >= PACK_USAGE_THRESHOLD })

        for (name in counts.keys) {
            if (counts[name]!! < PACK_USAGE_THRESHOLD) {
                add(name)
            }
        }
    }

    /**
     * Returns the texture region of the given path, loading it individually if it has not been
     * loaded yet.
     *
     * @param name The texture path relative to the beatmap folder.
     */
    fun get(name: String): TextureRegion = textures[name] ?: add(name)

    /**
     * Unloads all textures created by this pool.
     */
    fun clear() {
        textures.clear()

        val textureManager = GlobalManager.getInstance().engine?.textureManager

        for (texture in createdTextures) {
            textureManager?.unloadTexture(texture)
        }

        createdTextures.clear()
        currentPage = 0
        currentX = 0
        currentY = 0
        lineMaxY = 0
    }

    private fun getSkinTexture(name: String): TextureRegion? {
        val resources = ResourceManager.getInstance()
        val skinName = name.substringBeforeLast('.')

        if (!resources.isTextureLoaded(skinName)) {
            return null
        }

        return resources.getTexture(skinName)?.takeIf { it !is BlankTextureRegion }
    }

    /**
     * Resolves the file of a texture. Some old storyboards do not include a file extension, in
     * which case the supported image extensions are tried, as in osu!lazer.
     */
    private fun resolve(name: String): File {
        val file = File(directory, name)

        if (file.extension.isEmpty() && !file.exists()) {
            for (extension in IMAGE_EXTENSIONS) {
                val candidate = File(directory, name + extension)

                if (candidate.exists()) {
                    return candidate
                }
            }
        }

        return file
    }

    private fun add(name: String): TextureRegion {
        if (useSkinSprites) {
            val region = getSkinTexture(name)

            if (region != null) {
                textures[name] = region
                return region
            }
        }

        val bitmap = loadBitmap(resolve(name))
        val region = TextureHelper.createRegion(bitmap)

        createdTextures.add(region.texture)
        textures[name] = region
        bitmap.recycle()

        return region
    }

    private fun packAll(names: List<String>) {
        val infos = names.map { loadInfo(it) }
            .sortedWith(compareBy({ it.height }, { it.width }))

        val toPack = mutableListOf<TextureInfo>()

        for (info in infos) {
            if (info.width > MAX_PACKED_SIZE || info.height > MAX_PACKED_SIZE) {
                // Oversized textures are loaded individually.
                add(info.name)
            } else {
                placeInPack(info)
                toPack.add(info)
            }
        }

        if (toPack.isEmpty()) {
            return
        }

        val textureManager = GlobalManager.getInstance().engine.textureManager
        val byPage = toPack.groupBy { it.pageIndex }

        // The first (and possibly only) page can be shrunk to the used height.
        val singlePageHeight = if (byPage.size == 1) lineMaxY + 10 else PAGE_SIZE

        val pageBitmap = Bitmap.createBitmap(
            PAGE_SIZE,
            singlePageHeight.coerceAtMost(PAGE_SIZE),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(pageBitmap)
        val paint = Paint().also {
            it.isAntiAlias = true
            it.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }

        for ((_, pageInfos) in byPage) {
            pageBitmap.eraseColor(Color.argb(0, 0, 0, 0))

            for (info in pageInfos) {
                val bitmap = loadBitmap(resolve(info.name))
                canvas.drawBitmap(bitmap, info.x.toFloat(), info.y.toFloat(), paint)
                bitmap.recycle()
            }

            val source = QualityFileBitmapSource(TextureHelper.createFactoryFromBitmap(pageBitmap))
            val atlas = BitmapTextureAtlas(textureManager, PAGE_SIZE, PAGE_SIZE, TextureOptions.BILINEAR)

            atlas.addTextureAtlasSource(source, 0, 0)
            textureManager.loadTexture(atlas)
            createdTextures.add(atlas)

            for (info in pageInfos) {
                textures[info.name] = TextureRegion(
                    atlas,
                    info.x.toFloat(), info.y.toFloat(),
                    info.width.toFloat(), info.height.toFloat()
                )
            }
        }

        pageBitmap.recycle()
    }

    private fun placeInPack(info: TextureInfo) {
        if (currentX + info.width + MARGIN >= PAGE_SIZE) {
            currentX = 0
            currentY = lineMaxY + MARGIN
        }

        if (currentY + info.height + MARGIN >= PAGE_SIZE) {
            ++currentPage
            currentX = 0
            currentY = 0
            lineMaxY = 0
        }

        info.pageIndex = currentPage
        info.x = currentX
        info.y = currentY
        currentX += info.width + MARGIN
        lineMaxY = maxOf(lineMaxY, currentY + info.height + MARGIN)
    }

    private fun loadInfo(name: String) = TextureInfo(name).also {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(resolve(name).absolutePath, options)

            if (options.outWidth > 0 && options.outHeight > 0) {
                it.width = options.outWidth
                it.height = options.outHeight
            }
        } catch (_: Exception) {
        }
    }

    private fun loadBitmap(file: File): Bitmap {
        return try {
            BitmapFactory.decodeFile(file.absolutePath, bitmapOptions) ?: errorBitmap()
        } catch (_: Exception) {
            errorBitmap()
        }
    }

    private fun errorBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).also {
        it.setPixel(0, 0, Color.argb(255, 255, 0, 0))
    }

    private class TextureInfo(@JvmField val name: String) {
        @JvmField var width = 1
        @JvmField var height = 1
        @JvmField var x = 0
        @JvmField var y = 0
        @JvmField var pageIndex = -1
    }

    companion object {
        /**
         * The minimum number of references a texture needs to be packed into a shared atlas.
         */
        private const val PACK_USAGE_THRESHOLD = 15

        private const val PAGE_SIZE = 2048
        private const val MAX_PACKED_SIZE = 400
        private const val MARGIN = 2

        private val IMAGE_EXTENSIONS = arrayOf(".jpeg", ".jpg", ".png")
    }
}
