package com.rian.osu.beatmap

import org.junit.Assert

sealed class HitWindowTest {
    protected fun testHitWindowValues(od: Float, greatWindow: Float, okWindow: Float, mehWindow: Float) {
        val hitWindow = createHitWindow(od)

        Assert.assertEquals(hitWindow.greatWindow, greatWindow, 1e-2f)
        Assert.assertEquals(hitWindow.okWindow, okWindow, 1e-2f)
        Assert.assertEquals(hitWindow.mehWindow, mehWindow, 1e-2f)
    }

    protected abstract fun createHitWindow(od: Float): HitWindow
}