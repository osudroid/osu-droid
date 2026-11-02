package org.andengine.graphics.gles

import android.opengl.GLES11Ext
import android.opengl.GLES20
import org.andengine.graphics.atlas.TextureAtlasPages
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log in GlesBackend.
@RunWith(RobolectricTestRunner::class)
class GlesTexturesTest {

    private val oes = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
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
    fun setUpExternal_bindsThroughCacheThenSetsLinearClampedParameters() {
        GlesTextures.setUpExternal(7)

        assertEquals(
            listOf(
                "glBindTexture($oes,7)",
                "glTexParameteri($oes,${GLES20.GL_TEXTURE_MIN_FILTER},${GLES20.GL_LINEAR})",
                "glTexParameteri($oes,${GLES20.GL_TEXTURE_MAG_FILTER},${GLES20.GL_LINEAR})",
                "glTexParameteri($oes,${GLES20.GL_TEXTURE_WRAP_S},${GLES20.GL_CLAMP_TO_EDGE})",
                "glTexParameteri($oes,${GLES20.GL_TEXTURE_WRAP_T},${GLES20.GL_CLAMP_TO_EDGE})",
            ),
            gl.calls,
        )

        // The bind went through the state cache: binding the same texture again is skipped.
        gl.calls.clear()
        GlesBackend.state.bindTexture(oes, 7)
        assertEquals(emptyList<String>(), gl.calls)
    }
}
