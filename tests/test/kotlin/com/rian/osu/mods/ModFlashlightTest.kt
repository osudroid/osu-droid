package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModFlashlightTest {
    @Test
    fun `Test serialization`() {
        ModFlashlight().apply {
            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(0.12f, getDouble("areaFollowDelay").toFloat())
            }

            followDelay = 0.36f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(0.36f, getDouble("areaFollowDelay").toFloat())
            }

            sizeMultiplier = 1.5f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(1.5f, getDouble("sizeMultiplier").toFloat())
            }
        }
    }

    @Test
    fun `Test toString`() {
        ModFlashlight().apply {
            Assert.assertEquals("FL", toString())

            followDelay = 0.36f

            Assert.assertEquals("FL (0.36s)", toString())

            followDelay = 0.12f
            sizeMultiplier = 1.5f

            Assert.assertEquals("FL (1.5x)", toString())

            followDelay = 0.36f

            Assert.assertEquals("FL (0.36s, 1.5x)", toString())
        }
    }
}