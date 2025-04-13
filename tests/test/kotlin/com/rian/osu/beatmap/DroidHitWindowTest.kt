package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test

class DroidHitWindowTest : HitWindowTest() {
    @Test
    fun testHitWindow() {
        testHitWindowValues(10f, 50f, 100f, 200f)
        testHitWindowValues(8.2f, 59f, 118f, 218f)
        testHitWindowValues(6.5f, 67.5f, 135f, 235f)
        testHitWindowValues(3.7f, 81.5f, 163f, 263f)
        testHitWindowValues(-1.6f, 108f, 216f, 316f)
    }

    @Test
    fun testHitWindowToODConversion() {
        fun testConversion(od: Float, greatWindow: Float, okWindow: Float, mehWindow: Float) {
            Assert.assertEquals(DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow), od, 1e-2f)
            Assert.assertEquals(DroidHitWindow.hitWindow100ToOverallDifficulty(okWindow), od, 1e-2f)
            Assert.assertEquals(DroidHitWindow.hitWindow50ToOverallDifficulty(mehWindow), od, 1e-2f)
        }

        testConversion(10f, 50f, 100f, 200f)
        testConversion(8.2f, 59f, 118f, 218f)
        testConversion(6.5f, 67.5f, 135f, 235f)
        testConversion(3.7f, 81.5f, 163f, 263f)
        testConversion(-1.6f, 108f, 216f, 316f)
    }

    override fun createHitWindow(od: Float) = DroidHitWindow(od)
}