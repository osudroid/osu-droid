package com.reco1l.andengine.shape

import android.opengl.GLES20
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.osudroid.testing.endOpenPass

@RunWith(RobolectricTestRunner::class)
class UITriangleMeshTest {

    private lateinit var gl: RecordingGl
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        endOpenPass()
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

        assertEquals(1, gl.count("glDrawArrays"))
        val upload = gl.lastUpload()
        assertEquals(3 * MeshRenderer.VERTEX_SIZE_BYTES, upload.capacity())
        assertEquals(10f, upload.getFloat(MeshRenderer.VERTEX_SIZE_BYTES), 0f)
    }

    @Test
    fun mesh_depthInfoMapsToPipeline() {
        drawMesh(mesh(*triangle) { depthInfo = DepthInfo.Less })

        assertTrue(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_DEPTH_TEST})"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glDepthFunc(${GLES20.GL_LESS})"))
        // Later entities are not tested against the depth values the mesh wrote.
        assertFalse(GlesBackend.state.isDepthTestEnabled)
        assertEquals(1, gl.count("glDrawArrays"))
    }

    @Test
    fun mesh_clearInfoClearsFirst() {
        drawMesh(mesh(*triangle) {
            clearInfo = ClearInfo(depthBuffer = true, colorBuffer = false, stencilBuffer = false)
            depthInfo = DepthInfo.Less
        })

        val clear = gl.calls.indexOf("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})")
        val draw = gl.indexOfFirst("glDrawArrays(")
        assertTrue(gl.calls.toString(), clear in 0 until draw)
        // The depth buffer only clears while it is writable.
        assertTrue(gl.calls.toString(), gl.calls.subList(0, clear).contains("glDepthMask(true)"))
    }

    @Test
    fun emptyMesh_drawsNothing() {
        drawMesh(mesh { blendInfo = BlendInfo.Additive })

        assertEquals(0, gl.count("glDrawArrays"))
        assertTrue(gl.vertexUploads.isEmpty())
    }
}
