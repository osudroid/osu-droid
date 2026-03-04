package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DroidHitWindowTest(
    od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double
) : HitWindowTest(od, greatWindow, okWindow, mehWindow) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "OD={0}, Great={1}ms, Ok={2}ms, Meh={3}ms")
        fun data() = listOf(
            arrayOf(10.0, 50.0, 100.0, 200.0),
            arrayOf(8.2, 59.0, 118.0, 218.0),
            arrayOf(6.5, 67.5, 135.0, 235.0),
            arrayOf(3.7, 81.5, 163.0, 263.0),
            arrayOf(-1.6, 108.0, 216.0, 316.0)
        )
    }

    @Test
    fun `Test hit window`() = testHitWindow()

    @Test
    fun `Test hit window to OD conversion`() {
        Assert.assertEquals(od, DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2)
        Assert.assertEquals(od, DroidHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2)
        Assert.assertEquals(od, DroidHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2)
    }

    override fun createHitWindow(od: Double) = DroidHitWindow(od)
}