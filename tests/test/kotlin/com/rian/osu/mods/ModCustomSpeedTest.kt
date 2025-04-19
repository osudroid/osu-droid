package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModCustomSpeedTest {
    @Test
    fun `Test serialization`() {
        ModCustomSpeed().apply {
            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(1f, getDouble("rateMultiplier").toFloat())
            }

            trackRateMultiplier = 1.25f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(1.25f, getDouble("rateMultiplier").toFloat())
            }
        }
    }
}