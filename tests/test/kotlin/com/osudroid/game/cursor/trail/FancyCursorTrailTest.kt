package com.osudroid.game.cursor.trail

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
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FancyCursorTrailTest {

    private val glCalls = ArrayList<String>()
    private val vertexUploads = ArrayList<ByteArray>()
    private var nextId = 1
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 800f, 480f)

    private fun fakeGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        glCalls += method.name + (args?.let { a -> if (method.name.startsWith("glBlendFunc")) "(" + a.joinToString() + ")" else "" } ?: "")
        when (method.name) {
            "glGetIntegerv" -> {
                val params = args!![1] as IntArray
                params[args[2] as Int] = when (args[0]) {
                    GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                    GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                    else -> 0
                }
            }
            "glGetShaderiv", "glGetProgramiv" -> (args!![2] as IntArray)[args[3] as Int] = 1
            "glGenBuffers", "glGenTextures", "glGenVertexArrays", "glGenSamplers", "glGenFramebuffers" -> {
                val ids = args!![1] as IntArray
                for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
            }
            "glBufferData" -> if (args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer) {
                val data = (args[2] as ByteBuffer).duplicate()
                data.position(0)
                val bytes = ByteArray(args[1] as Int)
                data.get(bytes)
                vertexUploads += bytes
            }
            "glBufferSubData" -> if (args!![0] == GLES20.GL_ARRAY_BUFFER) {
                val data = (args[3] as ByteBuffer).duplicate()
                data.position(0)
                val bytes = ByteArray(args[2] as Int)
                data.get(bytes)
                vertexUploads += bytes
            }
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (method.name == "glCreateShader" || method.name == "glCreateProgram") nextId++ else 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(fakeGl())
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        glCalls.clear()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
    }

    /** A trail with [points] control points; the cursor moves 10 units to the right per step. */
    private fun trail(points: Int): FancyCursorTrail {
        val trail = FancyCursorTrail()
        trail.cursorX = 100f
        trail.cursorY = 100f
        trail.update(0f)
        repeat(points) {
            trail.cursorX += 10f
            trail.update(0.001f)
        }
        return trail
    }

    @Test
    fun trail_drawsOneStrip() {
        trail(4).doDraw(glState, camera)

        assertEquals(1, glCalls.count { it == "glDrawArrays" })
        // 4 control points make 3 segments of 2 vertices.
        val upload = vertexUploads.last()
        assertEquals(6 * MeshRenderer.VERTEX_SIZE_BYTES, upload.size)
    }

    @Test
    fun trail_additiveBlend() {
        trail(3).doDraw(glState, camera)

        val additive = "glBlendFunc(${GLES20.GL_SRC_ALPHA}, ${GLES20.GL_ONE})"
        val separate = "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA}, ${GLES20.GL_ONE}, ${GLES20.GL_SRC_ALPHA}, ${GLES20.GL_ONE})"
        assertTrue(glCalls.toString(), glCalls.contains(additive) || glCalls.contains(separate))
    }

    @Test
    fun trail_perVertexAlpha() {
        val trail = trail(3)
        trail.update(0.1f)
        trail.doDraw(glState, camera)

        val upload = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        // Alpha is the top byte of the packed color; older pieces fade first.
        val first = upload.get(MeshRenderer.VERTEX_SIZE_BYTES * 0 + 11).toInt() and 0xFF
        val last = upload.get(MeshRenderer.VERTEX_SIZE_BYTES * 3 + 11).toInt() and 0xFF
        assertTrue("first=$first last=$last", first < last)
        assertTrue(first in 1..254)
    }
}
