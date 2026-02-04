package com.rian.osu.mods.settings

import kotlinx.serialization.json.*
import org.junit.Assert
import org.junit.Test

class FloatModSettingTest {
    @Test
    fun `Test boundaries`() {
        fun test(action: () -> Unit) {
            Assert.assertThrows(IllegalArgumentException::class.java, action)
        }

        // Test precision < 0
        test { FloatModSetting(name = "Test", defaultValue = 0.12f, precision = -1) }
    }

    @Test
    fun `Test step without precision`() {
        var setting by FloatModSetting(name = "Test", defaultValue = 0.12f, step = 0.12f)
        Assert.assertEquals(0.12f, setting, 1e-5f)

        setting = 0.24f
        Assert.assertEquals(0.24f, setting, 1e-5f)

        setting = 0.36f
        Assert.assertEquals(0.36f, setting, 1e-5f)

        setting = 0.48f
        Assert.assertEquals(0.48f, setting, 1e-5f)
    }

    @Test
    fun `Test step with precision`() {
        var setting by FloatModSetting(
            name = "Test",
            defaultValue = 0.12f,
            step = 0.12f,
            precision = 2,
        )

        // These need to be exactly equal to prevent serialization differences.
        Assert.assertEquals(0.12f, setting)

        setting = 0.24f
        Assert.assertEquals(0.24f, setting)

        setting = 0.36f
        Assert.assertEquals(0.36f, setting)

        setting = 0.48f
        Assert.assertEquals(0.48f, setting)
    }

    @Test
    fun `Test value cap without precision`() {
        var setting by FloatModSetting(
            name = "Test",
            defaultValue = 0.12f,
            minValue = 0.12f,
            maxValue = 1.2f,
            step = 0.12f,
        )

        Assert.assertEquals(0.12f, setting, 1e-5f)

        setting = 0f
        Assert.assertEquals(0.12f, setting, 1e-5f)

        setting = 1.2f
        Assert.assertEquals(1.2f, setting, 1e-5f)

        setting = 1.5f
        Assert.assertEquals(1.2f, setting, 1e-5f)
    }

    @Test
    fun `Test value cap with precision`() {
        var setting by FloatModSetting(
            name = "Test",
            defaultValue = 0.12f,
            minValue = 0.12f,
            // Intentional multiplication to introduce floating point errors.
            maxValue = 0.12f * 10,
            step = 0.12f,
            precision = 2,
        )

        // These need to be exactly equal to prevent serialization differences.
        Assert.assertEquals(0.12f, setting)

        setting = 0f
        Assert.assertEquals(0.12f, setting)

        setting = 1.2f
        Assert.assertEquals(1.2f, setting)

        setting = 1.5f
        Assert.assertEquals(1.2f, setting)
    }

    @Test
    fun `Test copying`() {
        val setting = FloatModSetting(
            name = "Test",
            defaultValue = 1.5f,
            minValue = 0.5f,
            maxValue = 2.5f,
            step = 0.1f,
            precision = 2
        )

        val otherSetting = FloatModSetting(
            name = "OtherTest",
            defaultValue = 0.0f
        )

        otherSetting.copyFrom(setting)

        Assert.assertEquals(setting.defaultValue, otherSetting.defaultValue, 0f)
        Assert.assertEquals(setting.value, otherSetting.value, 0f)
        Assert.assertEquals(setting.minValue, otherSetting.minValue, 0f)
        Assert.assertEquals(setting.maxValue, otherSetting.maxValue, 0f)
        Assert.assertEquals(setting.step, otherSetting.step, 0f)
        Assert.assertEquals(setting.precision, otherSetting.precision)
    }

    @Test
    fun `Test loading value from JSON`() {
        val setting = FloatModSetting(name = "Test", key = "test", defaultValue = 0f)
        val json = buildJsonObject {
            put("test", 1.25)
        }

        setting.load(json)

        Assert.assertEquals(1.25f, setting.value, 0f)
    }

    @Test
    fun `Test saving value to JSON`() {
        val setting = FloatModSetting(name = "Test", key = "test", defaultValue = 0f)
        setting.value = 2.5f

        val json = buildJsonObject {
            setting.save(this)
        }

        Assert.assertEquals(2.5f, json["test"]!!.jsonPrimitive.float, 0f)
    }
}