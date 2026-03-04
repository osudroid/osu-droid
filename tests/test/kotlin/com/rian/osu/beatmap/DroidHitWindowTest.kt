package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test

class DroidHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10.0, 50.0, 100.0, 200.0)
        testHitWindowValues(8.2, 59.0, 118.0, 218.0)
        testHitWindowValues(6.5, 67.5, 135.0, 235.0)
        testHitWindowValues(3.7, 81.5, 163.0, 263.0)
        testHitWindowValues(-1.6, 108.0, 216.0, 316.0)
    }

    @Test
    fun `Test hit window to OD conversion`() {
        fun testConversion(od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double) {
            Assert.assertEquals(od, DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2)
            Assert.assertEquals(od, DroidHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2)
            Assert.assertEquals(od, DroidHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2)
        }

        testConversion(10.0, 50.0, 100.0, 200.0)
        testConversion(8.2, 59.0, 118.0, 218.0)
        testConversion(6.5, 67.5, 135.0, 235.0)
        testConversion(3.7, 81.5, 163.0, 263.0)
        testConversion(-1.6, 108.0, 216.0, 316.0)
    }

    override fun createHitWindow(od: Double) = DroidHitWindow(od)
}