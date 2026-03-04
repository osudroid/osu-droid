package com.rian.osu.beatmap

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PreciseDroidHitWindowTest(
    od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double
) : HitWindowTest(od, greatWindow, okWindow, mehWindow) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "OD={0}, Great={1}ms, Ok={2}ms, Meh={3}ms")
        fun data() = listOf(
            arrayOf(10.0, 25.0, 80.0, 130.0),
            arrayOf(8.2, 35.8, 94.4, 148.0),
            arrayOf(6.5, 46.0, 108.0, 165.0),
            arrayOf(3.7, 62.8, 130.4, 193.0),
            arrayOf(-1.6, 94.6, 172.8, 246.0)
        )
    }

    @Test
    fun `Test hit window`() = testHitWindow()

    @Test
    fun `Test hit window to OD conversion`() {
        Assert.assertEquals(od, PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow), 1e-2)
        Assert.assertEquals(od, PreciseDroidHitWindow.hitWindow100ToOverallDifficulty(okWindow), 1e-2)
        Assert.assertEquals(od, PreciseDroidHitWindow.hitWindow50ToOverallDifficulty(mehWindow), 1e-2)
    }

    override fun createHitWindow(od: Double) = PreciseDroidHitWindow(od)
}