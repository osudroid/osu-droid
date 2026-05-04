package com.osudroid.mods.settings

import kotlinx.serialization.json.*
import org.junit.Assert
import org.junit.Test

class EnumModSettingTest {
    @Test
    fun `Test loading value from JSON`() {
        val json = buildJsonObject {
            put("test", 1)
        }

        val setting = createSetting()
        setting.load(json)

        Assert.assertEquals(_root_ide_package_.com.osudroid.mods.settings.TestEnum.Value2, setting.value)
    }

    @Test
    fun `Test saving value to JSON`() {
        val setting = createSetting()
        setting.value = _root_ide_package_.com.osudroid.mods.settings.TestEnum.Value2

        val json = buildJsonObject {
            setting.save(this)
        }

        Assert.assertEquals(1, json["test"]!!.jsonPrimitive.int)
    }

    private fun createSetting() = EnumModSetting(name = "Test", key = "test", defaultValue = _root_ide_package_.com.osudroid.mods.settings.TestEnum.Value1)
}

private enum class TestEnum {
    Value1,
    Value2
}