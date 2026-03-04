package com.rian.osu.beatmap

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class StandardHitWindowTest(
    od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double
) : HitWindowTest(od, greatWindow, okWindow, mehWindow) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "OD={0}, Great={1}ms, Ok={2}ms, Meh={3}ms")
        fun data() = listOf(
            arrayOf(10.0, 19.5, 59.5, 99.5),
            arrayOf(8.2, 29.5, 73.5, 117.5),
            arrayOf(6.5, 40.5, 87.5, 134.5),
            arrayOf(3.7, 56.5, 109.5, 162.5),
            arrayOf(-1.6, 88.5, 151.5, 215.5)
        )
    }

    @Test
    fun `Test hit window`() = testHitWindow()

    override fun createHitWindow(od: Double) = StandardHitWindow(od)
}