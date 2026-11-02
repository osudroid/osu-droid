package org.andengine.graphics.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacingTest {

    @Test
    fun framePacing_defaults_matchTodaysBehaviour() {
        val pacing = FramePacing()

        assertTrue(pacing.vsyncEnabled)
        assertFalse(pacing.presentationTimeEnabled)
        assertFalse(pacing.choreographerPacingEnabled)
        assertEquals(1, pacing.takeSwapIntervalChange())
        assertEquals(-1, pacing.takeSwapIntervalChange())
    }

    @Test
    fun framePacing_surfaceRecreated_reappliesInterval() {
        val pacing = FramePacing()
        pacing.takeSwapIntervalChange()

        pacing.markSurfaceRecreated()

        assertEquals(1, pacing.takeSwapIntervalChange())
        assertEquals(-1, pacing.takeSwapIntervalChange())
    }

    @Test
    fun framePacing_disablingVsync_reportsZeroOnce() {
        val pacing = FramePacing()
        pacing.takeSwapIntervalChange()

        pacing.vsyncEnabled = false

        assertEquals(0, pacing.takeSwapIntervalChange())
        assertEquals(-1, pacing.takeSwapIntervalChange())
    }
}
