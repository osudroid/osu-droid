package org.andengine.opengl.texture

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES30
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesTextures
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.opengl.texture.bitmap.BitmapTextureFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for the bitmaps of the atlas source.
@RunWith(RobolectricTestRunner::class)
class TextureLoadTest {

    private class FakeBitmapSource : BaseTextureAtlasSource(1, 2, 3, 2), IBitmapTextureAtlasSource {
        override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        override fun deepCopy() = FakeBitmapSource()
    }

    private lateinit var gl: RecordingGL

    @Before
    fun setUp() {
        gl = installRecordingGL()
    }

    private fun count(call: String) = gl.calls.count { it == call }

    private fun bind(id: Int) = "glBindTexture(${GLES20.GL_TEXTURE_2D},$id)"

    @Test
    fun texture_loadsWithoutGlState() {
        val manager = TextureManager()
        val atlas = BitmapTextureAtlas(manager, 16, 16, BitmapTextureFormat.RGBA_8888, TextureOptions.BILINEAR)
        atlas.addTextureAtlasSource(FakeBitmapSource(), 1, 2)

        manager.loadTexture(atlas)
        manager.updateTextures()

        assertTrue(atlas.isLoadedToHardware)
        val id = atlas.hardwareTextureID
        val generated = gl.calls.indexOf("glGenTextures(1,buf,0)")
        val bound = gl.calls.indexOf(bind(id))
        val storage = gl.calls.indexOf("glTexStorage2D(${GLES20.GL_TEXTURE_2D},1,${GLES30.GL_RGBA8},16,16)")
        val upload = gl.calls.indexOf("glTexSubImage2D(${GLES20.GL_TEXTURE_2D},0,1,2,3,2,${GLES20.GL_RGBA},${GLES20.GL_UNSIGNED_BYTE},ints)")
        val filter = gl.calls.indexOf("glTexParameteri(${GLES20.GL_TEXTURE_2D},${GLES20.GL_TEXTURE_MIN_FILTER},${GLES20.GL_LINEAR})")
        assertTrue(gl.calls.toString(), generated in 0 until bound)
        assertTrue(gl.calls.toString(), bound < storage && storage < upload && upload < filter)
        // Not a power-of-two bitmap: unpack alignment 1 around the upload, then back to the default.
        assertEquals("glPixelStorei(${GLES20.GL_UNPACK_ALIGNMENT},1)", gl.calls[upload - 1])
        assertEquals("glPixelStorei(${GLES20.GL_UNPACK_ALIGNMENT},${GlesTextures.UNPACK_ALIGNMENT_DEFAULT})", gl.calls[upload + 1])

        // Bound through the state cache: a second bind is skipped, another unit binds again.
        atlas.bind()
        assertEquals(1, count(bind(id)))
        atlas.bind(GLES20.GL_TEXTURE1)
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE1})", gl.calls[gl.calls.size - 2])
        assertEquals(2, count(bind(id)))

        manager.unloadTexture(atlas)
        manager.updateTextures()

        assertFalse(atlas.isLoadedToHardware)
        assertEquals(1, count("glDeleteTextures(1,buf,0)"))
        // The cache forgot the deleted name, so binding it again reaches GL.
        GlesTextures.bind(id)
        assertEquals(3, count(bind(id)))
    }

    @Test
    fun loadTextureNow_loadsAndUnloadsRightAway() {
        val manager = TextureManager()
        val atlas = BitmapTextureAtlas(manager, 16, 16, BitmapTextureFormat.RGBA_8888, TextureOptions.BILINEAR)
        atlas.addTextureAtlasSource(FakeBitmapSource(), 0, 0)

        assertTrue(manager.loadTextureNow(atlas))
        assertTrue(atlas.isLoadedToHardware)

        assertTrue(manager.unloadTextureNow(atlas))
        assertFalse(atlas.isLoadedToHardware)
        assertEquals(1, count("glDeleteTextures(1,buf,0)"))
    }

    @Test
    fun bind_goesThroughCache() {
        GlesTextures.bind(5)
        GlesTextures.bind(5)

        assertEquals(1, count(bind(5)))
    }

    @Test
    fun bind_isPerUnit() {
        GlesTextures.bind(5, GLES20.GL_TEXTURE1)
        GlesTextures.bind(5, GLES20.GL_TEXTURE0)
        GlesTextures.bind(5, GLES20.GL_TEXTURE1)

        assertEquals(1, GlesBackend.state.currentActiveTextureUnit)
        assertEquals(2, count(bind(5)))
    }

    @Test
    fun delete_forgetsBinding() {
        GlesTextures.bind(5)
        GlesTextures.delete(5)
        GlesTextures.bind(5)

        assertEquals(2, count(bind(5)))
    }

    @Test
    fun newContext_forgetsBindings() {
        GlesTextures.bind(5)
        GlesBackend.onContextCreated(gl)
        GlesTextures.bind(5)

        assertEquals(2, count(bind(5)))
    }
}
