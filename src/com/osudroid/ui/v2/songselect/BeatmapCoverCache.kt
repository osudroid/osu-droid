package com.osudroid.ui.v2.songselect

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.File
import kotlin.math.min

/**
 * Creates the beatmap set covers shown in the song select and caches them as files.
 */
object BeatmapCoverCache {

    /**
     * Size of the cached covers, the aspect ratio of a panel at its largest.
     */
    private const val COVER_WIDTH = 512
    private const val COVER_HEIGHT = 80
    private const val COVER_QUALITY = 80

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val rootDirectory: File
        get() = File(GlobalManager.getInstance().mainActivity.cacheDir, "covers")

    /**
     * Covers are cached per build, so changes to how they are created apply right away.
     */
    private val directory: File
        get() = File(rootDirectory, BuildConfig.VERSION_CODE.toString())

    private val directoryLock = Any()
    private var isDirectoryPrepared = false


    /**
     * Loads the cover of [model] in the background, creating the cached cover file first if needed.
     */
    fun load(model: BeatmapSetModel) {
        model.isLoadingCover = true

        scope.launch {
            try {
                val beatmap = model.beatmapSetInfo.beatmaps[0]
                val textureName = model.coverTextureName!!

                if (ResourceManager.getInstance().isTextureLoaded(textureName)) {
                    model.coverTexture = ResourceManager.getInstance().getTexture(textureName)
                } else {
                    prepareDirectory()

                    val cacheFile = File(directory, "${beatmap.md5}.webp")

                    if (!cacheFile.exists() && beatmap.backgroundFilename != null) {
                        val bitmap = decodeCover(beatmap.backgroundPath)

                        if (bitmap != null) {
                            // Written to a temporary file first, so a cover is never visible half written. Its name is
                            // unique because duplicate imports of a set share the same cover and load it at the same time.
                            val tempFile = File.createTempFile(beatmap.md5, ".tmp", directory)

                            val compressed = tempFile.outputStream().use { out ->
                                bitmap.compress(Bitmap.CompressFormat.WEBP, COVER_QUALITY, out)
                            }

                            if (!compressed || !tempFile.renameTo(cacheFile)) {
                                tempFile.delete()
                            }
                        }
                    }

                    if (cacheFile.exists()) {
                        val texture = ResourceManager.getInstance().loadHighQualityFile(textureName, cacheFile)

                        if (texture != null) {
                            model.coverTexture = texture
                        } else {
                            cacheFile.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BeatmapCoverCache", "Failed to load cover for ${model.beatmapSetInfo.directory}", e)
            }

            // Otherwise the carousel would try again every frame.
            model.isCoverUnavailable = model.coverTexture == null
            model.isLoadingCover = false
        }
    }

    /**
     * Deletes all cached cover files, they are created again when shown. Covers already loaded stay visible
     * until they are unloaded.
     */
    fun clear() {
        scope.launch {
            synchronized(directoryLock) {
                rootDirectory.deleteRecursively()
                isDirectoryPrepared = false
            }
        }
    }


    /**
     * Creates the cover directory of this build and deletes the ones of previous builds. Covers are loaded
     * in parallel, so this only runs once and under a lock.
     */
    private fun prepareDirectory() = synchronized(directoryLock) {
        if (isDirectoryPrepared) {
            return
        }

        val directory = directory
        rootDirectory.listFiles()?.forEach { if (it != directory) it.deleteRecursively() }
        directory.mkdirs()
        isDirectoryPrepared = true
    }

    /**
     * Decodes the background as a [COVER_WIDTH] x [COVER_HEIGHT] cover. The band in the vertical center
     * with the cover's aspect ratio, scaled down. Large backgrounds are subsampled while decoding.
     */
    private fun decodeCover(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= COVER_WIDTH && bounds.outHeight / (sampleSize * 2) >= COVER_HEIGHT) {
            sampleSize *= 2
        }

        val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize }) ?: return null

        // Backgrounds flatter than the cover are cropped at the sides instead, to keep the aspect ratio.
        val bandWidth = min(decoded.width, decoded.height * COVER_WIDTH / COVER_HEIGHT).coerceAtLeast(1)
        val bandHeight = min(decoded.height, decoded.width * COVER_HEIGHT / COVER_WIDTH).coerceAtLeast(1)
        val band = Bitmap.createBitmap(decoded, (decoded.width - bandWidth) / 2, (decoded.height - bandHeight) / 2, bandWidth, bandHeight)

        return Bitmap.createScaledBitmap(band, COVER_WIDTH, COVER_HEIGHT, true)
    }

}
