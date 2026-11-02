package org.andengine.opengl.texture.atlas.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLES30
import org.andengine.graphics.atlas.ExtrudedBitmapSource
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.texture.Pixels
import org.andengine.graphics.texture.StraightPixels
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.ITextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.opengl.texture.bitmap.BitmapTextureFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PixelSourceUploadTest {

    private lateinit var gl: RecordingGL


    private class Atlas(listener: ITextureAtlas.ITextureAtlasStateListener<IBitmapTextureAtlasSource>?) :
        BitmapTextureAtlas(TextureManager(), 16, 16, BitmapTextureFormat.RGBA_8888, TextureOptions.BILINEAR, listener) {
        fun write() = writeTextureToHardware()
    }

    private class RecordingListener : ITextureAtlas.ITextureAtlasStateListener.TextureAtlasStateAdapter<IBitmapTextureAtlasSource>() {
        val loaded = ArrayList<IBitmapTextureAtlasSource>()
        val failed = ArrayList<IBitmapTextureAtlasSource>()

        override fun onTextureAtlasSourceLoaded(pTextureAtlas: ITextureAtlas<IBitmapTextureAtlasSource>, pTextureAtlasSource: IBitmapTextureAtlasSource) {
            loaded += pTextureAtlasSource
        }

        override fun onTextureAtlasSourceLoadExeption(pTextureAtlas: ITextureAtlas<IBitmapTextureAtlasSource>, pTextureAtlasSource: IBitmapTextureAtlasSource, pThrowable: Throwable) {
            failed += pTextureAtlasSource
        }
    }

    /** A 3x2 pixel source with distinct bytes per channel. */
    private class FakePixelSource : BaseTextureAtlasSource(0, 0, 3, 2), IPixelTextureAtlasSource {
        val bytes = ByteArray(3 * 2 * 4) { (it * 7 + 1).toByte() }
        var pixelLoads = 0
        var bitmapLoads = 0

        override fun onLoadPixels(): Pixels {
            pixelLoads++
            val data = ByteBuffer.allocateDirect(bytes.size)
            data.put(bytes).flip()
            return Pixels(3, 2, data)
        }

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
            bitmapLoads++
            return Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        }

        override fun deepCopy() = FakePixelSource()
    }

    private class FakeBitmapSource : BaseTextureAtlasSource(0, 0, 3, 2), IBitmapTextureAtlasSource {
        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        override fun deepCopy() = FakeBitmapSource()
    }

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
        gl.calls.clear()
    }

    private fun subImageCalls() = gl.calls.filter { it.startsWith("glTexSubImage2D(") && it.endsWith(",buf)") }

    /** The bitmap uploads (converted by getPixels) as `bitmap(x,y,width,height)`. */
    private fun bitmapUploads() = gl.calls.filter { it.startsWith("glTexSubImage2D(") && it.endsWith(",ints)") }.map { call ->
        val args = call.removePrefix("glTexSubImage2D(").removeSuffix(")").split(",")
        "bitmap(${args[2]},${args[3]},${args[4]},${args[5]})"
    }


    @Test
    fun pixelSource_uploadsWithoutConversion() {
        val listener = RecordingListener()
        val atlas = Atlas(listener)
        val source = FakePixelSource()
        atlas.addTextureAtlasSource(source, 5, 7)

        atlas.write()

        assertEquals(
            listOf("glTexSubImage2D(${GLES20.GL_TEXTURE_2D},0,5,7,3,2,${GLES20.GL_RGBA},${GLES20.GL_UNSIGNED_BYTE},buf)"),
            subImageCalls()
        )
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes.single())
        assertTrue("no getPixels conversion", bitmapUploads().isEmpty())
        assertEquals("no bitmap decoded", 0, source.bitmapLoads)
        assertEquals(listOf<IBitmapTextureAtlasSource>(source), listener.loaded)
        assertTrue(listener.failed.isEmpty())
    }

    @Test
    fun pixelSource_pageUsesImmutableStorage() {
        val atlas = Atlas(null)
        atlas.addTextureAtlasSource(FakePixelSource(), 0, 0)

        atlas.write()

        assertEquals(1, gl.calls.count { it == "glTexStorage2D(${GLES20.GL_TEXTURE_2D},1,${GLES30.GL_RGBA8},16,16)" })
        assertTrue(gl.calls.none { it.startsWith("glTexImage2D(") })
    }

    @Test
    fun bitmapSource_keepsLegacyUpload() {
        val listener = RecordingListener()
        val atlas = Atlas(listener)
        val source = FakeBitmapSource()
        atlas.addTextureAtlasSource(source, 2, 3)

        atlas.write()

        assertEquals(listOf("bitmap(2,3,3,2)"), bitmapUploads())
        assertTrue(subImageCalls().isEmpty())
        assertEquals(listOf<IBitmapTextureAtlasSource>(source), listener.loaded)
    }

    @Test
    fun pixelSource_deliversAgainAfterReload() {
        val atlas = Atlas(null)
        val source = FakePixelSource()
        atlas.addTextureAtlasSource(source, 0, 0)

        atlas.loadToHardware()
        val oldName = atlas.hardwareTextureID
        // A reload (as after a context loss) deletes the old name and uploads onto a fresh one.
        atlas.reloadToHardware()

        assertTrue(atlas.hardwareTextureID != oldName)
        assertEquals(1, gl.calls.count { it.startsWith("glDeleteTextures(") })
        assertEquals(2, gl.calls.count { it.startsWith("glTexStorage2D(") })
        assertEquals(2, source.pixelLoads)
        assertEquals(2, subImageCalls().size)
        assertArrayEquals(source.bytes, gl.texSubImage2DBytes[1])
    }

    @Test
    fun pixelSource_nullPixelsFallBackToBitmap() {
        val listener = RecordingListener()
        val atlas = Atlas(listener)
        val source = object : BaseTextureAtlasSource(0, 0, 3, 2), IPixelTextureAtlasSource {
            override fun onLoadPixels(): Pixels? = null
            override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
            override fun deepCopy() = this
        }
        atlas.addTextureAtlasSource(source, 0, 0)

        atlas.write()

        assertEquals(1, bitmapUploads().size)
        assertEquals(listOf<IBitmapTextureAtlasSource>(source), listener.loaded)
    }

    private fun straightPng(vararg colors: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(colors.size, 1, Bitmap.Config.ARGB_8888)
        bitmap.isPremultiplied = false
        colors.forEachIndexed { x, color -> bitmap.setPixel(x, 0, color) }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    private fun bytesOf(pixels: Pixels): ByteArray {
        val copy = pixels.data.duplicate()
        val bytes = ByteArray(copy.remaining())
        copy.get(bytes)
        return bytes
    }

    private fun rgba(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun straightDecode_keepsColorAtPartialAlpha() {
        val png = straightPng(Color.argb(128, 200, 100, 50), Color.argb(255, 10, 20, 30))

        val pixels = StraightPixels.decode(ByteArrayInputStream(png))!!

        assertEquals(2, pixels.width)
        assertEquals(1, pixels.height)
        assertTrue(pixels.data.isDirect)
        assertArrayEquals(rgba(200, 100, 50, 128, 10, 20, 30, 255), bytesOf(pixels))
    }

    @Test
    fun straightDecode_clearsColorAtZeroAlpha() {
        val png = straightPng(Color.argb(0, 200, 100, 50), Color.argb(128, 200, 100, 50))

        val pixels = StraightPixels.decode(ByteArrayInputStream(png))!!

        assertArrayEquals(rgba(0, 0, 0, 0, 200, 100, 50, 128), bytesOf(pixels))
    }

    @Test
    fun straightDecodeFile_readsTheFile() {
        val file = File.createTempFile("straight", ".png")
        try {
            file.writeBytes(straightPng(Color.argb(64, 1, 2, 3)))

            val pixels = StraightPixels.decodeFile(file.path)!!

            assertArrayEquals(rgba(1, 2, 3, 64), bytesOf(pixels))
        } finally {
            file.delete()
        }
    }

    @Test
    fun straightDecode_returnsNullForGarbage() {
        assertEquals(null, StraightPixels.decode(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
    }

    @Test
    fun fromBitmap_copiesStraightBytesAndRecycles() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.isPremultiplied = false
        bitmap.setPixel(0, 0, Color.argb(128, 200, 100, 50))

        val pixels = StraightPixels.fromBitmap(bitmap)

        assertArrayEquals(rgba(200, 100, 50, 128), bytesOf(pixels))
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun fromBitmap_normalizesAcrossChunks() {
        val size = 300
        // Transparent, partial and opaque pixels, also right at the 16384-pixel chunk boundaries.
        val colors = IntArray(size * size) { i ->
            val alpha = when {
                i % 16384 == 0 || i % 16384 == 16383 || i % 7 == 0 -> 0
                i % 5 == 0 -> 128
                i % 3 == 0 -> 1
                else -> 255
            }
            Color.argb(alpha, i % 251, (i / 7) % 253, (i * 3) % 256)
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.isPremultiplied = false
        bitmap.setPixels(colors, 0, size, 0, 0, size, size)

        val pixels = StraightPixels.fromBitmap(bitmap)

        val expected = ByteArray(colors.size * 4)
        colors.forEachIndexed { i, color ->
            if (Color.alpha(color) != 0) {
                expected[i * 4] = Color.red(color).toByte()
                expected[i * 4 + 1] = Color.green(color).toByte()
                expected[i * 4 + 2] = Color.blue(color).toByte()
                expected[i * 4 + 3] = Color.alpha(color).toByte()
            }
        }
        assertArrayEquals(expected, bytesOf(pixels))
    }

    private class PremultipliedAtlas(listener: ITextureAtlas.ITextureAtlasStateListener<IBitmapTextureAtlasSource>?) :
        BitmapTextureAtlas(TextureManager(), 16, 16, BitmapTextureFormat.RGBA_8888, TextureOptions.BILINEAR_PREMULTIPLYALPHA, listener) {
        fun write() = writeTextureToHardware()
    }

    /** Decodes [png] like the app's sources: bitmap path with the default (premultiplied) decoding, pixels in either variant. */
    private class PngSource(val png: ByteArray, width: Int) : BaseTextureAtlasSource(0, 0, width, 1), IPixelTextureAtlasSource {
        val requestedVariants = ArrayList<Boolean>()
        var bitmapLoads = 0

        override fun canLoadPixels(premultiplied: Boolean) = true

        override fun onLoadPixels(): Pixels? = onLoadPixels(false)

        override fun onLoadPixels(premultiplied: Boolean): Pixels? {
            requestedVariants += premultiplied
            return StraightPixels.decode(ByteArrayInputStream(png), 1, premultiplied)
        }

        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
            bitmapLoads++
            val options = BitmapFactory.Options()
            options.inPreferredConfig = pBitmapConfig
            return BitmapFactory.decodeByteArray(png, 0, png.size, options)
        }

        override fun deepCopy() = PngSource(png, textureWidth)
    }

    /** The memory of [bitmap] as R, G, B, A bytes: what GLUtils.texSubImage2D uploads for an ARGB_8888 bitmap on a device. */
    private fun bitmapMemoryAsRgba(bitmap: Bitmap): ByteArray {
        val buffer = ByteBuffer.allocate(bitmap.rowBytes * bitmap.height)
        bitmap.copyPixelsToBuffer(buffer)
        val bytes = buffer.array()
        // The host graphics library stores B, G, R, A; a device stores R, G, B, A.
        val probe = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        probe.setPixel(0, 0, Color.RED)
        val probeBytes = ByteBuffer.allocate(4)
        probe.copyPixelsToBuffer(probeBytes)
        probe.recycle()
        if (probeBytes.get(0).toInt() == 0) {
            for (i in bytes.indices step 4) {
                val b = bytes[i]
                bytes[i] = bytes[i + 2]
                bytes[i + 2] = b
            }
        }
        return bytes
    }

    @Test
    fun premultipliedAtlas_usesPixelPathWithSameBytes() {
        val png = straightPng(Color.argb(128, 200, 100, 50), Color.argb(0, 9, 9, 9), Color.argb(255, 10, 20, 30), Color.argb(1, 255, 255, 255))
        val listener = RecordingListener()
        val atlas = PremultipliedAtlas(listener)
        val source = PngSource(png, 4)
        atlas.addTextureAtlasSource(source, 2, 3)

        atlas.write()

        // The bytes the bitmap path handed to GLUtils.texSubImage2D before: the premultiplied bitmap's memory.
        val legacyBitmap = BitmapFactory.decodeByteArray(png, 0, png.size)
        assertTrue(legacyBitmap.isPremultiplied)
        val legacyBytes = bitmapMemoryAsRgba(legacyBitmap)
        legacyBitmap.recycle()

        assertEquals(listOf(true), source.requestedVariants)
        assertEquals("no bitmap decoded", 0, source.bitmapLoads)
        assertEquals(
            listOf("glTexSubImage2D(${GLES20.GL_TEXTURE_2D},0,2,3,4,1,${GLES20.GL_RGBA},${GLES20.GL_UNSIGNED_BYTE},buf)"),
            subImageCalls()
        )
        val uploaded = gl.texSubImage2DBytes.single()
        assertArrayEquals(legacyBytes, uploaded)
        // And they really are premultiplied, in R, G, B, A order.
        val expected = intArrayOf(100, 50, 25, 128, 0, 0, 0, 0, 10, 20, 30, 255, 1, 1, 1, 1)
        for (i in expected.indices) {
            assertTrue("byte $i: ${uploaded[i].toInt() and 0xFF} vs ${expected[i]}", Math.abs((uploaded[i].toInt() and 0xFF) - expected[i]) <= 1)
        }
        assertTrue(bitmapUploads().isEmpty())
        assertEquals(listOf<IBitmapTextureAtlasSource>(source), listener.loaded)
    }

    @Test
    fun premultipliedAtlas_straightOnlySourceKeepsBitmapPath() {
        val atlas = PremultipliedAtlas(null)
        val source = FakePixelSource()
        atlas.addTextureAtlasSource(source, 0, 0)

        assertFalse(atlas.canPreparePixels())
        atlas.write()

        assertEquals("straight pixels are never asked for", 0, source.pixelLoads)
        assertEquals(1, source.bitmapLoads)
        assertTrue(subImageCalls().isEmpty())
    }

    @Test
    fun wrongVariant_fallsBackToBitmap() {
        /** Claims both variants but always delivers [deliver]. */
        class WrongVariantSource(val deliver: Boolean) : BaseTextureAtlasSource(0, 0, 1, 1), IPixelTextureAtlasSource {
            var bitmapLoads = 0
            override fun canLoadPixels(premultiplied: Boolean) = true
            override fun onLoadPixels(): Pixels? = onLoadPixels(false)
            override fun onLoadPixels(premultiplied: Boolean): Pixels? {
                val data = ByteBuffer.allocateDirect(4)
                data.put(rgba(10, 20, 30, 128)).flip()
                return Pixels(1, 1, data, deliver)
            }
            override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap {
                bitmapLoads++
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
            override fun deepCopy() = this
        }

        val premultipliedSource = WrongVariantSource(deliver = false)
        val premultipliedAtlas = PremultipliedAtlas(null)
        premultipliedAtlas.addTextureAtlasSource(premultipliedSource, 0, 0)
        premultipliedAtlas.write()
        assertEquals(1, premultipliedSource.bitmapLoads)
        assertTrue(subImageCalls().isEmpty())

        val straightSource = WrongVariantSource(deliver = true)
        val straightAtlas = Atlas(null)
        straightAtlas.addTextureAtlasSource(straightSource, 0, 0)
        straightAtlas.write()
        assertEquals(1, straightSource.bitmapLoads)
        assertEquals(listOf("bitmap(0,0,1,1)"), bitmapUploads())
        assertTrue(subImageCalls().isEmpty())
    }

    @Test
    fun premultipliedDecode_keepsBitmapMemory() {
        val png = straightPng(Color.argb(128, 200, 100, 50), Color.argb(0, 200, 100, 50))

        val pixels = StraightPixels.decode(ByteArrayInputStream(png), 1, true)!!

        assertTrue(pixels.premultiplied)
        assertTrue(pixels.data.isDirect)
        val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size)
        assertArrayEquals(bitmapMemoryAsRgba(bitmap), bytesOf(pixels))
        bitmap.recycle()
        assertFalse(StraightPixels.decode(ByteArrayInputStream(png))!!.premultiplied)
    }

    @Test
    fun extrudedSource_deliversTheRequestedVariant() {
        val png = straightPng(Color.argb(128, 200, 100, 50), Color.argb(255, 10, 20, 30))
        val extruded = ExtrudedBitmapSource(PngSource(png, 2), 1)

        assertTrue(extruded.canLoadPixels(true))
        val premultiplied = extruded.onLoadPixels(true)!!
        val straight = extruded.onLoadPixels(false)!!

        assertTrue(premultiplied.premultiplied)
        assertFalse(straight.premultiplied)
        assertEquals(4, premultiplied.width)
        assertEquals(3, premultiplied.height)
        // Row 1 (the image row): extruded left texel, the two texels, extruded right texel.
        val row = bytesOf(premultiplied).copyOfRange(4 * 4, 8 * 4)
        val decoded = bytesOf(StraightPixels.decode(ByteArrayInputStream(png), 1, true)!!)
        assertArrayEquals(decoded.copyOfRange(0, 4) + decoded + decoded.copyOfRange(4, 8), row)
        assertFalse(ExtrudedBitmapSource(FakePixelSource(), 1).canLoadPixels(true))
    }

    @Test
    fun fromBitmap_recyclesOnRejection() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.argb(128, 200, 100, 50))

        try {
            StraightPixels.fromBitmap(bitmap)
            fail("premultiplied bitmap accepted")
        } catch (expected: IllegalArgumentException) {
        }

        assertTrue(bitmap.isRecycled)
    }
}
