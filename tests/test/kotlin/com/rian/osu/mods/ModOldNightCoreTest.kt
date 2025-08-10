package com.rian.osu.mods

import org.junit.Assert.*
import org.junit.Test

class ModOldNightCoreTest {
    @Test
    fun `Test track rate multiplier`() {
        val mod = ModOldNightCore()

        assertEquals(1.39f, mod.trackRateMultiplier)
    }
}