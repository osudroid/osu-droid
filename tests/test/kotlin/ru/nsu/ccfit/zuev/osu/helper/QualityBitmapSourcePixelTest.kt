package ru.nsu.ccfit.zuev.osu.helper

import android.graphics.Bitmap
import android.graphics.Color
import com.osudroid.testing.pixelAt
import com.osudroid.testing.writePng
import java.io.File
import java.nio.file.Files
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QualityBitmapSourcePixelTest {

    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("quality-source").toFile()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun writeImage(width: Int, height: Int, r: Int, g: Int, b: Int, a: Int): File =
        writePng(File(directory, "image.png"), width, height, r, g, b, a)

    private class NoTexturesFileSource(file: File) : QualityFileBitmapSource(file) {
        override fun isNoTexturesMode() = true
    }

    @Test
    fun qualityFileSource_deliversStraightPixels() {
        val file = writeImage(6, 4, 200, 100, 50, 128)
        val source: IPixelTextureAtlasSource = QualityFileBitmapSource(file)

        val pixels = checkNotNull(source.onLoadPixels())
        assertEquals(6, pixels.width)
        assertEquals(4, pixels.height)
        assertEquals(listOf(200, 100, 50, 128), pixelAt(pixels, 5, 3))

        // Every load decodes again (e.g. after a context loss).
        val again = checkNotNull(source.onLoadPixels())
        assertEquals(listOf(200, 100, 50, 128), pixelAt(again, 0, 0))
    }

    @Test
    fun qualityFileSource_honoursInSampleSize() {
        val file = writeImage(8, 6, 1, 2, 3, 255)
        val source = QualityFileBitmapSource(file, 2)
        assertEquals(4, source.width)
        assertEquals(3, source.height)

        val pixels = checkNotNull(source.onLoadPixels())
        assertEquals(4, pixels.width)
        assertEquals(3, pixels.height)
        assertEquals(listOf(1, 2, 3, 255), pixelAt(pixels, 3, 2))
    }

    @Test
    fun qualityFileSource_preloadIsUsedOnceByThePixelPath() {
        val file = writeImage(3, 3, 9, 8, 7, 255)
        val source = QualityFileBitmapSource(file)
        assertTrue(source.preload())

        // The file is gone: only the preloaded pixels can deliver the first load.
        assertTrue(file.delete())
        val preloaded = checkNotNull(source.onLoadPixels())
        assertEquals(listOf(9, 8, 7, 255), pixelAt(preloaded, 2, 2))

        // The preload is used up; later loads read the file again.
        assertNull(source.onLoadPixels())
        assertNull(source.onLoadBitmap(Bitmap.Config.ARGB_8888))
    }

    @Test
    fun qualityFileSource_bitmapPathAfterPreloadStillWorks() {
        val file = writeImage(3, 2, 9, 8, 7, 255)
        val source = QualityFileBitmapSource(file)
        assertTrue(source.preload())

        // A premultiplied atlas loads through the bitmap path; the pending pixels must not get in the way.
        val bitmap = checkNotNull(source.onLoadBitmap(Bitmap.Config.ARGB_8888))
        assertEquals(3, bitmap.width)
        assertEquals(Color.rgb(9, 8, 7), bitmap.getPixel(1, 1))
        bitmap.recycle()

        // The pixels pending from the preload were dropped, the next pixel load decodes anew.
        assertTrue(file.delete())
        assertNull(source.onLoadPixels())
    }

    @Test
    fun qualityFileSource_preloadFailsForCorruptFile() {
        val file = File(directory, "broken.png").also { it.writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val source = QualityFileBitmapSource(file)
        assertFalse(source.preload())
        assertNull(source.onLoadPixels())
    }

    @Test
    fun noTexturesMode_paintsPlaceholderThroughBitmapPath() {
        val file = writeImage(8, 8, 200, 100, 50, 255)
        val source = NoTexturesFileSource(file)

        // The placeholder painting lives in the bitmap path, so the pixel path stays out of the way.
        assertNull(source.onLoadPixels())
        assertTrue(source.preload())
        assertNull(source.onLoadPixels())

        val bitmap = checkNotNull(source.onLoadBitmap(Bitmap.Config.ARGB_8888))
        assertEquals(0x33000000, bitmap.getPixel(4, 4))
        bitmap.recycle()

        val fresh = checkNotNull(NoTexturesFileSource(file).onLoadBitmap(Bitmap.Config.ARGB_8888))
        assertEquals(0x33000000, fresh.getPixel(4, 4))
        fresh.recycle()
    }

    @Test
    fun qualityFileSource_deliversPremultipliedPixels() {
        val file = writeImage(2, 2, 200, 100, 50, 128)
        val source = QualityFileBitmapSource(file)

        assertTrue(source.canLoadPixels(true))
        val pixels = checkNotNull(source.onLoadPixels(true))
        assertTrue(pixels.premultiplied)
        val premultiplied = pixelAt(pixels, 1, 1)
        listOf(100, 50, 25, 128).forEachIndexed { i, expected ->
            assertTrue("channel $i: ${premultiplied[i]}", Math.abs(premultiplied[i] - expected) <= 1)
        }
        assertFalse(checkNotNull(source.onLoadPixels(false)).premultiplied)
    }

    @Test
    fun preloadStash_usedOnlyForMatchingVariant() {
        val file = writeImage(3, 3, 200, 100, 50, 128)
        val straight = QualityFileBitmapSource(file)
        val premultiplied = QualityFileBitmapSource(file)
        assertTrue(straight.preload())
        assertTrue(premultiplied.preload(true))

        // The file is gone: only a preload can deliver now.
        assertTrue(file.delete())

        // The wrong variant is never handed out, and asking for it drops the stash (no leak).
        assertNull(straight.onLoadPixels(true))
        assertNull(straight.onLoadPixels(false))
        assertNull(premultiplied.onLoadPixels(false))
        assertNull(premultiplied.onLoadPixels(true))

        val file2 = writeImage(3, 3, 200, 100, 50, 128)
        val matching = QualityFileBitmapSource(file2)
        assertTrue(matching.preload(true))
        assertTrue(file2.delete())
        val pixels = checkNotNull(matching.onLoadPixels(true))
        assertTrue(pixels.premultiplied)
        assertTrue(Math.abs(pixelAt(pixels, 2, 2)[0] - 100) <= 1)
    }
}
