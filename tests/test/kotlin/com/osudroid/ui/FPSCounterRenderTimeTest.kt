package com.osudroid.ui

import com.osudroid.testing.RecordingGl
import com.osudroid.testing.disposeTestFontAndMasks
import com.osudroid.testing.installTestFontAndMasks
import com.osudroid.testing.endOpenPass
import com.rian.andengine.timing.ManualClock
import com.rian.andengine.timing.ThrottledFrameClock
import org.andengine.graphics.text.Font
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.view.RenderTiming
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.Config

@RunWith(RobolectricTestRunner::class)
class FPSCounterRenderTimeTest {

    private lateinit var gl: RecordingGl
    private lateinit var font: Font
    private lateinit var masks: ShapeMasks
    private lateinit var source: ManualClock
    private lateinit var clock: ThrottledFrameClock
    private lateinit var counter: FPSCounter
    private var wasShowingFps = true

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        val fixture = installTestFontAndMasks()
        font = fixture.font
        masks = fixture.masks

        wasShowingFps = Config.isShowFPS()
        Config.setShowFPS(true)
        RenderTiming.lastWorkNanos = 0L

        source = ManualClock().apply { isRunning = true }
        clock = ThrottledFrameClock().apply { throttling = false }
        clock.changeSource(source)
        counter = FPSCounter({ clock }, FPSCounter.RefreshRateSource { 60f })
        counter.updateClock(clock)
    }

    @After
    fun tearDown() {
        Config.setShowFPS(wasShowingFps)
        RenderTiming.lastWorkNanos = 0L
        endOpenPass()
        disposeTestFontAndMasks(font, masks)
    }

    // Advances the update clock by 16 ms per frame, the way the engine does before the scene updates.
    private fun runFrames(count: Int) {
        repeat(count) {
            source.currentTime += 0.016f
            clock.processFrame()
            counter.onUpdate(0.016f)
        }
    }

    @Test
    fun text_includesRenderTime() {
        RenderTiming.lastWorkNanos = 9_000_000L

        runFrames(200)

        assertTrue(counter.text, counter.text.endsWith("· R 9 ms"))
    }

    @Test
    fun renderTime_zero_showsZero() {
        runFrames(200)

        assertTrue(counter.text, counter.text.endsWith("· R 0 ms"))
        assertEquals(true, counter.text.contains(" ms | "))
    }
}
