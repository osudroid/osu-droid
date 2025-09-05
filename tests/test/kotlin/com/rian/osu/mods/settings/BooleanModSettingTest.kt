package com.rian.osu.mods.settings

import org.junit.Assert
import org.junit.Test

class BooleanModSettingTest {
    @Test
    fun `Test delegate`() {
        var setting by BooleanModSetting("Test", true)
        Assert.assertTrue(setting)

        setting = false
        Assert.assertFalse(setting)
    }
}