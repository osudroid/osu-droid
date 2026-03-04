package com.rian.osu.beatmap

import org.junit.Test

class StandardHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(10f, 19.5f, 59.5f, 99.5f)
        testHitWindowValues(8.2f, 29.5f, 73.5f, 117.5f)
        testHitWindowValues(6.5f, 40.5f, 87.5f, 134.5f)
        testHitWindowValues(3.7f, 56.5f, 109.5f, 162.5f)
        testHitWindowValues(-1.6f, 88.5f, 151.5f, 215.5f)
    }

    override fun createHitWindow(od: Float) = StandardHitWindow(od)
}