package com.reco1l.andengine.sprite

import android.opengl.GLES20
import org.andengine.engine.camera.Camera
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
class UISpriteDrawGuardTest {

    private val glCalls = ArrayList<String>()
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    /** Fake GL that records every call as `name(args)` and answers with neutral values. */
    private fun recordingGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        glCalls += "${method.name}(${args?.joinToString(",") { if (it is IntArray) "buf" else it.toString() } ?: ""})"
        if (method.name == "glGetIntegerv") {
            val params = args!![1] as IntArray
            params[args[2] as Int] = if (args[0] == GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS) 16 else 0
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    private class Page(id: Int) : BitmapTextureAtlas(TextureManager(), 16, 16, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = id
        }
    }

    private class RecordingSprite(region: TextureRegion?) : UISprite(region) {
        val calls = ArrayList<String>()
        // Records the buffer path's steps, so it must not take the batch path.
        override val isBatchable: Boolean get() = false
        override fun beginDraw(pGLState: GLState) { calls += "begin" }
        override fun onDeclarePointers(gl: GLState) { calls += "pointers" }
        override fun onDrawBuffer(pGLState: GLState) { calls += "draw" }
    }

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(recordingGl())
        glCalls.clear()
    }

    @Test
    fun unloadedTexture_skipsDrawWithoutTouchingState() {
        val sprite = RecordingSprite(TextureRegion(Page(-1), 2f, 2f, 8f, 8f))
        sprite.doDraw(glState, camera)

        assertTrue(sprite.calls.isEmpty())
        assertTrue(glCalls.isEmpty())
        assertFalse(glCalls.any { it.startsWith("glBindTexture") || it.startsWith("glDrawArrays") })
    }

    @Test
    fun loadedTexture_draws() {
        val sprite = RecordingSprite(TextureRegion(Page(7), 2f, 2f, 8f, 8f))
        sprite.doDraw(glState, camera)

        assertEquals(listOf("begin", "pointers", "draw"), sprite.calls)
    }

    @Test
    fun noRegion_runsBufferHooksInOrder() {
        val sprite = RecordingSprite(null)
        sprite.doDraw(glState, camera)

        assertEquals(listOf("begin", "pointers", "draw"), sprite.calls)
    }
}
