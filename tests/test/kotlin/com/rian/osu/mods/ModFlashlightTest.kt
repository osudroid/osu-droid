package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModFlashlightTest {
    @Test
    fun `Test toString`() {
        ModFlashlight().apply {
            Assert.assertEquals("FL", toString())

            followDelay = 0.36f

            Assert.assertEquals("FL (360ms)", toString())

            followDelay = 0.12f
            sizeMultiplier = 1.5f

            Assert.assertEquals("FL (1.5x)", toString())

            followDelay = 0.36f

            Assert.assertEquals("FL (360ms, 1.5x)", toString())
        }
    }
}