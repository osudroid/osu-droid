package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModRandomTest {
    @Test
    fun `Test serialization`() {
        ModRandom().apply {
            serialize().getJSONObject("settings").apply {
                Assert.assertFalse(has("seed"))
                Assert.assertTrue(has("angleSharpness"))
            }

            angleSharpness = 8f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(8f, getDouble("angleSharpness").toFloat(), 1e-2f)
            }

            seed = 100

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(100, getInt("seed"))
                Assert.assertEquals(8f, getDouble("angleSharpness").toFloat(), 1e-2f)
            }
        }
    }
}