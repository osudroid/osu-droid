package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UnadjustedStandardHitWindowTest(
    od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double
) : HitWindowTest(od, greatWindow, okWindow, mehWindow) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "OD={0}, Great={1}ms, OK={2}ms, Meh={3}ms")
        fun data() = arrayOf(
            arrayOf(10.0, 20.0, 60.0, 100.0),
            arrayOf(8.2, 30.8, 74.4, 118.0),
            arrayOf(6.5, 41.0, 88.0, 135.0),
            arrayOf(3.7, 57.8, 110.4, 163.0),
            arrayOf(-1.6, 89.6, 152.8, 216.0)
        )
    }

    @Test
    fun `Test hit window`() = testHitWindow()

    @Test
    fun `Test hit window to OD conversion`() {
        Assert.assertEquals(od, UnadjustedStandardHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2)
        Assert.assertEquals(od, UnadjustedStandardHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2)
        Assert.assertEquals(od, UnadjustedStandardHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2)
    }

    override fun createHitWindow(od: Double) = UnadjustedStandardHitWindow(od)
}