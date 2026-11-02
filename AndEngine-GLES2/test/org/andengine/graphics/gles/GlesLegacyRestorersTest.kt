package org.andengine.graphics.gles

import android.opengl.GLES20
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlesLegacyRestorersTest {

    @Before
    fun setUp() {
        val gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
    }

    @After
    fun tearDown() {
        GlesLegacyRestorers.clearForTest()
    }

    @Test
    fun legacyRestorers_noVboOrShaderEntries() {
        // The constructor registers the engine's managers.
        Engine(EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f)))

        val names = GlesLegacyRestorers.names
        assertTrue(names.toString(), "texture-manager" in names)
        assertTrue(names.toString(), "font-manager" in names)
        assertFalse(names.toString(), "shader-programs" in names)
        assertFalse(names.toString(), "vbo-manager" in names)
        assertFalse(names.toString(), "shader-program-manager" in names)
    }
}
