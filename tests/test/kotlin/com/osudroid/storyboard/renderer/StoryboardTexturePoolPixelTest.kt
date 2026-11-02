package com.osudroid.storyboard.renderer

import android.graphics.Bitmap
import android.graphics.Color
import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardOrigin
import com.osudroid.storyboard.model.StoryboardSprite
import com.osudroid.testing.pixelAt
import com.osudroid.testing.writePng
import java.io.File
import java.nio.file.Files
import org.andengine.graphics.texture.Pixels
import org.andengine.graphics.texture.StraightPixels
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.atlas.TextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.andengine.opengl.texture.region.TextureRegion
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoryboardTexturePoolPixelTest {

    private lateinit var directory: File
    private val textureManager = TextureManager()
    private lateinit var pool: StoryboardTexturePool

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("storyboard-pool").toFile()
        pool = StoryboardTexturePool(directory, false, textureManager)
    }

    @After
    fun tearDown() {
        pool.clear()
        directory.deleteRecursively()
    }

    private fun writeImage(name: String, width: Int, height: Int, r: Int, g: Int, b: Int, a: Int) {
        writePng(File(directory, name), width, height, r, g, b, a)
    }

    private fun storyboardOf(vararg usages: Pair<String, Int>) = Storyboard().also { storyboard ->
        for ((name, count) in usages) {
            repeat(count) {
                storyboard.add(StoryboardSprite(StoryboardLayerType.Foreground, StoryboardOrigin.TopLeft, name, 0f, 0f))
            }
        }
    }

    private fun assertRegion(region: TextureRegion, x: Int, y: Int, width: Int, height: Int) {
        assertEquals(x.toFloat(), region.textureX, 0f)
        assertEquals(y.toFloat(), region.textureY, 0f)
        assertEquals(width.toFloat(), region.width, 0f)
        assertEquals(height.toFloat(), region.height, 0f)
    }

    @Suppress("UNCHECKED_CAST")
    private fun sourcesOf(atlas: BitmapTextureAtlas): List<IBitmapTextureAtlasSource> {
        val field = TextureAtlas::class.java.getDeclaredField("mTextureAtlasSources")
        field.isAccessible = true
        return field.get(atlas) as List<IBitmapTextureAtlasSource>
    }

    @Test
    fun storyboardPages_regionsUnchanged() {
        // Sorted by (height, width): b (10x10), a (30x20), c (50x40); shelf-packed with a 2 px margin.
        writeImage("a.png", 30, 20, 255, 0, 0, 255)
        writeImage("b.png", 10, 10, 0, 255, 0, 255)
        writeImage("c.png", 50, 40, 0, 0, 255, 255)
        // Used rarely and oversized: loaded individually.
        writeImage("single.png", 5, 3, 10, 20, 30, 255)
        writeImage("big.png", 401, 2, 40, 50, 60, 255)

        pool.load(storyboardOf("a.png" to 15, "b.png" to 20, "c.png" to 15, "single.png" to 1, "big.png" to 15))

        val a = pool.get("a.png")
        val b = pool.get("b.png")
        val c = pool.get("c.png")
        assertRegion(b, 0, 0, 10, 10)
        assertRegion(a, 12, 0, 30, 20)
        assertRegion(c, 44, 0, 50, 40)
        assertSame(a.texture, b.texture)
        assertSame(a.texture, c.texture)

        val page = a.texture as BitmapTextureAtlas
        assertEquals(2048, page.width)
        assertEquals(2048, page.height)
        val pageSources = sourcesOf(page)
        assertEquals(1, pageSources.size)
        // A single page is shrunk to the used height plus 10 (the tallest line ends at 40 + margin 2).
        assertEquals(2048, pageSources[0].textureWidth)
        assertEquals(52, pageSources[0].textureHeight)

        val single = pool.get("single.png")
        assertRegion(single, 0, 0, 5, 3)
        assertEquals(8, (single.texture as BitmapTextureAtlas).width)
        assertEquals(4, (single.texture as BitmapTextureAtlas).height)

        val big = pool.get("big.png")
        assertRegion(big, 0, 0, 401, 2)
        assertEquals(512, (big.texture as BitmapTextureAtlas).width)
        assertEquals(4, (big.texture as BitmapTextureAtlas).height)

        for (region in listOf(a, single, big)) {
            assertTrue(sourcesOf(region.texture as BitmapTextureAtlas).single() is IPixelTextureAtlasSource)
        }
    }

    @Test
    fun storyboardPage_reloadsFromFiles() {
        writeImage("a.png", 4, 4, 200, 100, 50, 128)
        writeImage("b.png", 2, 2, 1, 2, 3, 255)
        pool.load(storyboardOf("a.png" to 15, "b.png" to 15))

        val a = pool.get("a.png")
        val b = pool.get("b.png")
        val source = sourcesOf(a.texture as BitmapTextureAtlas).single() as IPixelTextureAtlasSource

        val first = checkNotNull(source.onLoadPixels())
        assertEquals(2048, first.width)
        assertEquals(source.textureHeight, first.height)
        assertEquals(listOf(1, 2, 3, 255), pixelAt(first, b.textureX.toInt(), b.textureY.toInt()))
        // Straight alpha: the color stays as it is in the file.
        assertEquals(listOf(200, 100, 50, 128), pixelAt(first, a.textureX.toInt() + 3, a.textureY.toInt() + 3))
        // Texels no image covers stay transparent black.
        assertEquals(listOf(0, 0, 0, 0), pixelAt(first, 2047, 0))

        // A later load (e.g. after a context loss) composes the page again from the files.
        writeImage("b.png", 2, 2, 9, 8, 7, 255)
        val second = checkNotNull(source.onLoadPixels())
        assertNotSame(first.data, second.data)
        assertEquals(listOf(9, 8, 7, 255), pixelAt(second, b.textureX.toInt(), b.textureY.toInt()))
        assertEquals(listOf(200, 100, 50, 128), pixelAt(second, a.textureX.toInt() + 3, a.textureY.toInt() + 3))
    }

    @Test
    fun storyboardPage_missingFileBecomesRedPlaceholder() {
        writeImage("a.png", 4, 4, 1, 2, 3, 255)
        pool.load(storyboardOf("a.png" to 15, "missing.png" to 15))

        val missing = pool.get("missing.png")
        assertRegion(missing, 0, 0, 1, 1)
        val source = sourcesOf(missing.texture as BitmapTextureAtlas).single() as IPixelTextureAtlasSource
        val pixels = checkNotNull(source.onLoadPixels())
        assertEquals(listOf(255, 0, 0, 255), pixelAt(pixels, 0, 0))

        val lone = pool.get("lone-missing.png")
        assertRegion(lone, 0, 0, 1, 1)
        val loneSource = sourcesOf(lone.texture as BitmapTextureAtlas).single() as IPixelTextureAtlasSource
        assertEquals(listOf(255, 0, 0, 255), pixelAt(checkNotNull(loneSource.onLoadPixels()), 0, 0))
    }

    @Test
    fun singleFullSizeTexture_deliveredWithoutPageCopy() {
        writeImage("bg.png", 7, 5, 4, 5, 6, 255)
        val file = File(directory, "bg.png")
        val decoded = ArrayList<Pixels>()
        val source = StoryboardTexturePool.FileImageSource(7, 5, arrayOf(file), intArrayOf(0), intArrayOf(0)) {
            StraightPixels.decodeFile(it.absolutePath)?.also { pixels -> decoded += pixels }
        }

        val pixels = checkNotNull(source.onLoadPixels())
        assertSame(decoded.single(), pixels)
        assertEquals(listOf(4, 5, 6, 255), pixelAt(pixels, 6, 4))

        // An image smaller than its texture is still composed onto a page.
        val padded = StoryboardTexturePool.FileImageSource(8, 8, arrayOf(file), intArrayOf(0), intArrayOf(0)) {
            StraightPixels.decodeFile(it.absolutePath)
        }
        val page = checkNotNull(padded.onLoadPixels())
        assertEquals(8, page.width)
        assertEquals(listOf(4, 5, 6, 255), pixelAt(page, 6, 4))
        assertEquals(listOf(0, 0, 0, 0), pixelAt(page, 7, 7))

        // The pool's own single textures take the same route.
        val region = pool.get("bg.png")
        val poolSource = sourcesOf(region.texture as BitmapTextureAtlas).single() as IPixelTextureAtlasSource
        val poolPixels = checkNotNull(poolSource.onLoadPixels())
        assertEquals(7, poolPixels.width)
        assertEquals(5, poolPixels.height)
    }

    @Test
    fun storyboardPage_bitmapFallbackComposesTheSamePage() {
        writeImage("a.png", 3, 3, 10, 20, 30, 255)
        pool.load(storyboardOf("a.png" to 15))

        val a = pool.get("a.png")
        val source = sourcesOf(a.texture as BitmapTextureAtlas).single()
        val bitmap = checkNotNull(source.onLoadBitmap(Bitmap.Config.ARGB_8888))
        try {
            assertEquals(source.textureWidth, bitmap.width)
            assertEquals(source.textureHeight, bitmap.height)
            assertEquals(Color.rgb(10, 20, 30), bitmap.getPixel(a.textureX.toInt() + 1, a.textureY.toInt() + 1))
            assertEquals(0, bitmap.getPixel(2047, 0))
        } finally {
            bitmap.recycle()
        }
    }
}
