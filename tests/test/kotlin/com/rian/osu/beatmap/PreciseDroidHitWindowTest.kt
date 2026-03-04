package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test

class PreciseDroidHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10.0, 25.0, 80.0, 130.0)
        testHitWindowValues(8.2, 35.8, 94.4, 148.0)
        testHitWindowValues(6.5, 46.0, 108.0, 165.0)
        testHitWindowValues(3.7, 62.8, 130.4, 193.0)
        testHitWindowValues(-1.6, 94.6, 172.8, 246.0)
    }

    @Test
    fun `Test hit window to OD conversion`() {
        fun testConversion(od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double) {
            Assert.assertEquals(od, PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2)
            Assert.assertEquals(od, PreciseDroidHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2)
            Assert.assertEquals(od, PreciseDroidHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2)
        }

        testConversion(10.0, 25.0, 80.0, 130.0)
        testConversion(8.2, 35.8, 94.4, 148.0)
        testConversion(6.5, 46.0, 108.0, 165.0)
        testConversion(3.7, 62.8, 130.4, 193.0)
        testConversion(-1.6, 94.6, 172.8, 246.0)
    }

    override fun createHitWindow(od: Double) = PreciseDroidHitWindow(od)
}