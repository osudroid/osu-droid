package com.osudroid.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModSynesthesiaTest {
    @Test
    fun `Test beat divisor combo color`() {
        data class Case(val beatDivisor: Int, val red: Int, val green: Int, val blue: Int)

        listOf(
            // Common beat divisors
            Case(1, 255, 255, 255),
            Case(2, 237, 17, 33),
            Case(3, 136, 102, 238),
            Case(4, 102, 204, 255),
            Case(6, 238, 170, 0),
            Case(8, 255, 204, 34),
            Case(12, 204, 102, 0),
            Case(16, 68, 17, 136),
            // Uncommon beat divisors
            Case(5, 255, 0, 0),
            Case(7, 255, 0, 0),
            Case(9, 255, 0, 0),
            Case(10, 255, 0, 0),
            Case(11, 255, 0, 0),
            Case(13, 255, 0, 0),
            Case(14, 255, 0, 0),
            Case(15, 255, 0, 0)
        ).forEach { (beatDivisor, red, green, blue) ->
            ModSynesthesia.getColorFor(beatDivisor).let { color ->
                Assert.assertEquals("Invalid red for $beatDivisor", red / 255f, color.red)
                Assert.assertEquals("Invalid green for $beatDivisor", green / 255f, color.green)
                Assert.assertEquals("Invalid blue for $beatDivisor", blue / 255f, color.blue)
            }
        }
    }
}