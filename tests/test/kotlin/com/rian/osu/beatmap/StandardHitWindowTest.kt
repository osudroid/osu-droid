package com.rian.osu.beatmap

import org.junit.Test

class StandardHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10.0, 19.5, 59.5, 99.5)
        testHitWindowValues(8.2, 29.5, 73.5, 117.5)
        testHitWindowValues(6.5, 40.5, 87.5, 134.5)
        testHitWindowValues(3.7, 56.5, 109.5, 162.5)
        testHitWindowValues(-1.6, 88.5, 151.5, 215.5)
    }

    override fun createHitWindow(od: Double) = StandardHitWindow(od)
}