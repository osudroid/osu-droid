package com.rian.osu.mods

import org.junit.Assert.*
import org.junit.Test

class ModHalfTimeTest {
    @Test
    fun `Test track rate multiplier`() {
        val mod = ModHalfTime()

        assertEquals(0.75f, mod.trackRateMultiplier)
    }
}