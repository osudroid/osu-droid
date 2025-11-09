package com.rian.osu.mods.settings

import kotlinx.serialization.json.*
import org.junit.Assert
import org.junit.Test

class EnumModSettingTest {
    @Test
    fun `Test loading value from enum ordinal`() {
        val json = buildJsonObject {
            put("test", 1)
        }

        val setting = createSetting()
        setting.load(json)

        Assert.assertEquals(TestEnum.Value2, setting.value)
    }

    @Test
    fun `Test saving value saving enum ordinal instead of value`() {
        val setting = createSetting()
        setting.value = TestEnum.Value2

        val json = buildJsonObject {
            setting.save(this)
        }

        Assert.assertEquals(1, json["test"]!!.jsonPrimitive.int)
    }

    private fun createSetting() = EnumModSetting(name = "Test", key = "test", defaultValue = TestEnum.Value1)
}

private enum class TestEnum {
    Value1,
    Value2
}