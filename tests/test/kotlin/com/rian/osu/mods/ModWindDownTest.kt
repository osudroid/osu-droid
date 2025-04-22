package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test

class ModWindDownTest {
    @Test
    fun `Test initial rate higher than final rate`() {
        ModWindDown().apply {
            initialRate = 1f
            finalRate = 0.75f

            Assert.assertTrue(initialRate > finalRate)

            initialRate = 0.7f

            Assert.assertTrue(initialRate > finalRate)
        }
    }
}