package ru.nsu.ccfit.zuev.osu

import com.osudroid.FrameLimiterMode
import com.osudroid.testing.RecordingGl
import org.andengine.engine.CountingEngineLock
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.graphics.DrawContext
import org.andengine.graphics.gles.GlesLegacyRestorers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameLimiterSettingTest {

    @After
    fun tearDown() {
        GlesLegacyRestorers.clearForTest()
    }

    private fun assertMode(
        frameLimiter: Int,
        refreshRate: Float,
        vsync: Boolean,
        presentationTime: Boolean,
        choreographer: Boolean,
        releaseAfterSwap: Boolean,
        maximumUpdateHz: Float
    ) {
        assertEquals(
            FrameLimiterMode(vsync, presentationTime, choreographer, releaseAfterSwap, maximumUpdateHz),
            FrameLimiterMode.of(frameLimiter, refreshRate)
        )
    }

    @Test
    fun parseFrameLimiter_invalidFallsBackToVsync() {
        assertEquals(1, Config.parseFrameLimiter("x"))
        assertEquals(1, Config.parseFrameLimiter("-2"))
        assertEquals(1, Config.parseFrameLimiter(""))
    }

    @Test
    fun parseFrameLimiter_validValuesAreKept() {
        for (value in intArrayOf(1, -1, 2, 4, 8, 0)) {
            assertEquals(value, Config.parseFrameLimiter(value.toString()))
        }
    }

    @Test
    fun vsync_at60Hz() {
        assertMode(1, 60f, vsync = true, presentationTime = false, choreographer = false, releaseAfterSwap = false, maximumUpdateHz = 1000f)
    }

    @Test
    fun vsyncLowLatency_at120Hz() {
        assertMode(-1, 120f, vsync = true, presentationTime = true, choreographer = true, releaseAfterSwap = true, maximumUpdateHz = 1000f)
    }

    @Test
    fun twiceRefreshRate_at60Hz() {
        assertMode(2, 60f, vsync = false, presentationTime = true, choreographer = false, releaseAfterSwap = false, maximumUpdateHz = 120f)
    }

    @Test
    fun eightTimesRefreshRate_at144Hz_isCappedByTheHardLimit() {
        assertMode(8, 144f, vsync = false, presentationTime = true, choreographer = false, releaseAfterSwap = false, maximumUpdateHz = 1000f)
    }

    @Test
    fun unlimited() {
        assertMode(0, 60f, vsync = false, presentationTime = true, choreographer = false, releaseAfterSwap = false, maximumUpdateHz = 1000f)
    }

    @Test
    fun frameLimiter_refreshRateChange_updatesLimit() {
        assertEquals(240f, FrameLimiterMode.of(4, 60f).maximumUpdateHz)
        assertEquals(480f, FrameLimiterMode.of(4, 120f).maximumUpdateHz)
    }

    @Test
    fun frameLimiter_switchingModes_neverStrandsUpdate() {
        RecordingGl().install()
        val lock = CountingEngineLock()
        val engine = Engine(
            EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, FillResolutionPolicy(), Camera(0f, 0f, 800f, 480f))
                .also { it.setEngineLock(lock) }
        )

        fun drawFrame(frameLimiter: Int) {
            engine.setReleaseUpdateAfterSwap(FrameLimiterMode.of(frameLimiter, 60f).releaseUpdateAfterSwap)
            lock.handOverToDraw()
            engine.onDrawFrame(DrawContext())
        }

        // The low latency frame is never swapped before the setting changes back to plain vertical sync.
        drawFrame(Config.FRAME_LIMITER_VSYNC_LOW_LATENCY)
        drawFrame(Config.FRAME_LIMITER_VSYNC)
        engine.onFrameSwapped()

        assertTrue(lock.updateMayRun())
        assertEquals(1, lock.released)
    }
}
