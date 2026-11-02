package com.reco1l.andengine.texture

import android.opengl.GLES20
import android.os.Looper
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.lang.reflect.Proxy
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.TextureManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class VideoTextureTest {

    private var nextId = 100
    private val playerCalls = ArrayList<String>()
    private val surfaces = ArrayList<Surface>()
    private val surfacesValidWhenSet = ArrayList<Boolean>()
    private val surfacesValidWhenDetached = ArrayList<Boolean>()

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        String::class.java -> ""
        else -> null
    }

    private fun gl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        if (method.name == "glGetIntegerv") {
            val params = args!![1] as IntArray
            params[args[2] as Int] = when (args[0]) {
                GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                else -> 0
            }
        } else if (method.name.startsWith("glGen") && args?.size == 3 && args[1] is IntArray) {
            val ids = args[1] as IntArray
            for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
        }
        if (method.returnType == String::class.java && args?.getOrNull(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test"
        else defaultValue(method.returnType)
    } as GL32

    private fun player(): ExoPlayer =
        Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { _, method, args ->
            val surface = args?.getOrNull(0) as? Surface
            playerCalls += if (method.name == "setVideoSurface" && surface == null) "detachSurface" else method.name
            if (method.name == "setVideoSurface") {
                // Whether the surface was still valid when the player got it (or let go of it).
                surface?.let { surfaces += it; surfacesValidWhenSet += it.isValid }
                if (surface == null) surfacesValidWhenDetached += this.surfaces.lastOrNull()?.isValid ?: false
            }
            defaultValue(method.returnType)
        } as ExoPlayer

    private fun idleMain() = shadowOf(Looper.getMainLooper()).idle()

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(gl())
    }

    @Test
    fun reloadOnLiveContext_keepsPlayerAndStopsReloading() {
        val manager = TextureManager()
        val texture = VideoTexture("video.mp4", player())
        manager.loadTexture(texture)
        manager.updateTextures()
        idleMain()
        assertTrue(texture.isLoadedToHardware)
        val id = texture.hardwareTextureID

        // Returning to gameplay reloads every loaded texture on the live context.
        manager.reloadLoadedTextures()
        repeat(3) {
            manager.updateTextures()
            idleMain()
        }

        assertEquals(0, playerCalls.count { it == "release" })
        assertFalse(texture.isUpdateOnHardwareNeeded)
        assertTrue(texture.isLoadedToHardware)
        assertEquals(id, texture.hardwareTextureID)
    }

    @Test
    fun contextLoss_recreatesSurfaceWithoutReleasingPlayer() {
        val manager = TextureManager()
        val texture = VideoTexture("video.mp4", player())
        manager.loadTexture(texture)
        manager.updateTextures()
        idleMain()

        manager.onReload()
        repeat(2) {
            manager.updateTextures()
            idleMain()
        }

        assertEquals(0, playerCalls.count { it == "release" })
        assertEquals(2, playerCalls.count { it == "setVideoSurface" })
        // The old surface is detached from the player while still valid, before the new one is set;
        // releasing it while attached would abandon the decoder's output and stop playback.
        assertEquals(listOf("setVideoSurface", "detachSurface", "setVideoSurface"),
            playerCalls.filter { it == "setVideoSurface" || it == "detachSurface" })
        assertEquals(listOf(true), surfacesValidWhenDetached)
        assertEquals(listOf(true, true), surfacesValidWhenSet)
        // The surface of the lost context is released, the new one stays valid.
        assertEquals(2, surfaces.size)
        assertFalse(surfaces[0].isValid)
        assertTrue(surfaces[1].isValid)
        assertTrue(texture.isLoadedToHardware)
        assertFalse(texture.isUpdateOnHardwareNeeded)
    }

    @Test
    fun contextLossWhileUnloadQueued_releasesPlayerOnce() {
        val manager = TextureManager()
        val texture = VideoTexture("video.mp4", player())
        manager.loadTexture(texture)
        manager.updateTextures()
        idleMain()

        // Leaving the beatmap queues the unload; the context is lost before the next frame runs it.
        manager.unloadTexture(texture)
        manager.onReload()
        repeat(2) {
            manager.updateTextures()
            idleMain()
        }

        assertEquals(1, playerCalls.count { it == "release" })
        // The surface is detached before the player goes, and nothing is attached again.
        assertEquals(listOf("setVideoSurface", "detachSurface", "detachSurface", "release"),
            playerCalls.filter { it == "setVideoSurface" || it == "detachSurface" || it == "release" })
        assertEquals(1, surfaces.size)
        assertFalse(surfaces[0].isValid)
        assertFalse(texture.isLoadedToHardware)
    }

    @Test
    fun releaseBeforeFirstUpload_releasesPlayerOnce() {
        val manager = TextureManager()
        val texture = VideoTexture("video.mp4", player())
        manager.loadTexture(texture)

        // Released before the frame that would upload it.
        manager.unloadTexture(texture)
        repeat(2) {
            manager.updateTextures()
            idleMain()
        }

        assertEquals(1, playerCalls.count { it == "release" })
        assertEquals(0, playerCalls.count { it == "setVideoSurface" })
        assertTrue(surfaces.isEmpty())
        assertFalse(texture.isLoadedToHardware)
    }
}
