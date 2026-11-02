package org.andengine.graphics.atlas

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES20
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.texture.Pixels
import org.andengine.graphics.texture.StraightPixels
import org.andengine.opengl.texture.PixelFormat
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.opengl.util.GLState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PixelPageTest {

    private lateinit var gl: RecordingGL

    /** Records the legacy bitmap upload (the getPixels conversion) instead of calling GL. */
    private class RecordingGLState : GLState() {
        val bitmapUploads = ArrayList<String>()
        private var nextTextureName = 100

        override fun glTexSubImage2D(pTarget: Int, pLevel: Int, pX: Int, pY: Int, pBitmap: Bitmap, pPixelFormat: PixelFormat) {
            bitmapUploads += "bitmap($pX,$pY,${pBitmap.width},${pBitmap.height})"
        }

        override fun generateTexture() = nextTextureName++
    }

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
        gl.calls.clear()
    }

    /** ARGB ints as straight R, G, B, A bytes. */
    private fun pixelsOf(argb: IntArray, width: Int, height: Int): Pixels {
        val data = ByteBuffer.allocateDirect(argb.size * 4)
        for (color in argb) {
            data.put(Color.red(color).toByte()).put(Color.green(color).toByte()).put(Color.blue(color).toByte()).put(Color.alpha(color).toByte())
        }
        data.flip()
        return Pixels(width, height, data)
    }

    private fun bytesOf(pixels: Pixels): ByteArray {
        val copy = pixels.data.duplicate()
        val bytes = ByteArray(copy.remaining())
        copy.get(bytes)
        return bytes
    }

    private fun bytesOf(argb: IntArray): ByteArray = bytesOf(pixelsOf(argb, argb.size, 1))

    /** What the bitmap upload delivered for [bitmap]: its unpremultiplied colors as R, G, B, A bytes. */
    private fun uploadedBytesOf(bitmap: Bitmap): ByteArray {
        val argb = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bytesOf(argb)
    }

    @Test
    fun pixelPage_putCopiesRowsAtOffset() {
        val a = Color.argb(255, 1, 2, 3)
        val b = Color.argb(128, 4, 5, 6)
        val c = Color.argb(200, 7, 8, 9)
        val d = Color.argb(255, 10, 11, 12)
        val page = PixelPage(5, 4)

        page.put(pixelsOf(intArrayOf(a, b, c, d), 2, 2), 1, 2)
        val result = page.toPixels()
        page.release()

        assertEquals(5, result.width)
        assertEquals(4, result.height)
        assertTrue(result.data.isDirect)
        val expected = IntArray(5 * 4)
        expected[2 * 5 + 1] = a
        expected[2 * 5 + 2] = b
        expected[3 * 5 + 1] = c
        expected[3 * 5 + 2] = d
        assertArrayEquals(bytesOf(expected), bytesOf(result))
    }

    @Test
    fun pixelPage_extrusionMatchesAtlasEdgeExtrusion() {
        val width = 3
        val height = 2
        val padding = 2
        val image = IntArray(width * height) { Color.argb(40 + it * 30, it * 11, 200 - it * 7, it * 3 + 1) }

        // Placed at an offset inside a larger page: the extruded block lands at (1, 1).
        val page = PixelPage(9, 8)
        page.put(pixelsOf(image, width, height), 1 + padding, 1 + padding, padding)
        val result = page.toPixels()
        page.release()

        val extruded = AtlasEdgeExtrusion.extrude(image, width, height, padding)
        val outW = width + 2 * padding
        val outH = height + 2 * padding
        val expected = IntArray(9 * 8)
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                expected[(y + 1) * 9 + x + 1] = extruded[y * outW + x]
            }
        }
        assertArrayEquals(bytesOf(expected), bytesOf(result))
    }

    @Test
    fun pixelPage_matchesCanvasPageForOpaqueAndTransparent() {
        val colors = intArrayOf(
            Color.argb(255, 200, 10, 30), Color.argb(0, 0, 0, 0), Color.argb(255, 1, 2, 3),
            Color.argb(0, 0, 0, 0), Color.argb(255, 40, 50, 60), Color.argb(255, 255, 255, 255)
        )
        fun image(): Bitmap {
            val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(colors, 0, 3, 0, 0, 3, 2)
            return bitmap
        }

        // The previous page content: the extruded bitmap as the bitmap upload converted it.
        val canvasSource = ExtrudedBitmapSource(BitmapSource(3, 2, ::image), 2)
        val canvasBitmap = canvasSource.onLoadBitmap(Bitmap.Config.ARGB_8888)!!
        val expected = uploadedBytesOf(canvasBitmap)
        canvasBitmap.recycle()

        val straight = image()
        straight.isPremultiplied = false
        val page = PixelPage(7, 6)
        page.put(StraightPixels.fromBitmap(straight), 2, 2, 2)
        val result = page.toPixels()
        page.release()

        assertArrayEquals(expected, bytesOf(result))
    }

    /** A bitmap source that counts its decodes. */
    private class BitmapSource(width: Int, height: Int, private val decode: () -> Bitmap) :
        BaseTextureAtlasSource(0, 0, width, height), IBitmapTextureAtlasSource {
        var loads = 0

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap {
            loads++
            return decode()
        }

        override fun deepCopy() = this
    }

    private class PixelSource(private val image: () -> Pixels) : BaseTextureAtlasSource(0, 0, 2, 1), IPixelTextureAtlasSource {
        var pixelLoads = 0
        var bitmapLoads = 0

        override fun onLoadPixels(): Pixels {
            pixelLoads++
            return image()
        }

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap {
            bitmapLoads++
            return Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        }

        override fun deepCopy() = this
    }

    private fun subImageCalls() = gl.calls.filter { it.startsWith("glTexSubImage2D(") }

    /** Adds [source] to a fresh skin page set, uploads the page and returns it with the recorded bitmap uploads. */
    private fun uploadOnSkinPage(source: IBitmapTextureAtlasSource): List<String> {
        val pages = TextureAtlasPages(TextureManager(), 64, 32, padding = 1)
        pages.beginSession()
        val region = pages.add(source, TextureOptions.BILINEAR)!!
        pages.endSession()
        val glState = RecordingGLState()
        (region.texture as BitmapTextureAtlas).loadToHardware(glState)
        return glState.bitmapUploads
    }

    @Test
    fun skinAtlasPages_usePixelPath() {
        val red = Color.argb(255, 255, 0, 0)
        val blue = Color.argb(128, 0, 0, 255)
        val expected = bytesOf(AtlasEdgeExtrusion.extrude(intArrayOf(red, blue), 2, 1, 1))

        // An inner pixel source: its bytes are extruded and uploaded without any bitmap.
        val pixelSource = PixelSource { pixelsOf(intArrayOf(red, blue), 2, 1) }
        val pixelUploads = uploadOnSkinPage(pixelSource)
        assertTrue(pixelUploads.isEmpty())
        assertEquals(0, pixelSource.bitmapLoads)
        assertEquals(1, pixelSource.pixelLoads)
        assertEquals(listOf("glTexSubImage2D(${GLES20.GL_TEXTURE_2D},0,0,0,4,3,${GLES20.GL_RGBA},${GLES20.GL_UNSIGNED_BYTE},buf)"), subImageCalls())
        assertArrayEquals(expected, gl.texSubImage2DBytes.single())

        // An inner bitmap source, even one with straight alpha, keeps the bitmap path with one decode.
        gl.calls.clear()
        gl.texSubImage2DBytes.clear()
        val straightSource = BitmapSource(2, 1) {
            val bitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
            bitmap.isPremultiplied = false
            bitmap.setPixel(0, 0, red)
            bitmap.setPixel(1, 0, blue)
            bitmap
        }
        assertEquals(listOf("bitmap(0,0,4,3)"), uploadOnSkinPage(straightSource))
        assertEquals(1, straightSource.loads)
        assertTrue(subImageCalls().isEmpty())
    }

    @Test
    fun extrudedSource_pixelsOfWrongSizeFallBackToBitmap() {
        // Declared 2x1, delivers 1x1.
        val inner = PixelSource { pixelsOf(intArrayOf(Color.RED), 1, 1) }
        val extruded = ExtrudedBitmapSource(inner, 1)

        assertEquals(null, extruded.onLoadPixels())
        assertEquals(listOf("bitmap(0,0,4,3)"), uploadOnSkinPage(inner))
        assertEquals(1, inner.bitmapLoads)
        assertTrue(subImageCalls().isEmpty())
    }

    @Test
    fun extrudedSource_bitmapSourceIsNotDecodedForPixels() {
        val source = BitmapSource(2, 1) { Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888) }

        assertEquals(null, ExtrudedBitmapSource(source, 1).onLoadPixels())
        assertEquals(0, source.loads)
    }

    @Test
    fun skinAtlasPages_premultipliedBitmapKeepsBitmapPathWithOneDecode() {
        val source = BitmapSource(2, 1) {
            val bitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
            bitmap.setPixel(0, 0, Color.argb(128, 255, 0, 0))
            bitmap
        }

        val uploads = uploadOnSkinPage(source)

        assertEquals(listOf("bitmap(0,0,4,3)"), uploads)
        assertTrue(subImageCalls().isEmpty())
        assertEquals("decoded exactly once", 1, source.loads)
    }

    @Test
    fun extrudedSource_deliversPixelsAgainOnEveryLoad() {
        val inner = PixelSource { pixelsOf(intArrayOf(Color.RED, Color.BLUE), 2, 1) }
        val extruded = ExtrudedBitmapSource(inner, 1)

        val first = bytesOf(extruded.onLoadPixels()!!)
        val second = bytesOf(extruded.onLoadPixels()!!)

        assertEquals(2, inner.pixelLoads)
        assertArrayEquals(first, second)
    }
}
