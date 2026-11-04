package org.andengine.graphics.debug

import java.time.Duration
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock

// Robolectric for SystemClock, which closes the windows.
@RunWith(RobolectricTestRunner::class)
class RenderStatsTest {

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        installRecordingGL()
        RenderStats.reset()
        RenderStats.enabled = true
    }

    @After
    fun tearDown() {
        RenderStats.enabled = false
        RenderStats.listener = null
        RenderStats.reset()
    }

    private fun frame(renderWorkNanos: Long = -1L) {
        if (renderWorkNanos >= 0L) {
            RenderStats.onRenderWork(renderWorkNanos)
        }
        RenderStats.beginDraw()
        RenderStats.endDraw()
    }

    @Test
    fun summary_showsRenderThreadTimeAverageAndMaximum() {
        frame(4_000_000L)
        ShadowSystemClock.advanceBy(Duration.ofMillis(600))
        frame(6_000_000L)

        val summary = RenderStats.summary
        assertTrue(summary, summary.contains("  render 5.0/6.0  update "))
    }

    @Test
    fun onRenderWork_whileDisabled_isIgnored() {
        RenderStats.enabled = false
        RenderStats.onRenderWork(9_000_000L)
        RenderStats.enabled = true

        frame()
        ShadowSystemClock.advanceBy(Duration.ofMillis(600))
        frame()

        assertTrue(RenderStats.summary, RenderStats.summary.contains("  render 0.0/0.0  update "))
    }
}
