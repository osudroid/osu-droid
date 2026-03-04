package com.rian.osu.beatmap

import org.junit.Test

class EmptyHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(5.0, 0.0, 0.0, 0.0)
        testHitWindowValues(8.0, 0.0, 0.0, 0.0)
    }

    override fun createHitWindow(od: Double) = EmptyHitWindow()
}