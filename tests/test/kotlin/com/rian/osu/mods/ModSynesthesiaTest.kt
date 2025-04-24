package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModSynesthesiaTest {
    @Test
    fun `Test beat divisor combo color`() {
        fun test(beatDivisor: Int, red: Int, green: Int, blue: Int) {
            ModSynesthesia.getColorFor(beatDivisor).apply {
                Assert.assertEquals(red / 255f, r())
                Assert.assertEquals(green / 255f, g())
                Assert.assertEquals(blue / 255f, b())
            }
        }

        // Common beat divisors
        test(1, 255, 255, 255)
        test(2, 237, 17, 33)
        test(3, 136, 102, 238)
        test(4, 102, 204, 255)
        test(6, 238, 170, 0)
        test(8, 255, 204, 34)
        test(12, 204, 102, 0)
        test(16, 68, 17, 136)

        // Uncommon beat divisors
        for (divisor in intArrayOf(5, 7, 9, 10, 11, 13, 14, 15)) {
            test(divisor, 255, 0, 0)
        }
    }
}