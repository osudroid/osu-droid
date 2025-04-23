package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test

class ModWindUpTest {
    @Test
    fun `Test initial rate lower than final rate`() {
        ModWindUp().apply {
            initialRate = 1f
            finalRate = 1.5f

            Assert.assertTrue(initialRate < finalRate)

            initialRate = 1.6f

            Assert.assertTrue(initialRate < finalRate)
        }
    }
}