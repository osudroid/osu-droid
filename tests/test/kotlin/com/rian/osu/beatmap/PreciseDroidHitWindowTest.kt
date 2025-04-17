package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test

class PreciseDroidHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10f, 25f, 80f, 130f)
        testHitWindowValues(8.2f, 35.8f, 94.4f, 148f)
        testHitWindowValues(6.5f, 46f, 108f, 165f)
        testHitWindowValues(3.7f, 62.8f, 130.4f, 193f)
        testHitWindowValues(-1.6f, 94.6f, 172.8f, 246f)
    }

    @Test
    fun `Test hit window to OD conversion`() {
        fun testConversion(od: Float, greatWindow: Float, okWindow: Float, mehWindow: Float) {
            Assert.assertEquals(PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow), od, 1e-2f)
            Assert.assertEquals(PreciseDroidHitWindow.hitWindow100ToOverallDifficulty(okWindow), od, 1e-2f)
            Assert.assertEquals(PreciseDroidHitWindow.hitWindow50ToOverallDifficulty(mehWindow), od, 1e-2f)
        }

        testConversion(10f, 25f, 80f, 130f)
        testConversion(8.2f, 35.8f, 94.4f, 148f)
        testConversion(6.5f, 46f, 108f, 165f)
        testConversion(3.7f, 62.8f, 130.4f, 193f)
        testConversion(-1.6f, 94.6f, 172.8f, 246f)
    }

    override fun createHitWindow(od: Float) = PreciseDroidHitWindow(od)
}