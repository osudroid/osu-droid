package com.osudroid.ui.v2.menu.main

import android.opengl.GLES20
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KiaiCatJamSpriteTest {

    private val glCalls = ArrayList<String>()
    private val uploads = ArrayList<ByteArray>()
    private val sources = ArrayList<String>()
    private var nextId = 100
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private fun recordingGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        val name = method.name
        glCalls += "$name(${args?.joinToString(",") { if (it is IntArray || it is java.nio.Buffer) "buf" else "$it" } ?: ""})"
        if (name == "glGetIntegerv") {
            val params = args!![1] as IntArray
            params[args[2] as Int] = when (args[0]) {
                GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                else -> 0
            }
        } else if (name.startsWith("glGen") && args?.size == 3 && args[1] is IntArray) {
            val ids = args[1] as IntArray
            for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
        } else if ((name == "glGetShaderiv" || name == "glGetProgramiv") && args!![2] is IntArray) {
            (args[2] as IntArray)[args[3] as Int] = 1
        } else if (name == "glShaderSource") {
            sources += args!![1].toString()
        } else if (name == "glBufferData" && args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer) {
            val copy = (args[2] as ByteBuffer).duplicate()
            val bytes = ByteArray(minOf(args[1] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        } else if (name == "glBufferSubData" && args!![3] is ByteBuffer) {
            val copy = (args[3] as ByteBuffer).duplicate()
            val bytes = ByteArray(minOf(args[2] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (name.startsWith("glCreate")) nextId++ else 0
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

    private fun frames(page: Page) = arrayOf(
        TextureRegion(page, 0f, 0f, 4f, 4f),
        TextureRegion(page, 4f, 8f, 4f, 4f)
    )

    private fun cat(page: Page): KiaiCatJamSprite =
        KiaiCatJamSprite(frames(page)).apply {
            setSize(40f, 40f)
            alpha = 1f
            onHandleInvalidations()
        }

    private fun programDraws() = glCalls.count { it.startsWith("glDrawArrays") }

    /** The vertices of the last mesh upload: per vertex x, y, u, v, lx, ly and the color int. */
    private fun lastMesh(): ByteBuffer {
        val bytes = uploads.last { it.size == 4 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES }
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
    }

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(recordingGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        glCalls.clear()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        KiaiCatJamSprite.releaseProgram()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
    }

    @Test
    fun catJam_drawsThroughMeshProgramWithCircleUniforms() {
        val sprite = cat(Page(7)).apply { clipRadius = 0.4f }
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 1, programDraws())
        assertFalse(QuadBatcher.hasPending)
        // Circle clip and premultiplied output of the fragment shader.
        val fragment = sources.last { it.contains("smoothstep") }
        assertTrue(fragment, fragment.contains("uniform float uRadius"))
        assertTrue(fragment, fragment.contains("vLocal"))
        assertTrue(glCalls.toString(), glCalls.contains("glUniform1f(0,0.4)"))
        assertTrue(glCalls.toString(), glCalls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        // Premultiplied blending, as the fragment shader writes premultiplied color.
        assertTrue(glCalls.toString(), glCalls.contains("glBlendFunc(${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})") ||
            glCalls.contains("glBlendFuncSeparate(${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"))

        // Full quad, local coordinates 0..1 across it, region UVs of the first frame.
        val bytes = lastMesh()
        val stride = MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES
        val corners = listOf(0f to 0f, 0f to 1f, 1f to 0f, 1f to 1f)
        for ((i, c) in corners.withIndex()) {
            assertEquals(c.first * 40f, bytes.getFloat(i * stride), 1e-4f)
            assertEquals(c.second * 40f, bytes.getFloat(i * stride + 4), 1e-4f)
            assertEquals(c.first, bytes.getFloat(i * stride + 16), 0f)
            assertEquals(c.second, bytes.getFloat(i * stride + 20), 0f)
        }
    }

    @Test
    fun catJam_frameChangesUvs() {
        val sprite = cat(Page(7))
        sprite.doDraw(glState, camera)
        val first = lastMesh()
        val u0 = first.getFloat(8)
        val v0 = first.getFloat(12)
        val u1 = first.getFloat(2 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES + 8)

        sprite.textureRegion = sprite.frames[1]
        sprite.onHandleInvalidations()
        sprite.doDraw(glState, camera)
        val second = lastMesh()

        assertEquals(0f, u0, 1e-6f)
        assertEquals(0f, v0, 1e-6f)
        assertEquals(4f / 16f, u1, 1e-6f)
        assertEquals(4f / 16f, second.getFloat(8), 1e-6f)
        assertEquals(8f / 16f, second.getFloat(12), 1e-6f)
        assertEquals(8f / 16f, second.getFloat(2 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES + 8), 1e-6f)
        assertEquals(12f / 16f, second.getFloat(MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES + 12), 1e-6f)
    }

    @Test
    fun catJam_notDrawnUntilTextureLoaded() {
        val sprite = cat(Page(-1))
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 0, programDraws())
        assertFalse(QuadBatcher.hasPending)
    }
}
