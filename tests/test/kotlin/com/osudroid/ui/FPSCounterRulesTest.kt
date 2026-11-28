package com.osudroid.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.Config

class FPSCounterRulesTest {
    @Test
    fun `Presented frames are used only in VSync modes`() {
        assertTrue(FPSCounterRules.usesPresentedFrames(Config.FRAME_LIMITER_VSYNC))
        assertTrue(FPSCounterRules.usesPresentedFrames(Config.FRAME_LIMITER_VSYNC_LOW_LATENCY))
        assertFalse(FPSCounterRules.usesPresentedFrames(Config.FRAME_LIMITER_UNLIMITED))
        listOf(2, 4, 8).forEach { assertFalse(FPSCounterRules.usesPresentedFrames(it)) }
    }

    @Test
    fun `Below target at under 80 percent of the aim on either rate`() {
        assertTrue(FPSCounterRules.isBelowTarget(95f, 1 / 120f, 120f))
        assertFalse(FPSCounterRules.isBelowTarget(97f, 1 / 120f, 120f))
        assertTrue(FPSCounterRules.isBelowTarget(120f, 1 / 90f, 120f))
        assertFalse(FPSCounterRules.isBelowTarget(120f, 0f, 120f))
    }

    @Test
    fun `One dropped frame at 120 Hz is not an FPS spike`() {
        assertFalse(FPSCounterRules.isFpsSpike(120f, 0.0167f))
        assertTrue(FPSCounterRules.isFpsSpike(120f, 0.025f))
        assertFalse(FPSCounterRules.isFpsSpike(40f, 0.03f))
    }

    @Test
    fun `Frame time spike needs a calm display and a long frame`() {
        assertTrue(FPSCounterRules.isFrameTimeSpike(0.008f, 0.03f))
        assertFalse(FPSCounterRules.isFrameTimeSpike(0.025f, 0.03f))
        assertFalse(FPSCounterRules.isFrameTimeSpike(0.008f, 0.015f))
    }

    @Test
    fun `Presented clock is used only once it reports a frame rate`() {
        assertFalse(FPSCounterRules.usesPresentedClock(
            shouldTrack = false,
            isAvailable = true,
            presentedFramesPerSecond = 120f
        ))

        assertFalse(FPSCounterRules.usesPresentedClock(
            shouldTrack = true,
            isAvailable = false,
            presentedFramesPerSecond = 120f
        ))

        assertFalse(FPSCounterRules.usesPresentedClock(
            shouldTrack = true,
            isAvailable = true,
            presentedFramesPerSecond = 0f
        ))

        assertTrue(FPSCounterRules.usesPresentedClock(
            shouldTrack = true,
            isAvailable = true,
            presentedFramesPerSecond = 120f
        ))
    }
}
