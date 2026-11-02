package com.reco1l.andengine.shape

import android.opengl.GLES20
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
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
class UITriangleMeshTest {

    private val glCalls = ArrayList<String>()
    private val vertexUploads = ArrayList<ByteArray>()
    private var nextId = 1
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private fun fakeGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        glCalls += method.name + (args?.let { a -> if (method.name in setOf("glDepthFunc", "glDepthMask", "glEnable", "glDisable", "glClear")) "(" + a.joinToString() + ")" else "" } ?: "")
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

    private fun mesh(vararg xy: Float, configure: UITriangleMesh.() -> Unit = {}): UITriangleMesh {
        val mesh = UITriangleMesh()
        mesh.vertices.ary = xy
        mesh.vertices.offset = 0
        mesh.vertices.length = xy.size
        mesh.configure()
        return mesh
    }

    private fun drawMesh(mesh: UITriangleMesh) {
        mesh.beginDraw(glState)
        mesh.doDraw(glState, camera)
    }

    private val triangle = floatArrayOf(0f, 0f, 10f, 0f, 0f, 10f)

    @Test
    fun mesh_drawsThroughMeshRenderer() {
        drawMesh(mesh(*triangle))

        assertTrue(MeshRenderer.vertexCounterIndex != -2)
        assertEquals(1, glCalls.count { it == "glDrawArrays" })
        val upload = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        assertEquals(3 * MeshRenderer.VERTEX_SIZE_BYTES, upload.capacity())
        assertEquals(10f, upload.getFloat(MeshRenderer.VERTEX_SIZE_BYTES), 0f)
    }

    @Test
    fun mesh_depthInfoMapsToPipeline() {
        drawMesh(mesh(*triangle) { depthInfo = DepthInfo.Less })

        assertTrue(glCalls.toString(), glCalls.contains("glEnable(${GLES20.GL_DEPTH_TEST})"))
        assertTrue(glCalls.toString(), glCalls.contains("glDepthFunc(${GLES20.GL_LESS})"))
        // Later entities are not tested against the depth values the mesh wrote.
        assertTrue(!GlesBackend.state.isDepthTestEnabled)
        assertEquals(1, glCalls.count { it == "glDrawArrays" })
    }

    @Test
    fun mesh_clearInfoClearsFirst() {
        drawMesh(mesh(*triangle) {
            clearInfo = ClearInfo(depthBuffer = true, colorBuffer = false, stencilBuffer = false)
            depthInfo = DepthInfo.Less
        })

        val clear = glCalls.indexOf("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})")
        val draw = glCalls.indexOf("glDrawArrays")
        assertTrue(glCalls.toString(), clear in 0 until draw)
        // The depth buffer only clears while it is writable.
        assertTrue(glCalls.toString(), glCalls.subList(0, clear).contains("glDepthMask(true)"))
    }

    @Test
    fun emptyMesh_drawsNothing() {
        drawMesh(mesh { blendInfo = BlendInfo.Additive })

        assertEquals(0, glCalls.count { it == "glDrawArrays" })
        assertTrue(vertexUploads.isEmpty())
    }
}
