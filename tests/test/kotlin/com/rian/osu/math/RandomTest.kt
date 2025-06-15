package com.rian.osu.math

import org.junit.Assert
import org.junit.Test

class RandomTest {
    @Test
    fun `Test 10 repeated nextDouble`() {
        val random = Random(100)

        fun test(expected: Double) = Assert.assertEquals(expected, random.nextDouble(), 1e-10)

        test(0.9687746888812514)
        test(0.15918711859695014)
        test(0.6668217371529069)
        test(0.9024542499810709)
        test(0.35460713056596327)
        test(0.9486654628760486)
        test(0.7116968248559613)
        test(0.6106181548026475)
        test(0.3492197945477533)
        test(0.14881422191337412)
    }
}