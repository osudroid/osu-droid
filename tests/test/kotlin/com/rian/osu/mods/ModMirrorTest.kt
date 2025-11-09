package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test

class ModMirrorTest {
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