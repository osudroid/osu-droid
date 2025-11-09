package com.rian.osu.mods.settings

import org.junit.Assert
import org.junit.Test

class ModSettingTest {
    @Test
    fun `Test setting reset`() {
        val setting = ModSetting(name = "Test", valueFormatter = null, defaultValue = 1)

        setting.defaultValue = 2
        setting.value = 3

        setting.reset()

        Assert.assertEquals(1, setting.defaultValue)
        Assert.assertEquals(1, setting.value)
    }
}