package com.rian.osu.beatmap

import org.junit.Assert

sealed class HitWindowTest {
    protected fun testHitWindowValues(od: Double, greatWindow: Double, okWindow: Double, mehWindow: Double) {
        val hitWindow = createHitWindow(od)

        Assert.assertEquals(greatWindow, hitWindow.greatWindow, 1e-2)
        Assert.assertEquals(okWindow, hitWindow.okWindow, 1e-2)
        Assert.assertEquals(mehWindow, hitWindow.mehWindow, 1e-2)
    }

    protected abstract fun createHitWindow(od: Double): HitWindow
}