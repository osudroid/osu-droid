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
            reflection = ModMirror.MirrorType.Horizontal

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("flippedAxes"))
                Assert.assertEquals(0, getInt("flippedAxes"))
            }

            reflection = ModMirror.MirrorType.Vertical

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("flippedAxes"))
                Assert.assertEquals(1, getInt("flippedAxes"))
            }

            reflection = ModMirror.MirrorType.Both

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("flippedAxes"))
                Assert.assertEquals(2, getInt("flippedAxes"))
            }
        }
    }

    @Test
    fun `Test toString`() {
        ModMirror().apply {
            reflection = ModMirror.MirrorType.Horizontal
            Assert.assertEquals("MR (↔)", toString())

            reflection = ModMirror.MirrorType.Vertical
            Assert.assertEquals("MR (↕)", toString())

            reflection = ModMirror.MirrorType.Both
            Assert.assertEquals("MR (↔, ↕)", toString())
        }
    }
}