package com.osudroid.mods

import org.junit.Assert.*
import org.junit.Test

class ModOldNightCoreTest {
    @Test
    fun `Test track rate multiplier`() {
        val mod = ModOldNightCore()

        assertEquals(1.3781248f, mod.trackRateMultiplier)
    }
}