package com.rian.osu.mods.settings

import org.junit.Assert
import org.junit.Test

class NullableNumberModSettingTest {
    @Test
    fun `Test step boundary`() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            createSetting(defaultValue = 5, minValue = 1, maxValue = 10, step = -1)
        }
    }

    @Test
    fun `Test step`() {
        val setting = createSetting(defaultValue = 5, minValue = 0, maxValue = 10, step = 0)

        setting.step = 1
        Assert.assertEquals(1, setting.step)
        Assert.assertEquals(5, setting.value)

        setting.step = 2
        Assert.assertEquals(2, setting.step)
        Assert.assertEquals(4, setting.value)
    }

    @Test
    fun `Test copying`() {
        val setting = createSetting(defaultValue = 5, minValue = 0, maxValue = 10, step = 1)
        val otherSetting = createSetting(defaultValue = 0, minValue = 0, maxValue = 0, step = 0)

        otherSetting.copyFrom(setting)

        Assert.assertEquals(5, otherSetting.defaultValue)
        Assert.assertEquals(5, otherSetting.value)
        Assert.assertEquals(0, otherSetting.minValue)
        Assert.assertEquals(10, otherSetting.maxValue)
        Assert.assertEquals(1, otherSetting.step)
    }

    private fun createSetting(defaultValue: Int?, minValue: Int, maxValue: Int, step: Int) =
        NullableNumberModSetting(
            name = "Test",
            defaultValue = defaultValue,
            minValue = minValue,
            maxValue = maxValue,
            step = step
        )
}