package com.rian.osu.mods.settings

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import org.junit.Assert
import org.junit.Test

class ModSettingTest {
    @Test
    fun `Test setting reset`() {
        val setting = TestModSetting(1)

        setting.defaultValue = 2
        setting.value = 3

        setting.reset()

        Assert.assertEquals(1, setting.defaultValue)
        Assert.assertEquals(1, setting.value)
    }
}

private class TestModSetting(defaultValue: Int) :
    ModSetting<Int>(name = "Test", valueFormatter = null, defaultValue = defaultValue) {
    override fun load(json: JsonObject) = Unit
    override fun save(builder: JsonObjectBuilder) = Unit
}