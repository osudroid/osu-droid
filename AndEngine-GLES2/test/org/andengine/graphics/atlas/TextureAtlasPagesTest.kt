package org.andengine.graphics.atlas

import android.graphics.Bitmap
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.EmptyBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.opengl.texture.region.TextureRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextureAtlasPagesTest {

    private class RecordingTextureManager : TextureManager() {
        val loaded = ArrayList<ITexture>()
        val unloaded = ArrayList<ITexture>()

        override fun loadTexture(pTexture: ITexture): Boolean {
            loaded.add(pTexture)
            return super.loadTexture(pTexture)
        }

        override fun unloadTexture(pTexture: ITexture): Boolean {
            unloaded.add(pTexture)
            return super.unloadTexture(pTexture)
        }
    }

    private val manager = RecordingTextureManager()

    private fun pages(pageSize: Int = 64, maxRegion: Int = 32) = TextureAtlasPages(manager, pageSize, maxRegion)

    private fun source(w: Int, h: Int): IBitmapTextureAtlasSource = EmptyBitmapTextureAtlasSource(w, h)

    @Test
    fun add_withoutSession_returnsNull() {
        val atlas = pages()
        assertNull(atlas.add(source(8, 8), TextureOptions.BILINEAR))
        assertEquals(0, atlas.pageCount)
    }

    @Test
    fun add_largeImage_returnsNull() {
        val atlas = pages(pageSize = 128, maxRegion = 32)
        atlas.beginSession()
        // Over the area of 32x32, or longer than twice the size on one side.
        assertNull(atlas.add(source(33, 33), TextureOptions.BILINEAR))
        assertNull(atlas.add(source(65, 8), TextureOptions.BILINEAR))
        assertNull(atlas.add(source(8, 65), TextureOptions.BILINEAR))
        atlas.endSession()
        assertEquals(0, atlas.pageCount)
        assertTrue(manager.loaded.isEmpty())
    }

    @Test
    fun add_longThinImage_accepted() {
        val atlas = pages(pageSize = 128, maxRegion = 32)
        atlas.beginSession()
        // 64x16 = 1024 = 32x32 in area: accepted although wider than 32.
        val wide = atlas.add(source(64, 16), TextureOptions.BILINEAR)
        val tall = atlas.add(source(16, 64), TextureOptions.BILINEAR)
        atlas.endSession()
        assertNotNull(wide)
        assertNotNull(tall)
        assertSame(wide!!.texture, tall!!.texture)
    }

    @Test
    fun add_twoSmall_samePage() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(10, 12), TextureOptions.BILINEAR)!!
        val b = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertSame(a.texture, b.texture)
        assertEquals(1, atlas.pageCount)
        assertEquals(listOf(a.texture), manager.loaded)

        // The region covers the content only, inside the padding.
        assertEquals(10f, a.width, 0f)
        assertEquals(12f, a.height, 0f)
        assertEquals(2f, a.textureX, 0f)
        assertEquals(2f, a.textureY, 0f)
        assertEquals(2f / 64f, a.u, 1e-6f)
        assertEquals(12f / 64f, a.u2, 1e-6f)
        assertTrue(atlas.isAtlasRegion(a))
    }

    @Test
    fun add_equalOptionsInstances_samePage() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        val copy = TextureOptions(
            android.opengl.GLES20.GL_LINEAR, android.opengl.GLES20.GL_LINEAR,
            android.opengl.GLES20.GL_CLAMP_TO_EDGE, android.opengl.GLES20.GL_CLAMP_TO_EDGE, false
        )
        val b = atlas.add(source(8, 8), copy)!!
        val nearest = atlas.add(source(8, 8), TextureOptions.NEAREST)!!
        atlas.endSession()

        assertSame(a.texture, b.texture)
        assertNotSame(a.texture, nearest.texture)
        assertEquals(2, atlas.pageCount)
    }

    @Test
    fun add_sourceIsPlacedPadded() {
        val atlas = pages()
        atlas.beginSession()
        atlas.add(source(10, 12), TextureOptions.BILINEAR)!!
        atlas.endSession()

        val page = manager.loaded.single() as BitmapTextureAtlas
        val placed = sourcesOf(page).single()
        assertEquals(0, placed.textureX)
        assertEquals(0, placed.textureY)
        assertEquals(14, placed.textureWidth)
        assertEquals(16, placed.textureHeight)
    }

    @Test
    fun add_pageFull_opensNewPage() {
        val atlas = pages(pageSize = 64, maxRegion = 64)
        atlas.beginSession()
        val a = atlas.add(source(40, 40), TextureOptions.BILINEAR)!!
        val b = atlas.add(source(40, 40), TextureOptions.BILINEAR)!!

        // The full page is queued at once, the new one only when the session ends.
        assertEquals(listOf(a.texture), manager.loaded)
        atlas.endSession()

        assertNotSame(a.texture, b.texture)
        assertEquals(2, atlas.pageCount)
        assertEquals(listOf(a.texture, b.texture), manager.loaded)
    }

    @Test
    fun add_pageFull_laterSmallImageUsesNewPage() {
        val atlas = pages(pageSize = 64, maxRegion = 64)
        atlas.beginSession()
        val a = atlas.add(source(40, 40), TextureOptions.BILINEAR)!!
        val b = atlas.add(source(40, 40), TextureOptions.BILINEAR)!!
        val c = atlas.add(source(4, 4), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertNotSame(a.texture, c.texture)
        assertSame(b.texture, c.texture)
        assertEquals(2, manager.loaded.size)
    }

    @Test
    fun add_pageFull_otherOptionsPageStaysOpen() {
        val atlas = pages(pageSize = 64, maxRegion = 64)
        atlas.beginSession()
        val premultiplied = atlas.add(source(8, 8), TextureOptions.BILINEAR_PREMULTIPLYALPHA)!!
        val a = atlas.add(source(40, 40), TextureOptions.BILINEAR)!!
        atlas.add(source(40, 40), TextureOptions.BILINEAR)!!
        assertEquals(listOf(a.texture), manager.loaded)
        atlas.endSession()

        assertTrue(manager.loaded.contains(premultiplied.texture))
    }

    @Test
    fun add_tooLargeForEmptyPage_keepsOpenPageOpen() {
        val atlas = pages(pageSize = 64, maxRegion = 512)
        atlas.beginSession()
        atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        assertNull(atlas.add(source(62, 10), TextureOptions.BILINEAR))
        assertTrue(manager.loaded.isEmpty())
        atlas.endSession()
        assertEquals(1, manager.loaded.size)
    }

    @Test
    fun add_repeatOptions_returnsNull() {
        val atlas = pages()
        atlas.beginSession()
        assertNull(atlas.add(source(8, 8), TextureOptions.REPEATING_BILINEAR))
        assertNull(atlas.add(source(8, 8), TextureOptions.REPEATING_NEAREST_PREMULTIPLYALPHA))
        atlas.endSession()
        assertEquals(0, atlas.pageCount)
    }

    @Test
    fun add_regionLargerThanPage_returnsNull() {
        val atlas = pages(pageSize = 64, maxRegion = 512)
        atlas.beginSession()
        assertNull(atlas.add(source(62, 10), TextureOptions.BILINEAR))
        atlas.endSession()
        assertEquals(0, atlas.pageCount)
    }

    @Test
    fun uploadedPage_isClosedForNextSession() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        atlas.beginSession()
        val b = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertNotSame(a.texture, b.texture)
        assertEquals(2, manager.loaded.size)
    }

    @Test
    fun nestedSessions_uploadOnlyAtOutermostEnd() {
        val atlas = pages()
        atlas.beginSession()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()
        assertTrue(manager.loaded.isEmpty())
        val b = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertSame(a.texture, b.texture)
        assertEquals(listOf(a.texture), manager.loaded)
    }

    @Test
    fun pageRefCount_releaseLast_unloadsPage() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        val b = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertTrue(atlas.release(a))
        assertTrue(atlas.release(b))
        assertEquals(listOf(a.texture), manager.unloaded)
        assertEquals(0, atlas.pageCount)
    }

    @Test
    fun pageRefCount_releaseOne_keepsPage() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertTrue(atlas.release(a))
        assertTrue(manager.unloaded.isEmpty())
        assertEquals(1, atlas.pageCount)
    }

    @Test
    fun release_twice_unloadsOnce() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        val b = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertTrue(atlas.release(a))
        assertTrue(atlas.release(a))
        assertTrue(manager.unloaded.isEmpty())
        assertTrue(atlas.release(b))
        assertTrue(atlas.release(b))
        assertEquals(1, manager.unloaded.size)
    }

    @Test
    fun releaseAll_beforeUpload_neverUploadsPage() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        assertTrue(atlas.release(a))
        atlas.endSession()

        assertTrue(manager.loaded.isEmpty())
        assertTrue(manager.unloaded.isEmpty())
        assertEquals(0, atlas.pageCount)
    }

    @Test
    fun differentOptions_differentPages() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        val b = atlas.add(source(8, 8), TextureOptions.BILINEAR_PREMULTIPLYALPHA)!!
        val c = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        assertNotSame(a.texture, b.texture)
        assertSame(a.texture, c.texture)
        assertSame(TextureOptions.BILINEAR_PREMULTIPLYALPHA, b.texture.textureOptions)
        assertEquals(2, atlas.pageCount)
    }

    @Test
    fun release_nonAtlasRegion_returnsFalse() {
        val atlas = pages()
        val own = BitmapTextureAtlas(manager, 8, 8, TextureOptions.BILINEAR)
        val region = TextureRegion(own, 0f, 0f, 8f, 8f)
        assertFalse(atlas.release(region))
        assertFalse(atlas.isAtlasRegion(region))
        assertTrue(manager.unloaded.isEmpty())
    }

    @Test
    fun pageSize_limitedByDeviceMaximum() {
        val previous = TextureAtlasPages.deviceMaxTextureSize
        try {
            TextureAtlasPages.deviceMaxTextureSize = 32
            val atlas = pages(pageSize = 64, maxRegion = 64)
            atlas.beginSession()
            val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
            assertNull(atlas.add(source(40, 8), TextureOptions.BILINEAR))
            atlas.endSession()
            assertEquals(32, a.texture.width)
        } finally {
            TextureAtlasPages.deviceMaxTextureSize = previous
        }
    }

    @Test
    fun extrudedSource_duplicatesEdgePixels() {
        val inner = object : BaseTextureAtlasSource(0, 0, 2, 1), IBitmapTextureAtlasSource {
            override fun deepCopy(): IBitmapTextureAtlasSource = this
            override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap {
                val bmp = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
                bmp.setPixel(0, 0, 0xFFFF0000.toInt())
                bmp.setPixel(1, 0, 0xFF0000FF.toInt())
                return bmp
            }
        }
        val extruded = ExtrudedBitmapSource(inner, 1)
        assertEquals(4, extruded.textureWidth)
        assertEquals(3, extruded.textureHeight)

        val bmp = extruded.onLoadBitmap(Bitmap.Config.ARGB_8888)!!
        assertEquals(4, bmp.width)
        assertEquals(3, bmp.height)
        assertEquals(0xFFFF0000.toInt(), bmp.getPixel(0, 0))
        assertEquals(0xFFFF0000.toInt(), bmp.getPixel(1, 1))
        assertEquals(0xFF0000FF.toInt(), bmp.getPixel(2, 1))
        assertEquals(0xFF0000FF.toInt(), bmp.getPixel(3, 2))
    }

    @Suppress("UNCHECKED_CAST")
    private fun sourcesOf(page: BitmapTextureAtlas): List<IBitmapTextureAtlasSource> {
        val field = org.andengine.opengl.texture.atlas.TextureAtlas::class.java.getDeclaredField("mTextureAtlasSources")
        field.isAccessible = true
        return field.get(page) as List<IBitmapTextureAtlasSource>
    }

    @Test
    fun isAtlasRegion_copyAndReleasedRegion() {
        val atlas = pages()
        atlas.beginSession()
        val a = atlas.add(source(8, 8), TextureOptions.BILINEAR)!!
        atlas.endSession()

        val copy = a.deepCopy()
        assertTrue(atlas.isAtlasRegion(copy))
        assertTrue(atlas.release(copy))
        assertEquals(1, atlas.pageCount)

        assertTrue(atlas.release(a))
        assertTrue(atlas.isAtlasRegion(a))
        assertEquals(0, atlas.pageCount)
    }
}
