package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModMirrorTest {
    @Test
    fun `Test serialization`() {
        ModMirror().apply {
            flipHorizontally = false
            flipVertically = false

            serialize().apply {
                Assert.assertFalse(has("settings"))
            }

            flipHorizontally = true

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("flippedAxes"))
                Assert.assertEquals(0, getInt("flippedAxes"))
            }

            flipHorizontally = false
            flipVertically = true

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("flippedAxes"))
                Assert.assertEquals(1, getInt("flippedAxes"))
            }

            flipHorizontally = true

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("flippedAxes"))
                Assert.assertEquals(2, getInt("flippedAxes"))
            }
        }
    }

    @Test
    fun `Test toString`() {
        ModMirror().apply {
            flipHorizontally = false
            flipVertically = false

            Assert.assertEquals("MR", toString())

            flipHorizontally = true

            Assert.assertEquals("MR (↔)", toString())

            flipHorizontally = false
            flipVertically = true

            Assert.assertEquals("MR (↕)", toString())

            flipHorizontally = true

            Assert.assertEquals("MR (↔, ↕)", toString())
        }
    }
}