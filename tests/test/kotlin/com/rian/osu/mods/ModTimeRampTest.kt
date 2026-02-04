package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test

class ModTimeRampTest {
    @Test
    fun `Test toString`() {
        Assert.assertEquals("TS (1.00x - 1.50x)", TestModTimeRamp().toString())
    }
}

private class TestModTimeRamp : ModTimeRamp() {
    override val name = "Test"
    override val acronym = "TS"
    override val description = "Test mod"
    override val type = ModType.Fun

    override var initialRate = 1f
    override var finalRate = 1.5f
}