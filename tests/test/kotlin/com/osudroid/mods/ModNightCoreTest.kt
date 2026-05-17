package com.osudroid.mods

import org.junit.Assert.*
import org.junit.Test

class ModNightCoreTest {
    @Test
    fun `Test track rate multiplier`() {
        val mod = ModNightCore()

        assertEquals(1.5f, mod.trackRateMultiplier)
    }
}