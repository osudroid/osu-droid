package com.osudroid.beatmaps

import org.junit.Assert

sealed class HitWindowTest(
    protected val od: Double,
    protected val greatWindow: Double,
    protected val okWindow: Double,
    protected val mehWindow: Double
) {
    protected fun testHitWindow() {
        val hitWindow = createHitWindow(od)

        Assert.assertEquals(greatWindow, hitWindow.greatWindow, 1e-2)
        Assert.assertEquals(okWindow, hitWindow.okWindow, 1e-2)
        Assert.assertEquals(mehWindow, hitWindow.mehWindow, 1e-2)
    }

    protected abstract fun createHitWindow(od: Double): HitWindow
}