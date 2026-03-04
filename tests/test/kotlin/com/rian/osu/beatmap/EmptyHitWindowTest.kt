package com.rian.osu.beatmap

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class EmptyHitWindowTest(od: Double) : HitWindowTest(od, 0.0, 0.0, 0.0) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "OD={0}")
        fun data() = listOf(
            arrayOf(5.0),
            arrayOf(8.0)
        )
    }

    @Test
    fun `Test hit window`() = testHitWindow()

    override fun createHitWindow(od: Double) = EmptyHitWindow()
}