package org.andengine.opengl.texture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextureManagerContextLossTest {

    /** Records hardware loads, deletes and drops instead of calling GL. */
    private class FakeTexture(manager: TextureManager) :
        Texture(manager, PixelFormat.RGBA_8888, TextureOptions.BILINEAR, null) {

        var loads = 0
        var deletes = 0
        var drops = 0

        override fun getWidth() = 4
        override fun getHeight() = 4
        override fun writeTextureToHardware() = Unit

        override fun loadToHardware() {
            mHardwareTextureID = 1
            loads++
            mUpdateOnHardwareNeeded = false
        }

        override fun unloadFromHardware() {
            mHardwareTextureID = -1
            deletes++
        }

        override fun onDroppedWithoutUnload() {
            drops++
        }
    }

    private val manager = TextureManager()

    private fun loadedTexture() = FakeTexture(manager).also { manager.loadTextureNow(it) }

    @Test
    fun onReload_dropsQueuedUnloadsWithoutTouchingGl() {
        val kept = loadedTexture()
        val released = loadedTexture()
        manager.unloadTexture(released)

        manager.onReload()
        manager.updateTextures()

        assertEquals("dropped texture told once", 1, released.drops)
        assertEquals("no delete of a name of the lost context", 0, released.deletes)
        assertEquals("dropped texture not uploaded again", 1, released.loads)
        assertFalse(released.isLoadedToHardware)

        assertEquals(0, kept.drops)
        assertEquals(0, kept.deletes)
        assertEquals("kept texture uploaded again", 2, kept.loads)
        assertTrue(kept.isLoadedToHardware)

        // The drop is not repeated by a later context loss.
        manager.onReload()
        manager.updateTextures()
        assertEquals(1, released.drops)
        assertEquals(0, kept.drops)
    }

    @Test
    fun unloadTexture_beforeUpload_dropsWithoutTouchingGl() {
        val fresh = FakeTexture(manager)
        manager.loadTexture(fresh)
        manager.unloadTexture(fresh)

        // Re-queued for its upload by a context loss, then released before the next frame.
        val requeued = loadedTexture()
        manager.onReload()
        manager.unloadTexture(requeued)

        manager.updateTextures()

        assertEquals(1, fresh.drops)
        assertEquals(0, fresh.loads)
        assertEquals(0, fresh.deletes)
        assertEquals(1, requeued.drops)
        assertEquals("not uploaded again", 1, requeued.loads)
        assertEquals(0, requeued.deletes)

        // Neither is managed any more, so nothing repeats the drop.
        manager.unloadTexture(fresh)
        manager.onReload()
        assertEquals(1, fresh.drops)
        assertEquals(1, requeued.drops)
    }

    @Test
    fun unloadTexture_twiceBeforeContextLoss_dropsOnce() {
        val released = loadedTexture()
        manager.unloadTexture(released)
        manager.unloadTexture(released)

        manager.onReload()
        manager.updateTextures()

        assertEquals(1, released.drops)
        assertEquals(0, released.deletes)
        assertEquals(1, released.loads)
    }
}
