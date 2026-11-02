package org.andengine.entity.primitive

import android.opengl.GLES20
import org.andengine.engine.camera.Camera
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.arrayDraws
import org.andengine.graphics.gles.disposeShapeMasks
import org.andengine.graphics.gles.draws
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.gles.installShapeMasks
import org.andengine.graphics.gles.lastVertices
import org.andengine.graphics.shape.ShapeMasks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.andengine.graphics.gles.endOpenPass

@RunWith(RobolectricTestRunner::class)
class RectangleBatchingTest {

    private lateinit var gl: RecordingGL
    private lateinit var masks: ShapeMasks
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    @Before
    fun setUp() {
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        masks = installShapeMasks()
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
        disposeShapeMasks(masks)
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
    }

    private fun rect(x: Float = 10f) = Rectangle(x, 0f, 8f, 6f)

    @Test
    fun plainRectangle_addsOneBatchedQuad() {
        rect().onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, gl.arrayDraws())
        QuadBatcher.flush()
        assertEquals(1, gl.draws())
        val v = gl.lastVertices()
        assertEquals(10f, v.getFloat(0), 0f)
        assertEquals(0f, v.getFloat(4), 0f)
        assertEquals(18f, v.getFloat(48), 0f)
        assertEquals(6f, v.getFloat(52), 0f)
    }

    @Test
    fun rectangleColorAndBlend_carriedOver() {
        val r = rect()
        r.setColor(1f, 0f, 0f, 0.5f)
        r.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        r.onDraw(glState, camera)
        QuadBatcher.flush()

        val v = gl.lastVertices()
        val packed = r.color.abgrPackedInt
        assertEquals(packed and 0xFF, v.get(16).toInt() and 0xFF)
        assertEquals((packed shr 8) and 0xFF, v.get(17).toInt() and 0xFF)
        assertEquals((packed shr 16) and 0xFF, v.get(18).toInt() and 0xFF)
        assertEquals((packed ushr 24) and 0xFF, v.get(19).toInt() and 0xFF)
        assertEquals(127, v.get(19).toInt() and 0xFF)
        val expected = "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun transparentRectangle_addsNothing() {
        val r = rect()
        r.setColor(1f, 1f, 1f, 0f)
        r.onDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(0, gl.arrayDraws())
    }

    @Test
    fun rectangle_colorReadAtDraw() {
        val r = rect()
        r.setColor(1f, 0f, 0f, 1f)
        r.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(255, gl.lastVertices().get(16).toInt() and 0xFF)
        assertEquals(0, gl.lastVertices().get(18).toInt() and 0xFF)

        r.setColor(0f, 0f, 1f)
        r.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(0, gl.lastVertices().get(16).toInt() and 0xFF)
        assertEquals(255, gl.lastVertices().get(18).toInt() and 0xFF)
    }

    @Test
    fun rectangle_sizeReadAtDraw() {
        val r = rect()
        r.setSize(20f, 4f)
        r.onDraw(glState, camera)
        QuadBatcher.flush()
        assertEquals(30f, gl.lastVertices().getFloat(48), 0f)
        assertEquals(4f, gl.lastVertices().getFloat(52), 0f)
    }

    @Test
    fun rectangleSubclassOverridingDraw_isBatched() {
        var ownDraws = 0
        val r = object : Rectangle(0f, 0f, 8f, 6f) {
            override fun draw(pGLState: DrawContext, pCamera: Camera) {
                ownDraws++
                super.draw(pGLState, pCamera)
            }
        }
        r.onDraw(glState, camera)
        assertEquals(1, ownDraws)
        assertTrue(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertEquals(0, gl.arrayDraws())
        assertEquals(1, gl.draws())
    }

    @Test
    fun rectangle_unknownBlendFallsBackToAlpha() {
        val r = rect()
        r.setBlendFunction(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE)
        r.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(1, gl.draws())
        val alpha = "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == alpha })
    }
}
