package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test

class UnadjustedStandardHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10.0, 20.0, 60.0, 100.0)
        testHitWindowValues(8.2, 30.8, 74.4, 118.0)
        testHitWindowValues(6.5, 41.0, 88.0, 135.0)
        testHitWindowValues(3.7, 57.8, 110.4, 163.0)
        testHitWindowValues(-1.6, 89.6, 152.8, 216.0)
    }

    @Test
    fun `Test hit window to OD conversion`() {
        fun testConversion(od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double) {
            Assert.assertEquals(od, UnadjustedStandardHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2)
            Assert.assertEquals(od, UnadjustedStandardHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2)
            Assert.assertEquals(od, UnadjustedStandardHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2)
        }

        testConversion(10.0, 20.0, 60.0, 100.0)
        testConversion(8.2, 30.8, 74.4, 118.0)
        testConversion(6.5, 41.0, 88.0, 135.0)
        testConversion(3.7, 57.8, 110.4, 163.0)
        testConversion(-1.6, 89.6, 152.8, 216.0)
    }

    override fun createHitWindow(od: Double) = UnadjustedStandardHitWindow(od)
}