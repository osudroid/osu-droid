package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModCustomSpeedTest {
    @Test
    fun `Test toString`() {
        ModCustomSpeed().apply {
            trackRateMultiplier = 1.25f

            Assert.assertEquals("CS (1.25x)", toString())
        }
    }
}