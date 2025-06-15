package com.rian.osu.beatmap

import org.junit.Test

class EmptyHitWindowTest : HitWindowTest() {
    @Test
    fun `Test hit window`() {
        testHitWindowValues(5f, 0f, 0f, 0f)
        testHitWindowValues(8f, 0f, 0f, 0f)
    }

    override fun createHitWindow(od: Float) = EmptyHitWindow()
}