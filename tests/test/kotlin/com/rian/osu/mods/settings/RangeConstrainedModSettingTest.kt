package com.rian.osu.mods.settings

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import org.junit.Assert
import org.junit.Test

class RangeConstrainedModSettingTest {
    @Test
    fun `Test initialization require() boundaries`() {
        fun test(action: () -> Unit) {
            Assert.assertThrows(IllegalArgumentException::class.java, action)
        }

        // Test min > max
        test { TestRangeConstrainedModSetting(defaultValue = 1, minValue = 2, maxValue = 1) }

        // Test default > max
        test { TestRangeConstrainedModSetting(defaultValue = 3, minValue = 1, maxValue = 2) }

        // Test default < min
        test { TestRangeConstrainedModSetting(defaultValue = 0, minValue = 1, maxValue = 2) }
    }

    @Test
    fun `Test assign boundaries`() {
        val setting = TestRangeConstrainedModSetting(defaultValue = 5, minValue = 1, maxValue = 10)

        setting.value = 0
        Assert.assertEquals(1, setting.value)

        setting.value = 11
        Assert.assertEquals(10, setting.value)
    }

    @Test
    fun `Test min assign boundaries`() {
        val setting = TestRangeConstrainedModSetting(defaultValue = 5, minValue = 1, maxValue = 10)

        setting.minValue = 6
        Assert.assertEquals(6, setting.minValue)
        Assert.assertEquals(6, setting.value)

        setting.minValue = 9
        Assert.assertEquals(9, setting.minValue)
        Assert.assertEquals(9, setting.value)
    }

    @Test
    fun `Test max assign boundaries`() {
        val setting = TestRangeConstrainedModSetting(defaultValue = 5, minValue = 1, maxValue = 10)

        setting.maxValue = 4
        Assert.assertEquals(4, setting.maxValue)
        Assert.assertEquals(4, setting.value)

        setting.maxValue = 2
        Assert.assertEquals(2, setting.maxValue)
        Assert.assertEquals(2, setting.value)
    }

    @Test
    fun `Test out of boundary assignments`() {
        val setting = TestRangeConstrainedModSetting(defaultValue = 5, minValue = 1, maxValue = 10)

        Assert.assertThrows(IllegalArgumentException::class.java) { setting.minValue = 11 }
        Assert.assertThrows(IllegalArgumentException::class.java) { setting.maxValue = 0 }
    }

    @Test
    fun `Test copying`() {
        val setting = TestRangeConstrainedModSetting(defaultValue = 5, minValue = 1, maxValue = 10)
        val otherSetting = TestRangeConstrainedModSetting(defaultValue = 6, minValue = 2, maxValue = 9)

        otherSetting.copyFrom(setting)

        Assert.assertEquals(setting.minValue, otherSetting.minValue)
        Assert.assertEquals(setting.maxValue, otherSetting.maxValue)
        Assert.assertEquals(setting.value, otherSetting.value)
    }
}

private class TestRangeConstrainedModSetting(defaultValue: Int, minValue: Int, maxValue: Int) :
    RangeConstrainedModSetting<Int>(name = "Test", defaultValue = defaultValue, minValue = minValue, maxValue = maxValue) {
    override fun load(json: JsonObject) = Unit
    override fun save(builder: JsonObjectBuilder) = Unit
}