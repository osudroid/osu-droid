package ru.nsu.ccfit.zuev.osu

import org.junit.Assert
import org.junit.Test

class ConfigTest {
    @Test
    fun `Shake intensity has percentage bounds`() {
        Assert.assertArrayEquals(intArrayOf(0, 100), Config.PREFERENCE_BOUNDS["shakeIntensity"])
    }
}
