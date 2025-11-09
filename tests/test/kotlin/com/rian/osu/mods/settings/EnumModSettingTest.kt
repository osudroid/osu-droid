package com.rian.osu.mods.settings

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnumModSettingTest {
    @Test
    fun `Test loading value from enum ordinal`() {
        val setting = createSetting()

        val json = JSONObject().apply {
            put("test", 1)
        }

        setting.load(json)

        Assert.assertEquals(TestEnum.Value2, setting.value)
    }

    @Test
    fun `Test saving value saving enum ordinal instead of value`() {
        val setting = createSetting()
        setting.value = TestEnum.Value2

        val json = JSONObject()
        setting.save(json)

        Assert.assertEquals(1, json.getInt("test"))
    }

    private fun createSetting() = EnumModSetting(name = "Test", key = "test", defaultValue = TestEnum.Value1)
}

private enum class TestEnum {
    Value1,
    Value2
}