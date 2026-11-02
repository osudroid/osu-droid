package org.andengine.graphics.gles

import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlesLegacyRestorersTest {

    @Before
    fun setUp() {
        installRecordingGL()
    }

    @After
    fun tearDown() {
        GlesLegacyRestorers.clearForTest()
    }

    @Test
    fun engine_registersTextureAndFontManagers() {
        // The constructor registers the engine's managers.
        Engine(EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f)))

        val names = GlesLegacyRestorers.names
        assertTrue(names.toString(), "texture-manager" in names)
        assertTrue(names.toString(), "font-manager" in names)
    }
}
