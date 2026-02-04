package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test

class StandardHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10f, 20f, 60f, 100f)
        testHitWindowValues(8.2f, 30.8f, 74.4f, 118f)
        testHitWindowValues(6.5f, 41f, 88f, 135f)
        testHitWindowValues(3.7f, 57.8f, 110.4f, 163f)
        testHitWindowValues(-1.6f, 89.6f, 152.8f, 216f)
    }

    @Test
    fun `Test hit window to OD conversion`() {
        fun testConversion(od: Float, greatWindow: Float, okWindow: Float, mehWindow: Float) {
            Assert.assertEquals(od, StandardHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2f)
            Assert.assertEquals(od, StandardHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2f)
            Assert.assertEquals(od, StandardHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2f)
        }

        testConversion(10f, 20f, 60f, 100f)
        testConversion(8.2f, 30.8f, 74.4f, 118f)
        testConversion(6.5f, 41f, 88f, 135f)
        testConversion(3.7f, 57.8f, 110.4f, 163f)
        testConversion(-1.6f, 89.6f, 152.8f, 216f)
    }

    override fun createHitWindow(od: Float) = StandardHitWindow(od)
}