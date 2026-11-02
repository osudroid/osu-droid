package org.andengine.opengl.texture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureManagerLiveReloadTest {

    /** Records hardware loads and deletes instead of calling GL. */
    private class FakeTexture(manager: TextureManager) :
        Texture(manager, PixelFormat.RGBA_8888, TextureOptions.BILINEAR, null) {

        var loaded = false
        var loads = 0
        var deletes = 0

        override fun getWidth() = 4
        override fun getHeight() = 4
        override fun writeTextureToHardware() = Unit
        override fun isLoadedToHardware() = loaded

        override fun loadToHardware() {
            loaded = true
            loads++
            mUpdateOnHardwareNeeded = false
        }

        override fun unloadFromHardware() {
            loaded = false
            deletes++
        }
    }

    private val manager = TextureManager()

    private fun loadedTexture() = FakeTexture(manager).also { manager.loadTextureNow(it) }

    @Test
    fun reloadLoadedTextures_deletesBeforeUploadingAgain() {
        val texture = loadedTexture()

        manager.reloadLoadedTextures()
        manager.updateTextures()

        assertEquals("old texture name deleted", 1, texture.deletes)
        assertEquals(2, texture.loads)
        assertTrue(texture.isLoadedToHardware)
    }

    @Test
    fun reloadLoadedTextures_keepsPendingUnloads() {
        val kept = loadedTexture()
        val released = loadedTexture()
        manager.unloadTexture(released)

        manager.reloadLoadedTextures()
        manager.updateTextures()

        assertEquals("released texture deleted, not leaked", 1, released.deletes)
        assertEquals("released texture not uploaded again", 1, released.loads)
        assertFalse(released.isLoadedToHardware)
        assertEquals(1, kept.deletes)
    }
}
