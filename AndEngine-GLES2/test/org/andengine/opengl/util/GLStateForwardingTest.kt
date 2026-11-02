package org.andengine.opengl.util

import android.opengl.GLES20
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.opengl.Matrix (GLState's matrix stacks).
@RunWith(RobolectricTestRunner::class)
class GLStateForwardingTest {

    private lateinit var gl: RecordingGL
    private val glState = GLState()

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
        gl.calls.clear()
    }

    private fun count(call: String) = gl.calls.count { it == call }

    @Test
    fun bindTexture_goesThroughCache() {
        glState.bindTexture(5)
        glState.bindTexture(5)

        assertEquals(1, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
    }

    @Test
    fun stateSetViaCache_visibleToGLState() {
        GlesBackend.state.setBlendEnabled(true)

        assertTrue(glState.isBlendEnabled())
        // Already enabled through the cache, so GLState must not enable it again.
        assertTrue(glState.enableBlend())
        assertEquals(1, count("glEnable(${GLES20.GL_BLEND})"))
    }

    @Test
    fun capabilities_returnPreviousState_andDedup() {
        assertFalse(glState.enableScissorTest())
        assertTrue(glState.enableScissorTest())
        assertTrue(glState.disableScissorTest())
        assertFalse(glState.disableScissorTest())
        assertEquals(1, count("glEnable(${GLES20.GL_SCISSOR_TEST})"))
        assertEquals(1, count("glDisable(${GLES20.GL_SCISSOR_TEST})"))
        assertTrue(GlesBackend.state.isScissorEnabled == glState.isScissorTestEnabled())
    }

    @Test
    fun framebufferAndBlend_shareCacheWithBackend() {
        glState.bindFramebuffer(9)
        GlesBackend.state.bindFramebuffer(9)
        glState.blendFunction(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GlesBackend.state.blendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        assertEquals(1, count("glBindFramebuffer(${GLES20.GL_FRAMEBUFFER},9)"))
        assertEquals(1, count("glBlendFunc(${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"))
    }

    @Test
    fun activeTexture_isPerUnit() {
        glState.activeTexture(GLES20.GL_TEXTURE1)
        glState.bindTexture(5)
        glState.activeTexture(GLES20.GL_TEXTURE0)
        glState.bindTexture(5)
        glState.activeTexture(GLES20.GL_TEXTURE1)
        glState.bindTexture(5)

        assertEquals(GLES20.GL_TEXTURE1, glState.getActiveTexture())
        assertEquals(2, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
    }

    @Test
    fun deleteTexture_forgetsBinding() {
        glState.bindTexture(5)
        glState.deleteTexture(5)
        glState.bindTexture(5)

        assertEquals(2, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
    }

    @Test
    fun newContext_isSeenByGLState() {
        glState.bindTexture(5)
        GlesBackend.onContextCreatedForTest(gl)
        glState.bindTexture(5)

        assertEquals(2, count("glBindTexture(${GLES20.GL_TEXTURE_2D},5)"))
    }
}
