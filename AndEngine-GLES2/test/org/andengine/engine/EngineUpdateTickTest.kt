package org.andengine.engine

import org.andengine.engine.Engine.EngineLock
import org.andengine.engine.camera.Camera
import org.andengine.engine.handler.IUpdateHandler
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.DrawContext
import org.andengine.graphics.gles.GlesLegacyRestorers
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineUpdateTickTest {

    private lateinit var engine: Engine

    @Before
    fun setUp() {
        installRecordingGL()
        engine = Engine(
            EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f))
                .also { it.setEngineLock(EngineLock(false)) }
        )
    }

    @After
    fun tearDown() {
        GlesLegacyRestorers.clearForTest()
    }

    @Test(timeout = 5000)
    fun lastUpdateNanos_reflectsTheLastTick() {
        assertEquals(0L, engine.lastUpdateNanos)

        engine.registerUpdateHandler(object : IUpdateHandler {
            override fun onUpdate(pSecondsElapsed: Float) {
                Thread.sleep(TICK_MILLIS)
            }

            override fun reset() {}
        })
        engine.start()

        // The tick hands the frame to the draw side and then waits for it, so it runs on its own thread.
        val tick = Thread { engine.onTickUpdate() }
        tick.start()
        engine.onDrawFrame(DrawContext())
        tick.join()

        assertTrue(engine.lastUpdateNanos >= TICK_MILLIS * 1_000_000L)
    }

    private companion object {
        const val TICK_MILLIS = 5L
    }
}
