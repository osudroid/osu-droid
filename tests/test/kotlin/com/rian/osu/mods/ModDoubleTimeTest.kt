package com.rian.osu.mods

import org.junit.Assert.*
import org.junit.Test

class ModDoubleTimeTest {
    @Test
    fun `Test track rate multiplier`() {
        val mod = ModDoubleTime()

        assertEquals(1.5f, mod.trackRateMultiplier)
    }
}