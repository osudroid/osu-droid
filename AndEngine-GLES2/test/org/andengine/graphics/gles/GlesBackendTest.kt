package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.graphics.atlas.TextureAtlasPages
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log in GlesBackend.
@RunWith(RobolectricTestRunner::class)
class GlesBackendTest {

    private lateinit var gl: RecordingGL
    private var previousDeviceMaxTextureSize = 0

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        previousDeviceMaxTextureSize = TextureAtlasPages.deviceMaxTextureSize
        gl = installRecordingGL()
    }

    @After
    fun tearDown() {
        TextureAtlasPages.deviceMaxTextureSize = previousDeviceMaxTextureSize
    }

    @Test
    fun contextCreated_readsMaxTextureSize() {
        assertEquals(4096, GlesBackend.caps.maxTextureSize)
    }

    @Test
    fun frameStart_resetsDrawState() {
        GlesBackend.resetDrawState(false)

        // Each state of a fresh cache is set once.
        assertEquals(
            listOf(
                "glBindVertexArray(0)",
                "glDisable(${GLES20.GL_CULL_FACE})",
                "glDisable(${GLES20.GL_DEPTH_TEST})",
                "glClearDepthf(1.0)",
                "glDepthFunc(${GLES20.GL_LESS})",
                "glDepthMask(true)",
                "glEnable(${GLES20.GL_BLEND})",
                "glDisable(${GLES20.GL_DITHER})"
            ),
            gl.calls
        )
        val state = GlesBackend.state
        assertEquals(0, state.currentVertexArray)
        assertFalse(state.isDitherEnabled)
        assertFalse(state.isDepthTestEnabled)
        assertTrue(state.isBlendEnabled)
        assertFalse(state.isCullFaceEnabled)
        assertFalse(state.isScissorEnabled)
        assertEquals(4096, TextureAtlasPages.deviceMaxTextureSize)

        // Every frame clears the depth buffer with the depth mask on, which a batch flush may have
        // turned off.
        gl.calls.clear()
        GlesBackend.beginFrame(false)
        assertEquals(listOf("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})"), gl.calls)

        state.depthMask(false)
        gl.calls.clear()
        GlesBackend.beginFrame(true)
        assertEquals(
            listOf("glClear($COVERAGE_BUFFER_BIT)", "glDepthMask(true)", "glClear(${GLES20.GL_DEPTH_BUFFER_BIT})"),
            gl.calls
        )
    }

    @Test
    fun frameStart_keepsDitherWhenEnabled() {
        GlesBackend.resetDrawState(true)

        assertTrue(GlesBackend.state.isDitherEnabled)
        assertEquals(1, gl.calls.count { it == "glEnable(${GLES20.GL_DITHER})" })
        assertEquals(0, gl.calls.count { it == "glDisable(${GLES20.GL_DITHER})" })
    }

    private companion object {
        const val COVERAGE_BUFFER_BIT = 0x8000
    }
}
