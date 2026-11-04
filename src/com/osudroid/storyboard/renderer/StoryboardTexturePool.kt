package com.osudroid.storyboard.renderer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.osudroid.storyboard.model.Storyboard
import com.reco1l.andengine.texture.BlankTextureRegion
import java.io.File
import java.nio.ByteBuffer
import org.andengine.graphics.atlas.PixelPage
import org.andengine.graphics.texture.Pixels
import org.andengine.graphics.texture.RawPixelSource
import org.andengine.graphics.texture.StraightPixels
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.texture.region.TextureRegionFactory
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Loads and manages the textures of a storyboard, rooted at the beatmap folder.
 *
 * Textures that are referenced frequently (at least [PACK_USAGE_THRESHOLD] times) and are small
 * enough are shelf-packed into shared [BitmapTextureAtlas] pages so that the sprite batch can
 * draw them without texture switches. All other textures are loaded individually. Missing or
 * corrupt files resolve to a 1x1 red placeholder.
 *
 * Every texture is a [FileImageSource]: it composes its image files whenever the texture is
 * loaded (also after a context loss), so no composed page is kept in memory.
 */
class StoryboardTexturePool @JvmOverloads constructor(
    private val directory: File,

    /**
     * Whether textures of the current skin are preferred over the textures in [directory].
     */
    private val useSkinSprites: Boolean = false,

    /**
     * The texture manager to load the textures with. Tests pass their own.
     */
    private val textureManager: TextureManager = GlobalManager.getInstance().engine.textureManager
) {
    private val textures = HashMap<String, TextureRegion>()
    private val createdTextures = HashSet<ITexture>()

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

        for (texture in createdTextures) {
            textureManager.unloadTexture(texture)
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

        val info = loadInfo(name)
        val source = FileImageSource(info.width, info.height, arrayOf(info.file), intArrayOf(0), intArrayOf(0))

        // Power-of-two texture of at least 4x4, as for any other single image.
        var textureWidth = 4
        var textureHeight = 4
        while (textureWidth < info.width) textureWidth *= 2
        while (textureHeight < info.height) textureHeight *= 2

        val texture = BitmapTextureAtlas(textureManager, textureWidth, textureHeight, TextureOptions.BILINEAR)
        val region = TextureRegionFactory.createFromSource(texture, source, 0, 0, false)
        textureManager.loadTexture(texture)

        createdTextures.add(texture)
        textures[name] = region

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

        val byPage = toPack.groupBy { it.pageIndex }

        // The first (and possibly only) page can be shrunk to the used height.
        val singlePageHeight = if (byPage.size == 1) lineMaxY + 10 else PAGE_SIZE
        val pageHeight = singlePageHeight.coerceAtMost(PAGE_SIZE)

        for ((_, pageInfos) in byPage) {
            val source = FileImageSource(
                PAGE_SIZE,
                pageHeight,
                Array(pageInfos.size) { pageInfos[it].file },
                IntArray(pageInfos.size) { pageInfos[it].x },
                IntArray(pageInfos.size) { pageInfos[it].y }
            )
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

    private fun loadInfo(name: String) = TextureInfo(name, resolve(name)).also {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(it.file.absolutePath, options)

            if (options.outWidth > 0 && options.outHeight > 0) {
                it.width = options.outWidth
                it.height = options.outHeight
            }
        } catch (_: Exception) {
        }
    }

    private class TextureInfo(@JvmField val name: String, @JvmField val file: File) {
        @JvmField var width = 1
        @JvmField var height = 1
        @JvmField var x = 0
        @JvmField var y = 0
        @JvmField var pageIndex = -1
    }

    /**
     * A [width]x[height] image composed of the image [files], each with its top-left corner at
     * ([xs], [ys]). Files that cannot be decoded become a 1x1 red pixel; texels no file covers are
     * transparent. Nothing is kept between loads: every load decodes the files again.
     *
     * On a straight-alpha atlas the files are decoded without premultiplication ([decode]) and
     * composed as bytes ([onLoadPixels]); a single file that fills the whole image is delivered as
     * decoded, without a copy. [onLoadBitmap] composes the files on a canvas instead.
     */
    class FileImageSource(
        width: Int,
        height: Int,
        private val files: Array<File>,
        private val xs: IntArray,
        private val ys: IntArray,
        // Tests replace the decoder to observe what it decodes.
        private val decode: (File) -> Pixels? = { StraightPixels.decodeFile(it.absolutePath) }
    ) : BaseTextureAtlasSource(0, 0, width, height), RawPixelSource {

        override fun deepCopy() = FileImageSource(mTextureWidth, mTextureHeight, files, xs, ys, decode).also {
            it.setTextureX(mTextureX)
            it.setTextureY(mTextureY)
        }

        override fun onLoadPixels(): Pixels? {
            var page: PixelPage? = null

            try {
                for (i in files.indices) {
                    var image = decode(files[i])

                    if (image == null) {
                        if (isDecodable(files[i])) {
                            // decode() could not give straight pixels for this file; fall back to the bitmap path
                            // (canvas composition).
                            return null
                        }

                        image = errorPixels()
                    }

                    if (xs[i] + image.width > mTextureWidth || ys[i] + image.height > mTextureHeight) {
                        // Larger than its reserved space (the bounds could not be read): drawn clipped, as on a canvas.
                        return null
                    }

                    if (files.size == 1 && xs[i] == 0 && ys[i] == 0 && image.width == mTextureWidth && image.height == mTextureHeight) {
                        // The image is the whole texture: no page to compose.
                        return image
                    }

                    if (page == null) {
                        page = PixelPage(mTextureWidth, mTextureHeight)
                    }

                    page.put(image, xs[i], ys[i])
                }

                return (page ?: PixelPage(mTextureWidth, mTextureHeight)).toPixels()
            } finally {
                page?.release()
            }
        }

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap {
            val page = Bitmap.createBitmap(mTextureWidth, mTextureHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(page)
            val paint = Paint().also {
                it.isAntiAlias = true
                it.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            }

            for (i in files.indices) {
                val bitmap = loadBitmap(files[i])
                canvas.drawBitmap(bitmap, xs[i].toFloat(), ys[i].toFloat(), paint)
                bitmap.recycle()
            }

            return page
        }

        override fun toString() = "${javaClass.simpleName}(${files.size} files, ${mTextureWidth}x$mTextureHeight)"

        private fun isDecodable(file: File): Boolean {
            val bitmap = try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            } ?: return false

            bitmap.recycle()
            return true
        }

        private fun loadBitmap(file: File): Bitmap {
            return try {
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().also { it.inPremultiplied = true }) ?: errorBitmap()
            } catch (_: Exception) {
                errorBitmap()
            }
        }

        private fun errorBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).also {
            it.setPixel(0, 0, Color.argb(255, 255, 0, 0))
        }

        private fun errorPixels(): Pixels {
            val data = ByteBuffer.allocateDirect(4)
            data.put(0, 255.toByte()).put(3, 255.toByte())
            return Pixels(1, 1, data)
        }
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
